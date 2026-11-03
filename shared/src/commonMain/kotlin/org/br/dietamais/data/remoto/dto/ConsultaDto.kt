package org.br.dietamais.data.remoto.dto

import kotlinx.serialization.Serializable
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Serializable(with = TipoConsultaSerializer::class)
enum class TipoConsulta { PRIMEIRA, RETORNO, ONLINE, OUTRO }
object TipoConsultaSerializer : EnumComOutro<TipoConsulta>("TipoConsulta", TipoConsulta.entries.toTypedArray(), TipoConsulta.OUTRO)

@Serializable(with = StatusConsultaSerializer::class)
enum class StatusConsulta { AGENDADA, CANCELADA, OUTRO }
object StatusConsultaSerializer : EnumComOutro<StatusConsulta>("StatusConsulta", StatusConsulta.entries.toTypedArray(), StatusConsulta.OUTRO)

/** `pacienteId` é o id da ficha. `lugar`: endereço ou link da videochamada. */
@Serializable
data class ConsultaResposta(val id: Uuid, val pacienteId: Uuid, val vinculoId: Uuid, val inicio: Instant, val duracaoMin: Int, val tipo: TipoConsulta,
    val lugar: String? = null, val status: StatusConsulta, val versao: Long, val criadoEm: Instant, val atualizadoEm: Instant) {
    val fim: Instant get() = inicio + kotlin.time.Duration.parse("${duracaoMin}m")
}

@Serializable
data class ConsultasResposta(val itens: List<ConsultaResposta> = emptyList())

@Serializable
data class CriarConsultaRequisicao(val id: Uuid? = null, val pacienteId: Uuid, val inicio: Instant, val duracaoMin: Int, val tipo: TipoConsulta, val lugar: String? = null)

/** Campo nulo = não muda (o JSON do app omite nulos). `lugar` vazio limpa. */
@Serializable
data class AtualizarConsultaRequisicao(val inicio: Instant? = null, val duracaoMin: Int? = null, val tipo: TipoConsulta? = null, val lugar: String? = null, val status: StatusConsulta? = null)
