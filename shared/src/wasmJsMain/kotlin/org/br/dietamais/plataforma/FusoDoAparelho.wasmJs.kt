package org.br.dietamais.plataforma

/** Obtém o identificador IANA do navegador pelo `Intl`. */
actual fun idFusoDoAparelho(): String = fusoDoNavegador()?.takeIf { it.isNotBlank() } ?: "UTC"

private fun fusoDoNavegador(): String? = js("Intl.DateTimeFormat().resolvedOptions().timeZone")
