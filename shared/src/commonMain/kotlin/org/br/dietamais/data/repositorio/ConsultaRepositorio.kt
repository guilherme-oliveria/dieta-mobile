package org.br.dietamais.data.repositorio

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.encodeToJsonElement
import org.br.dietamais.data.local.BancoDieta
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.sync.novoIdLocal
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

class ConsultaRepositorio(private val api: ApiDieta, private val banco: BancoDieta) : PortaConsultas {
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
        val contexto = contexto()
        val id = requisicao.id ?: novoIdLocal()
        val resposta = api.enviar<ConsultaResposta>("consultas", JsonContrato.encodeToJsonElement(requisicao.copy(id = id)), chave = id)
        conferir(contexto)
        if (resposta.id != id || resposta.pacienteId != requisicao.pacienteId) throw RespostaInvalida()
        return resposta.also { guardar(it) }
    }

    override suspend fun alterar(id: Uuid, versao: Long, requisicao: AtualizarConsultaRequisicao): ConsultaResposta {
        val contexto = contexto()
        val resposta = api.alterar<ConsultaResposta>("consultas/$id", JsonContrato.encodeToJsonElement(requisicao), versao)
        conferir(contexto)
        if (resposta.id != id) throw RespostaInvalida()
        return resposta.also { guardar(it) }
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
