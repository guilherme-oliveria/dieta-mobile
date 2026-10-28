package org.br.dietamais.dominio.dieta

import kotlin.uuid.Uuid

/**
 * Diferença da lista de substituição de um paciente em relação à lista base da nutri (RN-SUB).
 * Remover algo que a nutri tinha adicionado só desfaz a adição; adicionar algo removido só desfaz a remoção.
 */
data class AjusteLista(val adicionados: Set<Uuid> = emptySet(), val removidos: Set<Uuid> = emptySet()) {
    fun remover(id: Uuid): AjusteLista = if (id in adicionados) copy(adicionados = adicionados - id) else copy(removidos = removidos + id)
    fun adicionar(id: Uuid): AjusteLista = if (id in removidos) copy(removidos = removidos - id) else copy(adicionados = adicionados + id)
    val vazio: Boolean get() = adicionados.isEmpty() && removidos.isEmpty()
}
