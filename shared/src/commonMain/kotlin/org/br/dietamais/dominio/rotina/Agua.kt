package org.br.dietamais.dominio.rotina

import kotlin.time.Instant

const val AGUA_MIN_ML = 10
const val AGUA_MAX_ML = 2000

/** Valor livre de água em ml (MOB-UX-05): só número inteiro de [AGUA_MIN_ML] a [AGUA_MAX_ML]; o backend continua decidindo o limite. */
fun lerMlAgua(texto: String): Int? = texto.trim().toIntOrNull()?.takeIf { it in AGUA_MIN_ML..AGUA_MAX_ML }

/** Lançamento de água que ainda espera o prazo de desfazer; entra no total do dia como se já estivesse gravado. */
class AguaPendente(val ml: Int, val registradoEm: Instant)

/** Total do dia: o já gravado mais os lançamentos pendentes. */
fun totalAguaMl(gravadoMl: Int, pendentes: List<AguaPendente>): Int = gravadoMl + pendentes.sumOf { it.ml }
