package org.br.dietamais.data.remoto.dto

import kotlinx.serialization.Serializable

/** BE-55b: valores textuais desconhecidos são preservados; o backend valida as escolhas na escrita. */
@Serializable
data class DadosInfantisAvaliacaoDto(val pesoNascerGramas: Int? = null, val modoCurva: String? = null,
    val etniaSlaughter: String? = null, val fatorAtividade: Double? = null)

/** Extremos abertos são nulos. Limites distintos são exclusivos; iguais indicam corte exato publicado. */
@Serializable
data class IntervaloCurvaDto(val inferior: Double? = null, val superior: Double? = null)

/** Snapshot do servidor. Nenhum campo representa um percentil/Z interpolado pelo app. */
@Serializable
data class CurvaCrescimentoAvaliacaoDto(val indicador: String? = null, val valor: Double? = null, val unidade: String? = null,
    val idadeMesesReferencia: Int? = null, val modo: String? = null, val classificacao: String? = null,
    val percentil: IntervaloCurvaDto? = null, val escoreZ: IntervaloCurvaDto? = null,
    val cortesPercentis: List<Double>? = null, val cortesEscoresZ: List<Double>? = null,
    val fonte: String? = null, val versaoReferencia: String? = null, val motivo: String? = null)

@Serializable
data class FrisanchoAvaliacaoDto(val indicador: String? = null, val valor: Double? = null, val unidade: String? = null,
    val idadeAnosReferencia: Int? = null, val p5: Double? = null, val p10: Double? = null, val p50: Double? = null, val p90: Double? = null,
    val adequacaoPercentual: Double? = null, val classificacao: String? = null, val percentil: IntervaloCurvaDto? = null,
    val fonte: String? = null, val versaoReferencia: String? = null, val motivo: String? = null)

@Serializable
data class ComposicaoInfantilDto(val percentualGordura: Double? = null, val classificacaoPercentualGordura: String? = null,
    val percentualMagro: Double? = null, val pesoGordoKg: Double? = null, val pesoMagroKg: Double? = null,
    val motivosNulos: Map<String, String>? = null)

@Serializable
data class EnergiaInfantilDto(val gebIomKcal: Double? = null, val getIomKcal: Double? = null, val getRdiKcal: Double? = null,
    val motivosNulos: Map<String, String>? = null)

/** Ausente em adultos e avaliações anteriores. Não usar classificação adulta como fallback para menores. */
@Serializable
data class ResultadosInfantisAvaliacaoDto(val idadeMeses: Int? = null, val idadeTexto: String? = null, val sexoReferencia: String? = null,
    val modoCurva: String? = null, val classificacaoPesoNascer: String? = null, val pesoTeoricoMinKg: Double? = null, val pesoTeoricoMaxKg: Double? = null,
    val curvas: List<CurvaCrescimentoAvaliacaoDto>? = null, val frisancho: List<FrisanchoAvaliacaoDto>? = null,
    val composicao: ComposicaoInfantilDto? = null, val energia: EnergiaInfantilDto? = null, val motivosNulos: Map<String, String>? = null)

/** BE-55c: resultado calculado de uma avaliação ainda não gravada. Só `resultadosInfantis` interessa ao painel "Resultados ao vivo". */
@Serializable
data class PreviaAvaliacaoDto(val resultadosInfantis: ResultadosInfantisAvaliacaoDto? = null)
