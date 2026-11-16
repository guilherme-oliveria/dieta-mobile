package org.br.dietamais.dominio.avaliacao

/** Régua do escore Z da avaliação infantil (ART-A-Avaliacao-Infantil): vai de −3 a +3 e o marcador nunca sai das pontas. */
object ReguaZ {
    const val MINIMO = -3.0
    const val MAXIMO = 3.0

    /** Posição do marcador, de 0 (−3) a 1 (+3). */
    fun posicao(z: Double): Float = ((z.coerceIn(MINIMO, MAXIMO) - MINIMO) / (MAXIMO - MINIMO)).toFloat()

    /** Valor a marcar quando o backend devolve um intervalo: o ponto médio, ou a ponta que existir. */
    fun valor(inferior: Double?, superior: Double?): Double? = when {
        inferior != null && superior != null -> (inferior + superior) / 2
        else -> inferior ?: superior
    }
}
