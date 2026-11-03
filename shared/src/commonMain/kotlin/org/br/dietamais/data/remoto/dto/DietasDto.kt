package org.br.dietamais.data.remoto.dto

import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Serializable(with = StatusDietaSerializer::class)
enum class StatusDieta { RASCUNHO, PUBLICADA, SUBSTITUIDA, ARQUIVADA, OUTRO }
object StatusDietaSerializer : EnumComOutro<StatusDieta>("StatusDieta", StatusDieta.entries.toTypedArray(), StatusDieta.OUTRO)

@Serializable
data class ListaSubstituicaoResposta(val pacienteId: Uuid, val versao: Long, val alimentos: List<AlimentoResposta>,
    val adicionados: List<Uuid>, val removidos: List<Uuid>)
@Serializable
data class AtualizarListaSubstituicaoRequisicao(val adicionados: List<Uuid>, val removidos: List<Uuid>)

@Serializable
data class TotaisNutricionaisResposta(val kcal: Double, val carboidratos: Double, val proteinas: Double, val gorduras: Double)

@Serializable
data class ItemRefeicaoResposta(val id: Uuid, val alimentoId: Uuid, val grupoId: Uuid, val nomeAlimento: String,
    val porcoes: Double, val medidaCaseira: String, val gramas: Double, val observacao: String? = null, val totais: TotaisNutricionaisResposta,
    val grupoNome: String? = null)
@Serializable
data class RefeicaoResposta(val id: Uuid, val nome: String, val horario: LocalTime, val ordem: Int,
    val itens: List<ItemRefeicaoResposta>, val totais: TotaisNutricionaisResposta)
@Serializable
data class DietaResposta(val id: Uuid, val pacienteId: Uuid, val vinculoId: Uuid, val titulo: String, val status: StatusDieta,
    val orientacao: String? = null, val refeicoes: List<RefeicaoResposta>, val totais: TotaisNutricionaisResposta,
    val versao: Long, val criadoEm: Instant, val atualizadoEm: Instant)
@Serializable
data class DietasResposta(val itens: List<DietaResposta>)

@Serializable
data class ItemRefeicaoRequisicao(val id: Uuid? = null, val alimentoId: Uuid, val porcoes: Double, val observacao: String? = null)
@Serializable
data class RefeicaoRequisicao(val id: Uuid? = null, val nome: String, val horario: LocalTime, val ordem: Int, val itens: List<ItemRefeicaoRequisicao>)
@Serializable
data class SalvarDietaRequisicao(val id: Uuid? = null, val titulo: String, val orientacao: String? = null, val refeicoes: List<RefeicaoRequisicao>)

@Serializable
data class DietaSnapshotResposta(val id: Uuid, val dietaId: Uuid, val pacienteId: Uuid, val vinculoId: Uuid,
    val nutricionista: NutricionistaResumoResposta, val titulo: String, val orientacao: String? = null,
    val refeicoes: List<RefeicaoResposta>, val listaSubstituicao: ListaSubstituicaoResposta,
    val totais: TotaisNutricionaisResposta, val publicadoEm: Instant)
@Serializable
data class DietasPacienteResposta(val dietaEmUsoId: Uuid? = null, val versao: Long, val itens: List<DietaSnapshotResposta>)
@Serializable
data class DietaEmUsoResposta(val dietaId: Uuid, val versao: Long)
@Serializable
data class SelecionarDietaEmUsoRequisicao(val dietaId: Uuid)
