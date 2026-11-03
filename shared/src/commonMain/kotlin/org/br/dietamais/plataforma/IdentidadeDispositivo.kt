package org.br.dietamais.plataforma

import kotlin.uuid.Uuid

/** Identificador não secreto da instalação; não é removido no logout. */
expect class IdentidadeDispositivo {
    fun obter(): Uuid
}
