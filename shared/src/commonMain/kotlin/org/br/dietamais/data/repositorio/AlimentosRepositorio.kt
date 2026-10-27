package org.br.dietamais.data.repositorio

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.encodeToJsonElement
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

class AlimentosRepositorio(private val api: ApiDieta, private val banco: BancoDieta) : PortaAlimentos {
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

    override suspend fun criar(requisicao: CriarAlimentoRequisicao, chave: Uuid): AlimentoResposta = mutex.withLock {
        val contexto = contexto()
        val resposta = api.enviar<AlimentoResposta>("alimentos", JsonContrato.encodeToJsonElement(requisicao), chave)
        if (requisicao.id != null && resposta.id != requisicao.id) throw RespostaInvalida()
        gravar(contexto, resposta)
    }
    override suspend fun alterar(id: Uuid, versao: Long, requisicao: AtualizarAlimentoRequisicao): AlimentoResposta = mutex.withLock {
        val contexto = contexto()
        val resposta = api.alterar<AlimentoResposta>("alimentos/$id", JsonContrato.encodeToJsonElement(requisicao), versao)
        if (resposta.id != id) throw RespostaInvalida()
        gravar(contexto, resposta)
    }
    private fun gravar(contexto: OperacaoSessao, resposta: AlimentoResposta): AlimentoResposta {
        if (resposta.versao < 0 || !resposta.gramasPorPorcao.isFinite() || resposta.gramasPorPorcao <= 0) throw RespostaInvalida()
        banco.transaction { conferir(contexto); salvar(contexto, "alimentos", resposta.id, JsonContrato.encodeToString(resposta)) }
        return resposta
    }
    private fun salvar(contexto: OperacaoSessao, colecao: String, id: Uuid, json: String) =
        banco.alimentoCacheQueries.salvar(contexto.contexto.usuarioId.toString(), contexto.contexto.perfil.name, colecao, id.toString(), json)
    private fun contexto() = api.operacaoSessao.value?.takeIf { it.contexto.perfil == Perfil.NUTRICIONISTA } ?: throw SessaoAlterada()
    private fun conferir(esperado: OperacaoSessao) {
        val sessao = banco.sessaoQueries.atual().executeAsOneOrNull()
        if (api.operacaoSessao.value != esperado || sessao?.usuario_id != esperado.contexto.usuarioId.toString() || sessao.perfil != esperado.contexto.perfil.name) throw SessaoAlterada()
    }
}
