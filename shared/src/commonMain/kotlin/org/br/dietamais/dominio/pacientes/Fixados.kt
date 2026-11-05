package org.br.dietamais.dominio.pacientes

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

/** MOB-67c: pacientes fixados e abertos há pouco, guardados só no aparelho (o backend não tem isso). Identificados pelo `vinculoId`. */
@Serializable
data class FixadosRecentes(val fixados: List<Uuid> = emptyList(), val recentes: List<Uuid> = emptyList()) {
    fun alternarFixado(vinculo: Uuid) = copy(fixados = if (vinculo in fixados) fixados - vinculo else (listOf(vinculo) + fixados).take(LIMITE_FIXADOS))
    /** O mais recente primeiro, sem repetir, até [LIMITE_RECENTES]. */
    fun registrarAbertura(vinculo: Uuid) = copy(recentes = (listOf(vinculo) + recentes.filter { it != vinculo }).take(LIMITE_RECENTES))
    companion object { const val LIMITE_FIXADOS = 20; const val LIMITE_RECENTES = 5 }
}

/** Fixados e recentes que ainda existem na lista, na ordem guardada; recente já fixado não repete. */
data class AtalhosPacientes<T>(val fixados: List<T>, val recentes: List<T>)

fun <T> montarAtalhos(todos: List<T>, guardados: FixadosRecentes, vinculo: (T) -> Uuid): AtalhosPacientes<T> {
    val porId = todos.associateBy(vinculo)
    val fixados = guardados.fixados.mapNotNull { porId[it] }
    val recentes = guardados.recentes.filter { it !in guardados.fixados }.mapNotNull { porId[it] }
    return AtalhosPacientes(fixados, recentes)
}
