package org.br.dietamais.plataforma

import kotlin.uuid.Uuid

/** UUID da instalação no `localStorage`; não é dado de saúde. */
actual class IdentidadeDispositivo {
    actual fun obter(): Uuid {
        val salvo = lerArmazenado(CHAVE)
        return salvo?.let { runCatching { Uuid.parse(it) }.getOrNull() } ?: Uuid.random().also { gravarArmazenado(CHAVE, it.toString()) }
    }
    private companion object { const val CHAVE = "dietamais.instalacao" }
}

private fun lerArmazenado(chave: String): String? = js("localStorage.getItem(chave)")
private fun gravarArmazenado(chave: String, valor: String): Unit = js("localStorage.setItem(chave, valor)")
