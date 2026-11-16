package org.br.dietamais.dominio.dieta

import kotlin.math.floor
import kotlin.math.round

/**
 * RN-MOD-02, mesma conta do backend (`EscalaPorcoes`): o fator é alvo ÷ base com 4 casas e só vale entre 0,25 e 4; cada porção é multiplicada
 * pelo fator, arredondada para 0,5 (meio para cima) e nunca fica abaixo de 0,5. É só pré-visualização: quem cria o rascunho é o servidor.
 */
object EscalaPorcoes {
    const val MIN = 0.25
    const val MAX = 4.0

    fun fator(alvoKcal: Double?, baseKcal: Double?): Double? {
        if (alvoKcal == null || baseKcal == null || alvoKcal <= 0.0 || baseKcal <= 0.0) return null
        val f = round(alvoKcal / baseKcal * 10000 + 1e-9) / 10000
        return if (f < MIN || f > MAX) null else f
    }

    fun escalar(porcoes: Double, fator: Double): Double = maxOf(0.5, floor(porcoes * fator / 0.5 + 0.5 + 1e-9) * 0.5)
}
