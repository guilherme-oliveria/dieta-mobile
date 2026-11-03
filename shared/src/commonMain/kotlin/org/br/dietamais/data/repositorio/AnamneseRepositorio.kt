package org.br.dietamais.data.repositorio

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import org.br.dietamais.data.local.BancoDieta
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.sync.FilaMutacoes
import org.br.dietamais.data.sync.NovaMutacao
import org.br.dietamais.data.sync.OperacaoMutacao
import org.br.dietamais.data.sync.novoIdLocal
import kotlin.uuid.Uuid

/** Respostas parciais ainda não confirmadas pelo servidor: concluir antes disso perderia o que o paciente digitou. */
class RespostasPendentes : Exception("Há respostas aguardando sincronização.")

interface PortaAnamnese {
    fun observarModelos(): Flow<List<ModeloQuestionarioResposta>>
    suspend fun atualizarModelos()
    suspend fun criarModelo(requisicao: SalvarModeloRequisicao): ModeloQuestionarioResposta
    suspend fun salvarModelo(id: Uuid, versao: Long, requisicao: SalvarModeloRequisicao): ModeloQuestionarioResposta

    /** Nutri: passe o id da ficha. Paciente: nulo. Mais recentes primeiro. */
    fun observarQuestionarios(pacienteId: Uuid?): Flow<List<QuestionarioResposta>>
    suspend fun atualizarQuestionarios(pacienteId: Uuid)
    suspend fun enviar(pacienteId: Uuid, requisicao: EnviarQuestionarioRequisicao): QuestionarioResposta

    /** Offline-first (paciente): mescla no banco local e enfileira `ATUALIZAR`; valor nulo apaga a resposta. */
    fun salvarRespostas(id: Uuid, respostas: Map<Uuid, JsonElement?>)
    /** Online (RN-ANA-03): torna as respostas imutáveis. Falha com [RespostasPendentes] se a fila ainda tem respostas deste questionário. */
    suspend fun concluir(id: Uuid): QuestionarioResposta
}

class AnamneseRepositorio(private val api: ApiDieta, private val banco: BancoDieta, private val fila: FilaMutacoes = FilaMutacoes(banco)) : PortaAnamnese {
    override fun observarModelos(): Flow<List<ModeloQuestionarioResposta>> =
        banco.registroSyncQueries.daSessao("modelos").asFlow().mapToList(Dispatchers.Default).map { linhas ->
            try { linhas.map { JsonContrato.decodeFromString<ModeloQuestionarioResposta>(it) }.sortedWith(compareByDescending<ModeloQuestionarioResposta> { it.padrao }.thenBy { it.criadoEm }) }
            catch (_: Exception) { throw RespostaInvalida() }
        }

    override suspend fun atualizarModelos() {
        val contexto = contextoNutri()
        val resposta = api.obter<ModelosResposta>("modelos-questionario")
        conferir(contexto)
        resposta.itens.forEach { guardar("modelos", it.id, it.versao, JsonContrato.encodeToString(it)) }
    }

    override suspend fun criarModelo(requisicao: SalvarModeloRequisicao): ModeloQuestionarioResposta {
        val contexto = contextoNutri()
        val id = requisicao.id ?: novoIdLocal()
        val resposta = api.enviar<ModeloQuestionarioResposta>("modelos-questionario", JsonContrato.encodeToJsonElement(requisicao.copy(id = id)), chave = id)
        conferir(contexto)
        if (resposta.id != id) throw RespostaInvalida()
        return resposta.also { guardar("modelos", it.id, it.versao, JsonContrato.encodeToString(it)) }
    }

    override suspend fun salvarModelo(id: Uuid, versao: Long, requisicao: SalvarModeloRequisicao): ModeloQuestionarioResposta {
        val contexto = contextoNutri()
        val resposta = api.substituir<ModeloQuestionarioResposta>("modelos-questionario/$id", JsonContrato.encodeToJsonElement(requisicao.copy(id = id)), versao)
        conferir(contexto)
        if (resposta.id != id) throw RespostaInvalida()
        return resposta.also { guardar("modelos", it.id, it.versao, JsonContrato.encodeToString(it)) }
    }

    override fun observarQuestionarios(pacienteId: Uuid?): Flow<List<QuestionarioResposta>> =
        banco.registroSyncQueries.daSessao("questionarios").asFlow().mapToList(Dispatchers.Default).map { linhas ->
            try {
                linhas.map { JsonContrato.decodeFromString<QuestionarioResposta>(it) }.filter { pacienteId == null || it.pacienteId == pacienteId }
                    .sortedWith(compareByDescending<QuestionarioResposta> { it.enviadoEm }.thenByDescending { it.criadoEm })
            } catch (_: Exception) { throw RespostaInvalida() }
        }

