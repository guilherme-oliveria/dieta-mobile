package org.br.dietamais.data.online

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.json.encodeToJsonElement
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.PortaAvaliacoes
import kotlin.uuid.Uuid

/** Avaliações físicas direto da API (web). O IMC vem calculado pelo servidor. */
class AvaliacoesOnline(private val api: ApiDieta) : PortaAvaliacoes {
    private val recurso = RecursoOnline<AvaliacaoResposta>(api.sessao) { emptyList() }

    override fun observar(pacienteId: Uuid?): Flow<List<AvaliacaoResposta>> = recurso.fluxo.map { lista ->
        lista.filter { pacienteId == null || it.pacienteId == pacienteId }
            .sortedWith(compareByDescending<AvaliacaoResposta> { it.dataHora }.thenByDescending { it.criadoEm })
    }.let { fluxo ->
        // Paciente: sem `atualizar` na tela, então a primeira leitura vem ao abrir (só as avaliações que a nutri compartilhou).
        if (pacienteId == null && api.sessao.value?.perfil == Perfil.PACIENTE) fluxo.onStart { carregarMinhas() } else fluxo
    }

    private suspend fun carregarMinhas() {
        val esperado = api.operacaoSessao.value?.takeIf { it.contexto.perfil == Perfil.PACIENTE } ?: throw SessaoAlterada()
        val resposta = api.obter<AvaliacoesResposta>("eu/avaliacoes")
        if (api.operacaoSessao.value != esperado) throw SessaoAlterada()
        resposta.itens.forEach { nova -> recurso.gravado(nova) { it.id == nova.id } }
    }

    override suspend fun atualizar(pacienteId: Uuid) {
        val esperado = contexto()
        val resposta = api.obter<AvaliacoesResposta>("pacientes/$pacienteId/avaliacoes")
        conferir(esperado)
        if (resposta.itens.any { it.pacienteId != pacienteId }) throw RespostaInvalida()
        resposta.itens.forEach { nova -> recurso.gravado(nova) { it.id == nova.id } }
    }

    override suspend fun criar(pacienteId: Uuid, requisicao: SalvarAvaliacaoRequisicao): AvaliacaoResposta {
        val esperado = contexto()
        val criada = api.enviar<AvaliacaoResposta>("pacientes/$pacienteId/avaliacoes", JsonContrato.encodeToJsonElement(requisicao), requisicao.id ?: Uuid.random())
        conferir(esperado)
        if (criada.pacienteId != pacienteId) throw RespostaInvalida()
        recurso.gravado(criada) { it.id == criada.id }
        return criada
    }

    override suspend fun salvar(id: Uuid, versao: Long, requisicao: SalvarAvaliacaoRequisicao): AvaliacaoResposta {
        val esperado = contexto()
        val salva = api.substituir<AvaliacaoResposta>("avaliacoes/$id", JsonContrato.encodeToJsonElement(requisicao), versao)
        conferir(esperado)
        if (salva.id != id) throw RespostaInvalida()
        recurso.gravado(salva) { it.id == id }
        return salva
    }

    private fun contexto() = api.operacaoSessao.value?.takeIf { it.contexto.perfil == Perfil.NUTRICIONISTA } ?: throw SessaoAlterada()
    private fun conferir(esperado: OperacaoSessao) { if (api.operacaoSessao.value != esperado) throw SessaoAlterada() }
}
