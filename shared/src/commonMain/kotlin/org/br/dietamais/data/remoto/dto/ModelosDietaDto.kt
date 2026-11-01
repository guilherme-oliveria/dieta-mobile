package org.br.dietamais.data.remoto.dto

import kotlinx.serialization.Serializable
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * Modelo de dieta (BE-52). `escopo` é `SISTEMA` (somente leitura, RN-MOD-01) ou `NUTRI`. `restricoes` são marcadores de alimento
 * (`CONTEM_LACTOSE`, `CONTEM_GLUTEN`, `ALTO_SODIO`, `ACUCAR_ADICIONADO`, `ALTO_POTASSIO`), guardados como texto para tolerar valor novo.
 */
@Serializable
data class ModeloDietaResposta(val id: Uuid, val escopo: String, val titulo: String, val descricao: String? = null, val condicao: String? = null,
    val orientacao: String? = null, val refeicoes: List<RefeicaoRequisicao>, val porcoes: List<PorcaoGrupo>? = null, val metas: MetasDieta? = null,
    val rodape: String? = null, val restricoes: List<String> = emptyList(), val kcalReferencia: Double? = null,
    val versao: Long, val criadoEm: Instant, val atualizadoEm: Instant) {
    val doSistema: Boolean get() = escopo == "SISTEMA"
    /** Energia que o modelo entrega: a de referência ou, sem ela, o VET das metas. */
    val kcal: Double? get() = kcalReferencia ?: metas?.vetKcal
    val refeicoesPrincipais: Int get() = refeicoes.count { it.tipo != "ALTERNATIVA" }
}
@Serializable
data class ModelosDietaResposta(val itens: List<ModeloDietaResposta>)

/** Salva uma dieta dela como modelo (BE-52): o conteúdo vem da dieta no servidor. */
@Serializable
data class SalvarComoModeloRequisicao(val id: Uuid? = null, val titulo: String, val descricao: String? = null, val condicao: String? = null,
    val restricoes: List<String>? = null)
