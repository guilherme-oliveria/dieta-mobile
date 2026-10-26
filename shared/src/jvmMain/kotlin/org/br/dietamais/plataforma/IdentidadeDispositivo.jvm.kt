package org.br.dietamais.plataforma

import java.util.prefs.Preferences
import kotlin.uuid.Uuid

actual class IdentidadeDispositivo {
    private val preferencias = Preferences.userRoot().node("org/br/dietamais/instalacao")
    actual fun obter(): Uuid = synchronized(preferencias) {
        preferencias.get("id", null)?.let(Uuid::parse) ?: Uuid.random().also {
            preferencias.put("id", it.toString()); preferencias.flush()
        }
    }
}
