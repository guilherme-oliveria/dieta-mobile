package org.br.dietamais.dominio.graficos

import kotlin.math.max
import kotlin.math.min

/** Fatia de rosca já em graus: começa no topo (−90°) e segue no sentido horário. */
data class FatiaRosca(val rotulo: String, val valor: Double, val percentual: Double, val inicioGraus: Float, val varreduraGraus: Float, val indice: Int)

/** Valores zero, negativos ou não finitos não viram fatia; total zero devolve lista vazia (o gráfico mostra "sem dados"). */
fun fatiasRosca(itens: List<Pair<String, Double>>): List<FatiaRosca> {
    val validos = itens.withIndex().filter { it.value.second.isFinite() && it.value.second > 0.0 }
    val total = validos.sumOf { it.value.second }
    if (total <= 0.0) return emptyList()
    var inicio = -90f
    return validos.map { (indice, par) ->
        val varredura = (par.second / total * 360.0).toFloat()
        FatiaRosca(par.first, par.second, par.second / total * 100.0, inicio, varredura, indice).also { inicio += varredura }
    }
}

/** Posição de [valor] entre [minimo] e [maximo] em 0..1 (fora da faixa fica na ponta). */
fun fracaoNaFaixa(valor: Double, minimo: Double, maximo: Double): Float {
    if (!valor.isFinite() || maximo <= minimo) return 0f
    return ((valor - minimo) / (maximo - minimo)).coerceIn(0.0, 1.0).toFloat()
}

/** Trecho de uma régua de faixas (IMC, exames): vale de [de] (inclusive) até [ate] (exclusive, exceto no último trecho). */
data class SegmentoFaixa(val rotulo: String, val de: Double, val ate: Double)

fun segmentoDoValor(valor: Double, segmentos: List<SegmentoFaixa>): SegmentoFaixa? {
    if (!valor.isFinite() || segmentos.isEmpty()) return null
    segmentos.forEachIndexed { i, s ->
        val ultimo = i == segmentos.lastIndex
        if (valor >= s.de && (valor < s.ate || (ultimo && valor <= s.ate))) return s
    }
    return null
}

/** Posição do marcador na régua inteira (0..1), proporcional à largura de cada trecho; valor fora fica na ponta. */
fun posicaoNaRegua(valor: Double, segmentos: List<SegmentoFaixa>): Float =
    if (segmentos.isEmpty()) 0f else fracaoNaFaixa(valor, segmentos.first().de, segmentos.last().ate)

/** Largura relativa de cada trecho (soma 1), para desenhar a régua. */
fun largurasRegua(segmentos: List<SegmentoFaixa>): List<Float> {
    val total = segmentos.sumOf { it.ate - it.de }
    return if (total <= 0.0) segmentos.map { 1f / max(1, segmentos.size) } else segmentos.map { ((it.ate - it.de) / total).toFloat() }
}

/** Limites do eixo Y com folga de 10%; um valor único ganha ±1 para a linha não ficar achatada. */
fun limitesEixo(valores: List<Double>): Pair<Double, Double> {
    val v = valores.filter { it.isFinite() }
    if (v.isEmpty()) return 0.0 to 1.0
    val lo = v.min(); val hi = v.max()
    if (hi == lo) return (lo - 1) to (hi + 1)
    val folga = (hi - lo) * 0.1
    return (lo - folga) to (hi + folga)
}

/** Ponto da curva de crescimento: idade em meses e valor. */
data class PontoCurva(val mes: Int, val valor: Double)

/** Valor da curva em [mes] por interpolação linear entre os dois pontos vizinhos (fora do alcance, nulo). */
fun valorNaCurva(curva: List<PontoCurva>, mes: Int): Double? {
    if (curva.isEmpty()) return null
    val ordenada = curva.sortedBy { it.mes }
    if (mes < ordenada.first().mes || mes > ordenada.last().mes) return null
    val depois = ordenada.firstOrNull { it.mes >= mes } ?: return null
    if (depois.mes == mes) return depois.valor
    val antes = ordenada.last { it.mes < mes }
    val t = (mes - antes.mes).toDouble() / (depois.mes - antes.mes)
    return antes.valor + (depois.valor - antes.valor) * t
}

/** Diz entre quais curvas de percentil o ponto está: índice da maior curva que fica no ou abaixo do valor, ou −1 se abaixo de todas. */
fun curvaAbaixo(valor: Double, mes: Int, curvas: List<List<PontoCurva>>): Int {
    var achada = -1
    curvas.forEachIndexed { i, c -> val v = valorNaCurva(c, mes); if (v != null && valor >= v) achada = max(achada, i) }
    return min(achada, curvas.lastIndex)
}
