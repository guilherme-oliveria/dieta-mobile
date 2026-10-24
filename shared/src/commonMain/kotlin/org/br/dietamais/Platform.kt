package org.br.dietamais

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform