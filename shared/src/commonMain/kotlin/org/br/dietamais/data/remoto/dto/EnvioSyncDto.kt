package org.br.dietamais.data.remoto.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlin.uuid.Uuid

@Serializable(with = StatusMutacaoSerializer::class)
enum class StatusMutacao { APLICADA, CONFLITO, REJEITADA, OUTRO }
object StatusMutacaoSerializer : EnumComOutro<StatusMutacao>("StatusMutacao", StatusMutacao.entries.toTypedArray(), StatusMutacao.OUTRO)

@Serializable
data class MutacaoRequisicao(val idMutacao: Uuid, val entidade: String, val operacao: String, val id: Uuid, val versaoBase: Long? = null, val dados: JsonObject)
@Serializable
data class EnviarMutacoesRequisicao(val mutacoes: List<MutacaoRequisicao>)
@Serializable
data class ResultadoMutacaoResposta(val idMutacao: Uuid, val status: StatusMutacao, val versao: Long? = null,
    val codigo: String? = null, val regra: String? = null, val atual: JsonElement? = null)
@Serializable
data class EnviarMutacoesResposta(val resultados: List<ResultadoMutacaoResposta>)
