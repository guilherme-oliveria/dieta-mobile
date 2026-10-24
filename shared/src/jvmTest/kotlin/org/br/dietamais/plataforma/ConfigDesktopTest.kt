package org.br.dietamais.plataforma

import kotlin.test.Test
import kotlin.test.assertEquals

class ConfigDesktopTest {
    @Test
    fun usaServidorLocalNoDesktop() {
        assertEquals("http://localhost:8080", Config().URL_API)
    }
}
