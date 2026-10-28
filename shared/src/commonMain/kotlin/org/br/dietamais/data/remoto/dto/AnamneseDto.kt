package org.br.dietamais.data.remoto.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Serializable(with = TipoPerguntaSerializer::class)
enum class TipoPergunta { TEXTO, NUMERO, ESCOLHA_UNICA, MULTIPLA_ESCOLHA, ESCALA_1_5, HORARIO, LISTA_POR_REFEICAO, OUTRO }
object TipoPerguntaSerializer : EnumComOutro<TipoPergunta>("TipoPergunta", TipoPergunta.entries.toTypedArray(), TipoPergunta.OUTRO)

@Serializable(with = OperadorCondicaoSerializer::class)
enum class OperadorCondicao { IGUAL, DIFERENTE, CONTEM, OUTRO }
object OperadorCondicaoSerializer : EnumComOutro<OperadorCondicao>("OperadorCondicao", OperadorCondicao.entries.toTypedArray(), OperadorCondicao.OUTRO)

@Serializable(with = StatusQuestionarioSerializer::class)
enum class StatusQuestionario { PENDENTE, RESPONDIDO, OUTRO }
object StatusQuestionarioSerializer : EnumComOutro<StatusQuestionario>("StatusQuestionario", StatusQuestionario.entries.toTypedArray(), StatusQuestionario.OUTRO)

@Serializable
data class CondicaoPergunta(val perguntaId: Uuid, val operador: OperadorCondicao, val valor: String)

@Serializable
data class PerguntaQuestionario(val id: Uuid? = null, val texto: String, val tipo: TipoPergunta, val opcoes: List<String>? = null,
    val obrigatoria: Boolean = false, val condicao: CondicaoPergunta? = null)

@Serializable
data class BlocoQuestionario(val titulo: String, val perguntas: List<PerguntaQuestionario> = emptyList())

@Serializable
data class ModeloQuestionarioResposta(val id: Uuid, val nome: String, val padrao: Boolean = false, val blocos: List<BlocoQuestionario> = emptyList(),
    val versao: Long, val criadoEm: Instant, val atualizadoEm: Instant)
@Serializable
data class ModelosResposta(val itens: List<ModeloQuestionarioResposta> = emptyList())

@Serializable
data class SalvarModeloRequisicao(val id: Uuid? = null, val nome: String, val blocos: List<BlocoQuestionario>)
@Serializable
data class EnviarQuestionarioRequisicao(val id: Uuid? = null, val modeloId: Uuid)

@Serializable
data class QuestionarioResposta(val id: Uuid, val pacienteId: Uuid, val vinculoId: Uuid, val modeloId: Uuid? = null, val nome: String,
    val blocos: List<BlocoQuestionario> = emptyList(), val respostas: Map<Uuid, JsonElement> = emptyMap(), val status: StatusQuestionario,
    val enviadoEm: Instant, val respondidoEm: Instant? = null, val versao: Long, val criadoEm: Instant, val atualizadoEm: Instant)
@Serializable
data class QuestionariosResposta(val itens: List<QuestionarioResposta> = emptyList())

/** Valor nulo apaga a resposta daquela pergunta; ausentes ficam como estão (mescla por pergunta). */
@Serializable
data class SalvarRespostasRequisicao(val respostas: Map<Uuid, JsonElement?>)
