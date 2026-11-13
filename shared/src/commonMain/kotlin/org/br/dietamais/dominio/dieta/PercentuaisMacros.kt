package org.br.dietamais.dominio.dieta

import kotlin.math.round

/** Parte de cada macro nas calorias do dia, em % inteiros (carboidrato e proteína 4 kcal/g, gordura 9 kcal/g). */
data class PercentuaisMacros(val carboidratos: Int, val proteinas: Int, val lipidios: Int)

/** `null` quando ainda não há macro (dieta vazia): a faixa de totais mostra só as kcal. */
fun percentuaisMacros(carboidratosG: Double, proteinasG: Double, gordurasG: Double): PercentuaisMacros? {
    val c = carboidratosG * 4; val p = proteinasG * 4; val l = gordurasG * 9
    val total = c + p + l
    if (total <= 0.0) return null
    fun pct(v: Double) = round(v / total * 100).toInt()
    return PercentuaisMacros(pct(c), pct(p), pct(l))
}

/** kcal em número inteiro com ponto de milhar (pt-BR): 1850 vira "1.850". */
fun kcalInteiro(kcal: Double): String {
    val n = round(kcal).toLong()
    val digitos = kotlin.math.abs(n).toString()
    val agrupado = digitos.reversed().chunked(3).joinToString(".").reversed()
    return if (n < 0) "-$agrupado" else agrupado
}
