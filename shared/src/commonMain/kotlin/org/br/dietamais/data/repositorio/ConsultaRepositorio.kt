package org.br.dietamais.data.repositorio

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import org.br.dietamais.data.local.BancoDieta
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.sync.EscritaOffline
import org.br.dietamais.data.sync.novoIdLocal
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

interface PortaConsultas {
    /** Todas as consultas da sessão (nutri: as dela; paciente: as suas), da mais cedo para a mais tarde. Vem do banco local, então funciona offline. */
    fun observar(): Flow<List<ConsultaResposta>>
    /** Nutri: busca a janela no servidor e funde no banco local (o sync traz o restante). */
    suspend fun atualizar(de: Instant, ate: Instant)
    suspend fun criar(requisicao: CriarConsultaRequisicao): ConsultaResposta
    suspend fun alterar(id: Uuid, versao: Long, requisicao: AtualizarConsultaRequisicao): ConsultaResposta
}

/**
 * Leitura do banco local; criar e alterar são offline-first (MOB-29): gravam a consulta otimista e a mutação na fila, e o backend decide no /sync
 * (RN-AGE-01). Uma recusa volta pelas pendências. O `vinculoId` otimista é provisório; o servidor traz o real no próximo pull.
 */
class ConsultaRepositorio(private val api: ApiDieta, private val banco: BancoDieta, private val escrita: EscritaOffline = EscritaOffline(banco),
    private val agora: () -> Instant = { Clock.System.now() }) : PortaConsultas {
    override fun observar(): Flow<List<ConsultaResposta>> =
        banco.registroSyncQueries.daSessao("consultas").asFlow().mapToList(Dispatchers.Default).map { linhas ->
            try { linhas.map { JsonContrato.decodeFromString<ConsultaResposta>(it) }.sortedWith(compareBy<ConsultaResposta> { it.inicio }.thenBy { it.id.toString() }) }
            catch (_: Exception) { throw RespostaInvalida() }
        }

    override suspend fun atualizar(de: Instant, ate: Instant) {
        val contexto = contexto()
        val resposta = api.obter<ConsultasResposta>("consultas", mapOf("de" to de.toString(), "ate" to ate.toString()))
        conferir(contexto)
        resposta.itens.forEach { guardar(it) }
    }

    override suspend fun criar(requisicao: CriarConsultaRequisicao): ConsultaResposta {
        contexto()
        val id = requisicao.id ?: novoIdLocal()
        val pedido = requisicao.copy(id = id)
        val agoraLocal = agora()
        val otimista = ConsultaResposta(id, pedido.pacienteId, pedido.pacienteId, pedido.inicio, pedido.duracaoMin, pedido.tipo, pedido.lugar?.takeIf { it.isNotEmpty() },
            StatusConsulta.AGENDADA, 1, agoraLocal, agoraLocal)
        escrita.criar("consultas", id, JsonContrato.encodeToJsonElement(pedido).jsonObject, JsonContrato.encodeToString(otimista))
        return otimista
    }

    override suspend fun alterar(id: Uuid, versao: Long, requisicao: AtualizarConsultaRequisicao): ConsultaResposta {
        contexto()
        val sessao = banco.sessaoQueries.atual().executeAsOneOrNull() ?: throw SessaoAlterada()
        val atual = banco.registroSyncQueries.dadosDe(sessao.usuario_id, sessao.perfil, "consultas", id.toString()).executeAsOneOrNull()?.dados_json
            ?.let { JsonContrato.decodeFromString<ConsultaResposta>(it) } ?: throw ErroApi("RECURSO_NAO_ENCONTRADO", null, emptyList(), 404, null, null)
        val nova = atual.copy(inicio = requisicao.inicio ?: atual.inicio, duracaoMin = requisicao.duracaoMin ?: atual.duracaoMin, tipo = requisicao.tipo ?: atual.tipo,
            lugar = if (requisicao.lugar == null) atual.lugar else requisicao.lugar.ifEmpty { null }, status = requisicao.status ?: atual.status,
            versao = versao + 1, atualizadoEm = agora())
        escrita.atualizar("consultas", id, versao, JsonContrato.encodeToJsonElement(requisicao).jsonObject, JsonContrato.encodeToString(nova))
        return nova
    }

    private fun guardar(consulta: ConsultaResposta) {
        val sessao = banco.sessaoQueries.atual().executeAsOneOrNull() ?: return
        val atual = banco.registroSyncQueries.revisao(sessao.usuario_id, sessao.perfil, "consultas", consulta.id.toString()).executeAsOneOrNull()
        if (atual != null && atual > consulta.versao) return
        banco.registroSyncQueries.salvar(sessao.usuario_id, sessao.perfil, "consultas", consulta.id.toString(), consulta.versao, 0, JsonContrato.encodeToString(consulta))
    }

    private fun contexto() = api.operacaoSessao.value?.takeIf { it.contexto.perfil == Perfil.NUTRICIONISTA } ?: throw SessaoAlterada()
    private fun conferir(esperado: OperacaoSessao) { if (api.operacaoSessao.value != esperado) throw SessaoAlterada() }
}
