package org.br.dietamais.plataforma

data class Config(val URL_API: String = urlApiPadrao(), val DOMINIOS_CONVITE: Set<String> = emptySet())

internal expect fun urlApiPadrao(): String
