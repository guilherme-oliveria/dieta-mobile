package org.br.dietamais.plataforma

import android.content.Context
import kotlin.uuid.Uuid

actual class IdentidadeDispositivo(contexto: Context) {
    private val preferencias = contexto.applicationContext.getSharedPreferences("instalacao", Context.MODE_PRIVATE)
    actual fun obter(): Uuid = synchronized(preferencias) {
        preferencias.getString("id", null)?.let(Uuid::parse) ?: Uuid.random().also {
            check(preferencias.edit().putString("id", it.toString()).commit())
        }
    }
}
