package org.br.dietamais.dominio.avaliacao

import org.br.dietamais.data.remoto.dto.AvaliacaoResposta

/** Os 4 gráficos do PDF da avaliação do adulto (ART-P1-09 e ART-P1-10). */
enum class SerieGraficoAvaliacao(val unidade: String) { PESO("kg"), PERIMETROS("cm"), DOBRAS("mm"), COMPOSICAO("kg") }

/** O que cada barra representa: um ponto do peso (mínimo, atual, máximo) ou uma medida da avaliação. */
sealed interface ChaveGrafico {
    enum class Peso : ChaveGrafico { MINIMO, ATUAL, MAXIMO }
    data class Medida(val medida: MedidaAvaliacao) : ChaveGrafico
}

data class PontoGraficoAvaliacao(val chave: ChaveGrafico, val valor: Double)
data class GraficoAvaliacao(val serie: SerieGraficoAvaliacao, val pontos: List<PontoGraficoAvaliacao>)

/**
 * Dados dos 4 gráficos a partir do que a avaliação já traz (o backend filtra o que o paciente não pode ver). Série sem nenhum ponto não entra.
 * Peso: mínimo e máximo saudáveis do servidor e o peso atual. Composição: peso gordo × peso magro.
 */
fun graficosDaAvaliacao(avaliacao: AvaliacaoResposta): List<GraficoAvaliacao> {
    val r = avaliacao.resultados
    val peso = listOfNotNull(
        r?.pesoTeoricoMinKg?.let { PontoGraficoAvaliacao(ChaveGrafico.Peso.MINIMO, it) },
        PontoGraficoAvaliacao(ChaveGrafico.Peso.ATUAL, avaliacao.pesoKg),
        r?.pesoTeoricoMaxKg?.let { PontoGraficoAvaliacao(ChaveGrafico.Peso.MAXIMO, it) },
    )
    fun medidas(secao: SecaoAvaliacao, vararg incluir: MedidaAvaliacao) = MedidaAvaliacao.entries
        .filter { it.secao == secao && (incluir.isEmpty() || it in incluir) }
        .mapNotNull { m -> m.ler(avaliacao)?.let { PontoGraficoAvaliacao(ChaveGrafico.Medida(m), it) } }
    return listOf(
        GraficoAvaliacao(SerieGraficoAvaliacao.PESO, peso),
        GraficoAvaliacao(SerieGraficoAvaliacao.PERIMETROS, medidas(SecaoAvaliacao.PERIMETROS)),
        GraficoAvaliacao(SerieGraficoAvaliacao.DOBRAS, medidas(SecaoAvaliacao.DOBRAS)),
        GraficoAvaliacao(SerieGraficoAvaliacao.COMPOSICAO, medidas(SecaoAvaliacao.COMPOSICAO, MedidaAvaliacao.PESO_GORDO, MedidaAvaliacao.PESO_MAGRO)),
    ).filter { it.pontos.isNotEmpty() && (it.serie != SerieGraficoAvaliacao.PESO || it.pontos.size > 1) }
}

/** Sigla curta sob cada barra (ART-P1-09 e ART-P1-10). Medida sem sigla própria usa as 2 primeiras letras do nome em maiúsculas. */
fun siglaMedida(medida: MedidaAvaliacao): String = when (medida) {
    MedidaAvaliacao.PESCOCO -> "PE"
    MedidaAvaliacao.TORAX -> "TO"
    MedidaAvaliacao.CINTURA -> "C"
    MedidaAvaliacao.ABDOMEN -> "A"
    MedidaAvaliacao.QUADRIL -> "Q"
    MedidaAvaliacao.BRACO -> "BR"
    MedidaAvaliacao.ANTEBRACO -> "AB"
    MedidaAvaliacao.COXA -> "CX"
    MedidaAvaliacao.PANTURRILHA -> "PA"
    MedidaAvaliacao.DOBRA_TRICEPS -> "TR"
    MedidaAvaliacao.DOBRA_SUBESCAPULAR -> "SE"
    MedidaAvaliacao.DOBRA_PEITORAL -> "PE"
    MedidaAvaliacao.DOBRA_AXILAR_MEDIA -> "AM"
    MedidaAvaliacao.DOBRA_SUPRAILIACA -> "SI"
    MedidaAvaliacao.DOBRA_ABDOMINAL -> "AB"
    MedidaAvaliacao.DOBRA_COXA -> "CX"
    else -> medida.name.take(2)
}
