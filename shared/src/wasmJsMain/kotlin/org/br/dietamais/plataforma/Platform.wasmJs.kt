package org.br.dietamais.plataforma

class WebPlatform : Platform {
    override val name: String = "Navegador"
}

actual fun getPlatform(): Platform = WebPlatform()
