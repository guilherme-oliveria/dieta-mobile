package org.br.dietamais.data.remoto.dto

import kotlinx.serialization.Serializable
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Serializable(with = ClassificacaoImcSerializer::class)
enum class ClassificacaoImc { ABAIXO_DO_PESO, NORMAL, SOBREPESO, OBESIDADE_I, OBESIDADE_II, OBESIDADE_III, OUTRO }
object ClassificacaoImcSerializer : EnumComOutro<ClassificacaoImc>("ClassificacaoImc", ClassificacaoImc.entries.toTypedArray(), ClassificacaoImc.OUTRO)

@Serializable
data class PerimetrosAvaliacao(val pescoco: Double? = null, val torax: Double? = null, val cintura: Double? = null, val abdomen: Double? = null,
    val quadril: Double? = null, val braco: Double? = null, val antebraco: Double? = null, val coxa: Double? = null, val panturrilha: Double? = null)
@Serializable
data class DobrasAvaliacao(val triceps: Double? = null, val subescapular: Double? = null, val peitoral: Double? = null, val axilarMedia: Double? = null,
    val suprailiaca: Double? = null, val abdominal: Double? = null, val coxa: Double? = null)
@Serializable
data class ComposicaoAvaliacao(val percentualGordura: Double? = null, val pesoGordoKg: Double? = null, val pesoMagroKg: Double? = null)

@Serializable
data class AvaliacaoResposta(val id: Uuid, val pacienteId: Uuid, val vinculoId: Uuid, val dataHora: Instant, val pesoKg: Double, val estaturaM: Double,
    val imc: Double, val classificacaoImc: ClassificacaoImc, val perimetros: PerimetrosAvaliacao = PerimetrosAvaliacao(),
    val dobras: DobrasAvaliacao = DobrasAvaliacao(), val composicao: ComposicaoAvaliacao = ComposicaoAvaliacao(), val parecer: String? = null,
    val compartilhada: Boolean = true, val versao: Long, val criadoEm: Instant, val atualizadoEm: Instant)

@Serializable
data class AvaliacoesResposta(val itens: List<AvaliacaoResposta> = emptyList())

@Serializable
data class SalvarAvaliacaoRequisicao(val id: Uuid? = null, val dataHora: Instant, val pesoKg: Double, val estaturaM: Double,
    val perimetros: PerimetrosAvaliacao = PerimetrosAvaliacao(), val dobras: DobrasAvaliacao = DobrasAvaliacao(),
    val composicao: ComposicaoAvaliacao = ComposicaoAvaliacao(), val parecer: String? = null, val compartilhada: Boolean? = null,
    val justificativa: String? = null)
