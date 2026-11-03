package org.br.dietamais.dominio.dieta

import kotlin.math.roundToLong

/** Macros por porção de um grupo (4 casas, como no backend). */
data class MacrosPorcao(val kcal: Double, val carboidratos: Double, val proteinas: Double, val gorduras: Double, val gramas: Double = 0.0)

/** Totais em centésimos (kcal 2509,40 = 250940), para somar sem erro de ponto flutuante. */
data class TotaisCent(val kcal: Long = 0, val carboidratos: Long = 0, val proteinas: Long = 0, val gorduras: Long = 0) {
    operator fun plus(o: TotaisCent) = TotaisCent(kcal + o.kcal, carboidratos + o.carboidratos, proteinas + o.proteinas, gorduras + o.gorduras)
    val kcalReais get() = kcal / 100.0
    val carboidratosReais get() = carboidratos / 100.0
    val proteinasReais get() = proteinas / 100.0
    val gordurasReais get() = gorduras / 100.0
}

/**
 * RN-DIE-05, mesma fórmula do backend (`CalculadoraTotais`): cada item multiplica porções × macros do grupo e arredonda a 2 casas
 * (meio para cima); as somas usam os itens já arredondados. Aritmética inteira em 4 casas para não depender de Double.
 */
object CalculadoraTotais {
    private const val ESCALA = 10_000L

    fun item(macros: MacrosPorcao, porcoes: Double): TotaisCent {
        val p = (porcoes * ESCALA).roundToLong()
        return TotaisCent(centesimos(macros.kcal, p), centesimos(macros.carboidratos, p), centesimos(macros.proteinas, p), centesimos(macros.gorduras, p))
    }
    fun gramas(macros: MacrosPorcao, porcoes: Double): Long = centesimos(macros.gramas, (porcoes * ESCALA).roundToLong())
    fun somar(partes: Iterable<TotaisCent>): TotaisCent = partes.fold(TotaisCent()) { a, b -> a + b }

    private fun centesimos(macro: Double, porcoesEscala: Long): Long {
        val m = (macro * ESCALA).roundToLong()
        val produto = m * porcoesEscala // escala 1e8
        return if (produto >= 0) (produto + 500_000L) / 1_000_000L else -((-produto + 500_000L) / 1_000_000L)
    }
}
