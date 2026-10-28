package org.br.dietamais.ui.nutri.anamnese

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.PortaAnamnese
import kotlin.time.Instant
import kotlin.uuid.Uuid

val instanteFalso = Instant.parse("2026-10-28T12:00:00Z")

fun perguntaFalsa(texto: String, tipo: TipoPergunta = TipoPergunta.TEXTO, obrigatoria: Boolean = false, opcoes: List<String>? = null, condicao: CondicaoPergunta? = null) =
    PerguntaQuestionario(Uuid.random(), texto, tipo, opcoes, obrigatoria, condicao)

fun questionarioFalso(paciente: Uuid, blocos: List<BlocoQuestionario>, status: StatusQuestionario = StatusQuestionario.PENDENTE, respostas: Map<Uuid, JsonElement> = emptyMap(), nome: String = "Anamnese") =
    QuestionarioResposta(Uuid.random(), paciente, Uuid.random(), null, nome, blocos, respostas, status, instanteFalso, null, 1, instanteFalso, instanteFalso)

/** Imita o repositório: respostas parciais entram no "banco" (flow) na hora; concluir fecha o questionário. */
class PortaAnamneseFalsa(modelos: List<ModeloQuestionarioResposta> = emptyList(), questionarios: List<QuestionarioResposta> = emptyList()) : PortaAnamnese {
    val modelos = MutableStateFlow(modelos)
    val questionarios = MutableStateFlow(questionarios)
    var falha: Exception? = null
    var atualizacoes = 0
    val gravacoes = mutableListOf<Map<Uuid, JsonElement?>>()
    var modeloCriado: SalvarModeloRequisicao? = null
    var modeloSalvo: Pair<Long, SalvarModeloRequisicao>? = null
    var enviado: EnviarQuestionarioRequisicao? = null
    var concluidos = 0

    override fun observarModelos() = modelos
    override suspend fun atualizarModelos() { atualizacoes++; falha?.let { throw it } }
    override suspend fun criarModelo(requisicao: SalvarModeloRequisicao): ModeloQuestionarioResposta {
        falha?.let { throw it }; modeloCriado = requisicao
        return ModeloQuestionarioResposta(requisicao.id!!, requisicao.nome, false, requisicao.blocos, 1, instanteFalso, instanteFalso).also { modelos.value = modelos.value + it }
    }
    override suspend fun salvarModelo(id: Uuid, versao: Long, requisicao: SalvarModeloRequisicao): ModeloQuestionarioResposta {
        falha?.let { throw it }; modeloSalvo = versao to requisicao
        return modelos.value.first { it.id == id }.copy(nome = requisicao.nome, blocos = requisicao.blocos, versao = versao + 1)
    }
    override fun observarQuestionarios(pacienteId: Uuid?) = questionarios.map { l -> l.filter { pacienteId == null || it.pacienteId == pacienteId } }
    override suspend fun atualizarQuestionarios(pacienteId: Uuid) { falha?.let { throw it } }
    override suspend fun enviar(pacienteId: Uuid, requisicao: EnviarQuestionarioRequisicao): QuestionarioResposta {
        falha?.let { throw it }; enviado = requisicao
        val modelo = modelos.value.first { it.id == requisicao.modeloId }
        return QuestionarioResposta(requisicao.id!!, pacienteId, Uuid.random(), modelo.id, modelo.nome, modelo.blocos, emptyMap(), StatusQuestionario.PENDENTE, instanteFalso, null, 1, instanteFalso, instanteFalso)
            .also { questionarios.value = questionarios.value + it }
    }
    override fun salvarRespostas(id: Uuid, respostas: Map<Uuid, JsonElement?>) {
        gravacoes += respostas
        questionarios.value = questionarios.value.map { q ->
            if (q.id != id) q else q.copy(respostas = q.respostas.toMutableMap().also { m -> respostas.forEach { (k, v) -> if (v == null) m.remove(k) else m[k] = v } })
        }
    }
    override suspend fun concluir(id: Uuid): QuestionarioResposta {
        falha?.let { throw it }; concluidos++
        val fechado = questionarios.value.first { it.id == id }.copy(status = StatusQuestionario.RESPONDIDO, respondidoEm = instanteFalso)
        questionarios.value = questionarios.value.map { if (it.id == id) fechado else it }
        return fechado
    }
}

fun texto(valor: String): JsonElement = JsonPrimitive(valor)
