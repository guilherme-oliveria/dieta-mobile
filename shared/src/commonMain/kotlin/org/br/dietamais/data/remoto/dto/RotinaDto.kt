package org.br.dietamais.data.remoto.dto

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Serializable(with = StatusCheckinSerializer::class)
enum class StatusCheckin { FEITO, TROCADO, PULADO, OUTRO }
object StatusCheckinSerializer : EnumComOutro<StatusCheckin>("StatusCheckin", StatusCheckin.entries.toTypedArray(), StatusCheckin.OUTRO)

@Serializable
data class TrocaCheckin(val itemId: Uuid, val alimentoId: Uuid)

@Serializable
data class CheckinResposta(val id: Uuid, val pacienteId: Uuid, val dietaId: Uuid, val refeicaoId: Uuid, val data: LocalDate,
    val status: StatusCheckin, val trocas: List<TrocaCheckin> = emptyList(), val registradoEm: Instant)
@Serializable
data class AguaResposta(val id: Uuid, val pacienteId: Uuid, val ml: Int, val registradoEm: Instant)
@Serializable
data class PesoResposta(val id: Uuid, val pacienteId: Uuid, val pesoKg: Double, val registradoEm: Instant)

@Serializable
data class CheckinRequisicao(val id: Uuid? = null, val dietaId: Uuid, val refeicaoId: Uuid, val data: LocalDate, val status: StatusCheckin,
    val trocas: List<TrocaCheckin> = emptyList(), val registradoEm: Instant)
@Serializable
data class AguaRequisicao(val id: Uuid? = null, val ml: Int, val registradoEm: Instant)
@Serializable
data class PesoRequisicao(val id: Uuid? = null, val pesoKg: Double, val registradoEm: Instant)

@Serializable
data class RotinaResposta(val checkins: List<CheckinResposta> = emptyList(), val agua: List<AguaResposta> = emptyList(), val pesos: List<PesoResposta> = emptyList())
