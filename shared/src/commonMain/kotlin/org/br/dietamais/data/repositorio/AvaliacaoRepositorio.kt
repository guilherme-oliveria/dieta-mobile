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
import kotlin.uuid.Uuid

interface PortaAvaliacoes {
    /** Avaliações do paciente, da mais recente para a mais antiga. Sem `pacienteId`, todas as da sessão (perfil paciente). */
    fun observar(pacienteId: Uuid?): Flow<List<AvaliacaoResposta>>
    /** Busca no servidor e funde no banco local (a nutri abre a ficha antes do sync alcançar o histórico). */
    suspend fun atualizar(pacienteId: Uuid)
    suspend fun criar(pacienteId: Uuid, requisicao: SalvarAvaliacaoRequisicao): AvaliacaoResposta
    suspend fun salvar(id: Uuid, versao: Long, requisicao: SalvarAvaliacaoRequisicao): AvaliacaoResposta
}

class AvaliacaoRepositorio(private val api: ApiDieta, private val banco: BancoDieta) : PortaAvaliacoes {
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
        val contexto = contexto()
        val id = requisicao.id ?: org.br.dietamais.data.sync.novoIdLocal()
        val corpo = requisicao.copy(id = id)
        val resposta = api.enviar<AvaliacaoResposta>("pacientes/$pacienteId/avaliacoes", JsonContrato.encodeToJsonElement(corpo), chave = id)
        conferir(contexto)
        if (resposta.pacienteId != pacienteId || resposta.id != id) throw RespostaInvalida()
        return resposta.also { guardar(it) }
    }

    override suspend fun salvar(id: Uuid, versao: Long, requisicao: SalvarAvaliacaoRequisicao): AvaliacaoResposta {
        val contexto = contexto()
        val resposta = api.substituir<AvaliacaoResposta>("avaliacoes/$id", JsonContrato.encodeToJsonElement(requisicao.copy(id = id)), versao)
        conferir(contexto)
        if (resposta.id != id) throw RespostaInvalida()
        return resposta.also { guardar(it) }
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