    override suspend fun atualizarQuestionarios(pacienteId: Uuid) {
        val contexto = contextoNutri()
        val resposta = api.obter<QuestionariosResposta>("pacientes/$pacienteId/questionarios")
        conferir(contexto)
        if (resposta.itens.any { it.pacienteId != pacienteId }) throw RespostaInvalida()
        resposta.itens.forEach { guardar("questionarios", it.id, it.versao, JsonContrato.encodeToString(it)) }
    }

    override suspend fun enviar(pacienteId: Uuid, requisicao: EnviarQuestionarioRequisicao): QuestionarioResposta {
        val contexto = contextoNutri()
        val id = requisicao.id ?: novoIdLocal()
        val resposta = api.enviar<QuestionarioResposta>("pacientes/$pacienteId/questionarios", JsonContrato.encodeToJsonElement(requisicao.copy(id = id)), chave = id)
        conferir(contexto)
        if (resposta.id != id || resposta.pacienteId != pacienteId) throw RespostaInvalida()
        return resposta.also { guardar("questionarios", it.id, it.versao, JsonContrato.encodeToString(it)) }
    }

    override fun salvarRespostas(id: Uuid, respostas: Map<Uuid, JsonElement?>) {
        if (respostas.isEmpty()) return
        val corpo = JsonContrato.encodeToJsonElement(SalvarRespostasRequisicao(respostas)).jsonObject
        fila.gravarEEnfileirar(NovaMutacao("questionarios", OperacaoMutacao.ATUALIZAR, id, corpo)) {
            val sessao = checkNotNull(sessaoQueries.atual().executeAsOneOrNull())
            val json = registroSyncQueries.dadosDe(sessao.usuario_id, sessao.perfil, "questionarios", id.toString()).executeAsOneOrNull()?.dados_json
            if (json != null) {
                val atual = JsonContrato.decodeFromString<QuestionarioResposta>(json)
                if (atual.status != StatusQuestionario.PENDENTE) throw RespostasPendentes()
                val mescladas = atual.respostas.toMutableMap()
                respostas.forEach { (pergunta, valor) -> if (valor == null || valor is JsonNull) mescladas.remove(pergunta) else mescladas[pergunta] = valor }
                // a revisão local não muda: a versão nova só chega pelo sync, depois que o servidor aplicar a mutação
                val revisao = registroSyncQueries.revisao(sessao.usuario_id, sessao.perfil, "questionarios", id.toString()).executeAsOne()
                registroSyncQueries.salvar(sessao.usuario_id, sessao.perfil, "questionarios", id.toString(), revisao, 0, JsonContrato.encodeToString(atual.copy(respostas = mescladas)))
            }
        }
    }

    override suspend fun concluir(id: Uuid): QuestionarioResposta {
        val contexto = contextoPaciente()
        if (fila.pendentes().any { it.entidade == "questionarios" && it.id == id }) throw RespostasPendentes()
        val resposta = api.enviar<QuestionarioResposta>("eu/questionarios/$id/enviar", null, chave = novoIdLocal())
        conferir(contexto)
        if (resposta.id != id) throw RespostaInvalida()
        return resposta.also { guardar("questionarios", it.id, it.versao, JsonContrato.encodeToString(it)) }
    }

    private fun guardar(colecao: String, id: Uuid, versao: Long, json: String) {
        val sessao = banco.sessaoQueries.atual().executeAsOneOrNull() ?: return
        val atual = banco.registroSyncQueries.revisao(sessao.usuario_id, sessao.perfil, colecao, id.toString()).executeAsOneOrNull()
        if (atual != null && atual > versao) return
        banco.registroSyncQueries.salvar(sessao.usuario_id, sessao.perfil, colecao, id.toString(), versao, 0, json)
    }

    private fun contextoNutri() = api.operacaoSessao.value?.takeIf { it.contexto.perfil == Perfil.NUTRICIONISTA } ?: throw SessaoAlterada()
    private fun contextoPaciente() = api.operacaoSessao.value?.takeIf { it.contexto.perfil == Perfil.PACIENTE } ?: throw SessaoAlterada()
    private fun conferir(esperado: OperacaoSessao) { if (api.operacaoSessao.value != esperado) throw SessaoAlterada() }
}
