package org.br.dietamais.data.repositorio

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import org.br.dietamais.data.sync.EscritaOffline
import org.br.dietamais.dominio.avaliacao.classificarImc
import org.br.dietamais.dominio.avaliacao.imcDe
import kotlin.time.Clock
import kotlin.time.Instant
import org.br.dietamais.data.local.BancoDieta
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import kotlin.uuid.Uuid

interface PortaAvaliacoes {
    /** Avaliações do paciente, da mais recente para a mais antiga. Sem `pacienteId`, todas as da sessão (perfil paciente). */
    fun observar(pacienteId: Uuid?): Flow<List<AvaliacaoResposta>>
    /** Busca no servidor e funde no banco local (a nutri abre a ficha antes do sync alcançar o histórico). */
    suspend fun atualizar(pacienteId: Uuid)
    suspend fun criar(pacienteId: Uuid, requisicao: SalvarAvaliacaoRequisicao): AvaliacaoResposta
    suspend fun salvar(id: Uuid, versao: Long, requisicao: SalvarAvaliacaoRequisicao): AvaliacaoResposta
}

/**
 * Criar e salvar são offline-first (MOB-29): gravam a avaliação otimista (IMC calculado pela mesma regra do servidor) e a mutação na fila.
 * O `vinculoId` otimista é provisório; o servidor traz o real no próximo pull.
 */
class AvaliacaoRepositorio(private val api: ApiDieta, private val banco: BancoDieta, private val escrita: EscritaOffline = EscritaOffline(banco),
    private val agora: () -> Instant = { Clock.System.now() }) : PortaAvaliacoes {
    override fun observar(pacienteId: Uuid?): Flow<List<AvaliacaoResposta>> =
        banco.registroSyncQueries.daSessao("avaliacoes").asFlow().mapToList(Dispatchers.Default).map { linhas ->
            try {
                linhas.map { JsonContrato.decodeFromString<AvaliacaoResposta>(it) }
                    .filter { pacienteId == null || it.pacienteId == pacienteId }
                    .sortedWith(compareByDescending<AvaliacaoResposta> { it.dataHora }.thenByDescending { it.criadoEm })
            } catch (_: Exception) { throw RespostaInvalida() }
        }

    override suspend fun atualizar(pacienteId: Uuid) {
        val contexto = contexto()
        val resposta = api.obter<AvaliacoesResposta>("pacientes/$pacienteId/avaliacoes")
        conferir(contexto)
        if (resposta.itens.any { it.pacienteId != pacienteId }) throw RespostaInvalida()
        resposta.itens.forEach { guardar(it) }
    }

    override suspend fun criar(pacienteId: Uuid, requisicao: SalvarAvaliacaoRequisicao): AvaliacaoResposta {
        contexto()
        val id = requisicao.id ?: org.br.dietamais.data.sync.novoIdLocal()
        val corpo = requisicao.copy(id = id)
        val agoraLocal = agora()
        val imc = imcDe(corpo.pesoKg, corpo.estaturaM)
        val otimista = AvaliacaoResposta(id, pacienteId, pacienteId, corpo.dataHora, corpo.pesoKg, corpo.estaturaM, imc, classificarImc(imc), corpo.perimetros, corpo.dobras,
            corpo.composicao, corpo.parecer, corpo.compartilhada ?: true, 1, agoraLocal, agoraLocal)
        // RN-SYN-04: no CRIAR a ficha vai dentro de `dados`
        val dados = JsonObject(JsonContrato.encodeToJsonElement(corpo).jsonObject + ("pacienteId" to kotlinx.serialization.json.JsonPrimitive(pacienteId.toString())))
        escrita.criar("avaliacoes", id, dados, JsonContrato.encodeToString(otimista))
        return otimista
    }

    override suspend fun salvar(id: Uuid, versao: Long, requisicao: SalvarAvaliacaoRequisicao): AvaliacaoResposta {
        contexto()
        val sessao = banco.sessaoQueries.atual().executeAsOneOrNull() ?: throw SessaoAlterada()
        val atual = banco.registroSyncQueries.dadosDe(sessao.usuario_id, sessao.perfil, "avaliacoes", id.toString()).executeAsOneOrNull()?.dados_json
            ?.let { JsonContrato.decodeFromString<AvaliacaoResposta>(it) } ?: throw ErroApi("RECURSO_NAO_ENCONTRADO", null, emptyList(), 404, null, null)
        val imc = imcDe(requisicao.pesoKg, requisicao.estaturaM)
        val nova = atual.copy(dataHora = requisicao.dataHora, pesoKg = requisicao.pesoKg, estaturaM = requisicao.estaturaM, imc = imc, classificacaoImc = classificarImc(imc),
            perimetros = requisicao.perimetros, dobras = requisicao.dobras, composicao = requisicao.composicao, parecer = requisicao.parecer,
            compartilhada = requisicao.compartilhada ?: atual.compartilhada, versao = versao + 1, atualizadoEm = agora())
        escrita.atualizar("avaliacoes", id, versao, JsonContrato.encodeToJsonElement(requisicao.copy(id = id)).jsonObject, JsonContrato.encodeToString(nova))
        return nova
    }

    /** Só substitui o que o banco já tem se a versão for igual ou mais nova (o sync pode ter chegado antes). */
    private fun guardar(avaliacao: AvaliacaoResposta) {
        val sessao = banco.sessaoQueries.atual().executeAsOneOrNull() ?: return
        val atual = banco.registroSyncQueries.revisao(sessao.usuario_id, sessao.perfil, "avaliacoes", avaliacao.id.toString()).executeAsOneOrNull()
        if (atual != null && atual > avaliacao.versao) return
        banco.registroSyncQueries.salvar(sessao.usuario_id, sessao.perfil, "avaliacoes", avaliacao.id.toString(), avaliacao.versao, 0, JsonContrato.encodeToString(avaliacao))
    }

    private fun contexto() = api.operacaoSessao.value?.takeIf { it.contexto.perfil == Perfil.NUTRICIONISTA } ?: throw SessaoAlterada()
    private fun conferir(esperado: OperacaoSessao) { if (api.operacaoSessao.value != esperado) throw SessaoAlterada() }
}
