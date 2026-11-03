package org.br.dietamais.plataforma

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
