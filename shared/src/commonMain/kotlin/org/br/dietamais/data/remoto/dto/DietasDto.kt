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

/** Meta menos o total do dia (BE-49a); negativo = passou da meta. Carboidrato, proteína e lipídio em gramas. */
@Serializable
data class SaldoMetasResposta(val kcal: Double, val carboidrato: Double, val proteina: Double, val lipidio: Double)
/** Total do dia contra a meta DRI (BE-49b); `classe` é ABAIXO, ADEQUADO ou ACIMA (texto livre: o servidor pode criar outras). */
@Serializable
data class AdequacaoNutrienteResposta(val nutriente: String, val total: Double, val meta: Double, val percentual: Double, val classe: String)
/** Total da alternativa menos o da refeição que ela substitui (RN-DIE-11). */
@Serializable
data class DiferencaTotaisResposta(val kcal: Double, val carboidratos: Double, val proteinas: Double, val gorduras: Double)

@Serializable
data class TotaisNutricionaisResposta(val kcal: Double, val carboidratos: Double, val proteinas: Double, val gorduras: Double,
    val saldoMetas: SaldoMetasResposta? = null, val micronutrientes: Map<String, Double>? = null, val incompleto: List<String>? = null,
    val adequacao: List<AdequacaoNutrienteResposta>? = null)

/** Porção escolhida para um grupo da dieta (RN-DIE-10). */
@Serializable
data class PorcaoGrupo(val grupoId: Uuid, val opcao: Int)
/** Alvo da dieta: VET e % de macros (BE-49a). */
@Serializable
data class MetasDieta(val vetKcal: Double, val carboidratoPct: Double, val proteinaPct: Double, val lipidioPct: Double)

@Serializable
data class ItemRefeicaoResposta(val id: Uuid, val alimentoId: Uuid? = null, val grupoId: Uuid? = null, val nomeAlimento: String? = null,
    val porcoes: Double? = null, val medidaCaseira: String? = null, val gramas: Double? = null, val observacao: String? = null, val totais: TotaisNutricionaisResposta,
    val grupoNome: String? = null, val opcao: Int? = null, val aVontade: Boolean? = null, val textoLivre: String? = null)
@Serializable
data class RefeicaoResposta(val id: Uuid, val nome: String, val horario: LocalTime, val ordem: Int,
    val itens: List<ItemRefeicaoResposta>, val totais: TotaisNutricionaisResposta,
    val tipo: String? = null, val alternativaDe: Uuid? = null, val diferencaParaPrincipal: DiferencaTotaisResposta? = null) {
    /** RN-DIE-11: ausente vale principal (inclusive em snapshots antigos). */
    val ehAlternativa: Boolean get() = tipo == "ALTERNATIVA"
}
@Serializable
data class DietaResposta(val id: Uuid, val pacienteId: Uuid, val vinculoId: Uuid, val titulo: String, val status: StatusDieta,
    val orientacao: String? = null, val refeicoes: List<RefeicaoResposta>, val totais: TotaisNutricionaisResposta,
    val versao: Long, val criadoEm: Instant, val atualizadoEm: Instant,
    val porcoes: List<PorcaoGrupo>? = null, val metas: MetasDieta? = null, val pesoReferenciaKg: Double? = null, val rodape: String? = null)
@Serializable
data class DietasResposta(val itens: List<DietaResposta>)

@Serializable
data class ItemRefeicaoRequisicao(val id: Uuid? = null, val alimentoId: Uuid? = null, val porcoes: Double? = null, val observacao: String? = null,
    val grupoId: Uuid? = null, val aVontade: Boolean? = null, val textoLivre: String? = null)
@Serializable
data class RefeicaoRequisicao(val id: Uuid? = null, val nome: String, val horario: LocalTime, val ordem: Int, val itens: List<ItemRefeicaoRequisicao>,
    val tipo: String? = null, val alternativaDe: Uuid? = null)
@Serializable
data class SalvarDietaRequisicao(val id: Uuid? = null, val titulo: String, val orientacao: String? = null, val refeicoes: List<RefeicaoRequisicao>,
    val porcoes: List<PorcaoGrupo>? = null, val metas: MetasDieta? = null, val rodape: String? = null)

@Serializable
data class DietaSnapshotResposta(val id: Uuid, val dietaId: Uuid, val pacienteId: Uuid, val vinculoId: Uuid,
    val nutricionista: NutricionistaResumoResposta, val titulo: String, val orientacao: String? = null,
    val refeicoes: List<RefeicaoResposta>, val listaSubstituicao: ListaSubstituicaoResposta,
    val totais: TotaisNutricionaisResposta, val publicadoEm: Instant,
    val porcoes: List<PorcaoGrupo>? = null, val metas: MetasDieta? = null, val pesoReferenciaKg: Double? = null, val rodape: String? = null)
@Serializable
data class DietasPacienteResposta(val dietaEmUsoId: Uuid? = null, val versao: Long, val itens: List<DietaSnapshotResposta>)
@Serializable
data class DietaEmUsoResposta(val dietaId: Uuid, val versao: Long)
@Serializable
data class SelecionarDietaEmUsoRequisicao(val dietaId: Uuid)
