package org.br.dietamais.data.online

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.json.encodeToJsonElement
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.PortaConsultas
import kotlin.time.Instant
import kotlin.uuid.Uuid

/** Agenda da nutri direto da API (web). O conflito de horário (RN-AGE-01) é decidido pelo servidor e volta como erro da chamada. */
class ConsultasOnline(private val api: ApiDieta) : PortaConsultas {
    private val recurso = RecursoOnline<ConsultaResposta>(api.sessao) { emptyList() }

    override fun observar(): Flow<List<ConsultaResposta>> =
        recurso.fluxo.map { it.sortedWith(compareBy<ConsultaResposta> { c -> c.inicio }.thenBy { c -> c.id.toString() }) }
            .let { fluxo -> if (api.sessao.value?.perfil == Perfil.PACIENTE) fluxo.onStart { carregarMinhas() } else fluxo }

    /** Paciente: só leitura, janela padrão do servidor (de ontem a 90 dias). */
    private suspend fun carregarMinhas() {
        val esperado = api.operacaoSessao.value?.takeIf { it.contexto.perfil == Perfil.PACIENTE } ?: throw SessaoAlterada()
        val resposta = api.obter<ConsultasResposta>("eu/consultas")
        if (api.operacaoSessao.value != esperado) throw SessaoAlterada()
        resposta.itens.forEach { nova -> recurso.gravado(nova) { it.id == nova.id } }
    }

    override suspend fun atualizar(de: Instant, ate: Instant) {
        val esperado = contexto()
        val resposta = api.obter<ConsultasResposta>("consultas", mapOf("de" to de.toString(), "ate" to ate.toString()))
        conferir(esperado)
        resposta.itens.forEach { nova -> recurso.gravado(nova) { it.id == nova.id } }
    }

    override suspend fun criar(requisicao: CriarConsultaRequisicao): ConsultaResposta {
        val esperado = contexto()
        val criada = api.enviar<ConsultaResposta>("consultas", JsonContrato.encodeToJsonElement(requisicao), requisicao.id ?: Uuid.random())
        conferir(esperado)
        recurso.gravado(criada) { it.id == criada.id }
        return criada
    }

    override suspend fun alterar(id: Uuid, versao: Long, requisicao: AtualizarConsultaRequisicao): ConsultaResposta {
        val esperado = contexto()
        val alterada = api.alterar<ConsultaResposta>("consultas/$id", JsonContrato.encodeToJsonElement(requisicao), versao)
        conferir(esperado)
        if (alterada.id != id) throw RespostaInvalida()
        recurso.gravado(alterada) { it.id == id }
        return alterada
    }

    private fun contexto() = api.operacaoSessao.value?.takeIf { it.contexto.perfil == Perfil.NUTRICIONISTA } ?: throw SessaoAlterada()
    private fun conferir(esperado: OperacaoSessao) { if (api.operacaoSessao.value != esperado) throw SessaoAlterada() }
}
