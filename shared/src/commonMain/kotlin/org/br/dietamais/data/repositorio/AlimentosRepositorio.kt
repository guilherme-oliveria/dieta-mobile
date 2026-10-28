package org.br.dietamais.data.repositorio

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import org.br.dietamais.data.sync.EscritaOffline
import org.br.dietamais.data.sync.novoIdLocal
import org.br.dietamais.data.sync.reprojetarAbertas
import org.br.dietamais.data.local.BancoDieta
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import kotlin.uuid.Uuid

data class CatalogoAlimentos(val grupos: List<GrupoAlimentoResposta> = emptyList(), val alimentos: List<AlimentoResposta> = emptyList())
interface PortaAlimentos {
    fun observar(): Flow<CatalogoAlimentos>
    suspend fun atualizar()
    suspend fun criar(requisicao: CriarAlimentoRequisicao, chave: Uuid): AlimentoResposta
    suspend fun alterar(id: Uuid, versao: Long, requisicao: AtualizarAlimentoRequisicao): AlimentoResposta
}

class AlimentosRepositorio(private val api: ApiDieta, private val banco: BancoDieta, private val escrita: EscritaOffline = EscritaOffline(banco),
    private val agora: () -> kotlin.time.Instant = { kotlin.time.Clock.System.now() }) : PortaAlimentos {
    private val mutex = Mutex()
    override fun observar(): Flow<CatalogoAlimentos> = combine(
        banco.alimentoCacheQueries.daSessao("grupos").asFlow().mapToList(Dispatchers.Default),
        banco.alimentoCacheQueries.daSessao("alimentos").asFlow().mapToList(Dispatchers.Default)) { grupos, alimentos ->
        try { CatalogoAlimentos(grupos.map { JsonContrato.decodeFromString<GrupoAlimentoResposta>(it) }.sortedBy { it.nome.lowercase() },
            alimentos.map { JsonContrato.decodeFromString<AlimentoResposta>(it) }.sortedBy { it.nome.lowercase() }) }
        catch (_: Exception) { throw RespostaInvalida() }
    }

    override suspend fun atualizar() = mutex.withLock {
        val contexto = contexto()
        val grupos = api.obter<GruposAlimentosResposta>("grupos-alimentos").itens
        conferir(contexto)
        if (grupos.map { it.id }.distinct().size != grupos.size) throw RespostaInvalida()
        val alimentos = carregar(contexto, true) + carregar(contexto, false)
        if (alimentos.map { it.id }.distinct().size != alimentos.size || alimentos.any { item -> grupos.none { it.id == item.grupoId } }) throw RespostaInvalida()
        banco.transaction {
            conferir(contexto)
            banco.alimentoCacheQueries.limparDaConta(contexto.contexto.usuarioId.toString(), contexto.contexto.perfil.name)
            grupos.forEach { salvar(contexto, "grupos", it.id, JsonContrato.encodeToString(it)) }
            alimentos.forEach { salvar(contexto, "alimentos", it.id, JsonContrato.encodeToString(it)) }
            banco.reprojetarAbertas(contexto.contexto.usuarioId.toString(), contexto.contexto.perfil.name, "alimentos")
        }
    }

    private suspend fun carregar(contexto: OperacaoSessao, ativo: Boolean): List<AlimentoResposta> {
        val itens = mutableListOf<AlimentoResposta>()
        var pagina = 0
        var total: Long? = null
        do {
            val resposta = api.obter<PaginaAlimentosResposta>("alimentos", mapOf("pagina" to pagina.toString(), "tamanho" to "20", "ativo" to ativo.toString()))
            conferir(contexto)
            if (resposta.pagina != pagina || resposta.tamanho != 20 || resposta.total < 0 || resposta.itens.size > 20 ||
                (total != null && total != resposta.total) || (resposta.itens.isEmpty() && itens.size.toLong() < resposta.total) ||
                resposta.itens.any { it.ativo != ativo || it.versao < 0 || !it.gramasPorPorcao.isFinite() || it.gramasPorPorcao <= 0 }) throw RespostaInvalida()
            total = resposta.total
            itens += resposta.itens
            if (itens.size.toLong() > resposta.total || itens.map { it.id }.distinct().size != itens.size) throw RespostaInvalida()
            pagina++
        } while (itens.size.toLong() < total)
        return itens
    }

    /** Offline-first (MOB-29): grava o alimento otimista e a mutação na fila; o servidor decide no /sync. */
    override suspend fun criar(requisicao: CriarAlimentoRequisicao, chave: Uuid): AlimentoResposta = mutex.withLock {
        contexto()
        val id = requisicao.id ?: novoIdLocal()
        val otimista = AlimentoResposta(id, null, requisicao.grupoId, requisicao.nome, requisicao.medidaCaseira, requisicao.gramasPorPorcao, true, 0, agora())
        escrita.criar("alimentos", id, JsonContrato.encodeToJsonElement(requisicao.copy(id = id)).jsonObject, JsonContrato.encodeToString(otimista))
        otimista
    }
    override suspend fun alterar(id: Uuid, versao: Long, requisicao: AtualizarAlimentoRequisicao): AlimentoResposta = mutex.withLock {
        contexto()
        val sessao = banco.sessaoQueries.atual().executeAsOneOrNull() ?: throw SessaoAlterada()
        val guardado = banco.registroSyncQueries.dadosDe(sessao.usuario_id, sessao.perfil, "alimentos", id.toString()).executeAsOneOrNull()?.dados_json
            // o catálogo pode ter vindo só pela API (cache de leitura): o registro local nasce dele para o instantâneo anterior existir
            ?: banco.alimentoCacheQueries.buscar(sessao.usuario_id, sessao.perfil, "alimentos", id.toString()).executeAsOneOrNull()
                ?.also { banco.registroSyncQueries.salvar(sessao.usuario_id, sessao.perfil, "alimentos", id.toString(), 0, 0, it) }
        val atual = guardado?.let { JsonContrato.decodeFromString<AlimentoResposta>(it) } ?: throw ErroApi("RECURSO_NAO_ENCONTRADO", null, emptyList(), 404, null, null)
        val nova = atual.copy(grupoId = requisicao.grupoId, nome = requisicao.nome, medidaCaseira = requisicao.medidaCaseira, gramasPorPorcao = requisicao.gramasPorPorcao,
            ativo = requisicao.ativo, versao = versao + 1, atualizadoEm = agora())
        escrita.atualizar("alimentos", id, versao, JsonContrato.encodeToJsonElement(requisicao).jsonObject, JsonContrato.encodeToString(nova))
        nova
    }
    private fun salvar(contexto: OperacaoSessao, colecao: String, id: Uuid, json: String) =
        banco.alimentoCacheQueries.salvar(contexto.contexto.usuarioId.toString(), contexto.contexto.perfil.name, colecao, id.toString(), json)
    private fun contexto() = api.operacaoSessao.value?.takeIf { it.contexto.perfil == Perfil.NUTRICIONISTA } ?: throw SessaoAlterada()
    private fun conferir(esperado: OperacaoSessao) {
        val sessao = banco.sessaoQueries.atual().executeAsOneOrNull()
        if (api.operacaoSessao.value != esperado || sessao?.usuario_id != esperado.contexto.usuarioId.toString() || sessao.perfil != esperado.contexto.perfil.name) throw SessaoAlterada()
    }
}
