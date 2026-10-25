package org.br.dietamais.plataforma

import platform.Foundation.NSUserDefaults
import kotlin.uuid.Uuid

actual class IdentidadeDispositivo {
    private val preferencias = NSUserDefaults.standardUserDefaults
    actual fun obter(): Uuid = preferencias.stringForKey(CHAVE)?.let(Uuid::parse) ?: Uuid.random().also {
        preferencias.setObject(it.toString(), CHAVE)
    }
    private companion object { const val CHAVE = "org.br.dietamais.instalacao.id" }
}
