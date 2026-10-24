package org.br.dietamais.plataforma

data class Config(val URL_API: String = urlApiPadrao())

internal expect fun urlApiPadrao(): String
