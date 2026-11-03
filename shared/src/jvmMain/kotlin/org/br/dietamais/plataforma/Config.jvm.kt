package org.br.dietamais.plataforma

internal actual fun urlApiPadrao(): String = System.getenv("DIETA_URL_API")?.takeIf { it.isNotBlank() } ?: System.getProperty("dieta.url.api") ?: "http://localhost:8080"
