package org.br.dietamais.plataforma

/** No navegador o kotlinx-datetime devolve `SYSTEM` como id; o nome IANA vem do `Intl`. */
actual fun idFusoDoAparelho(): String = fusoDoNavegador()?.takeIf { it.isNotBlank() } ?: "UTC"

private fun fusoDoNavegador(): String? = js("Intl.DateTimeFormat().resolvedOptions().timeZone")
