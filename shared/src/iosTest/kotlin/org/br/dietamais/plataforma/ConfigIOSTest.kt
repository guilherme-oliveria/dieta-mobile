package org.br.dietamais.plataforma

import kotlin.test.Test
import kotlin.test.assertEquals

class ConfigIOSTest {
    @Test
    fun usaServidorLocalNoSimuladorIOS() {
        assertEquals("http://localhost:8080", Config().URL_API)
    }
}
