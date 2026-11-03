package org.br.dietamais.plataforma

import kotlin.test.Test
import kotlin.test.assertEquals

class ConfigAndroidTest {
    @Test
    fun usaEnderecoDoHostNoEmuladorAndroid() {
        assertEquals("http://10.0.2.2:8080", Config().URL_API)
    }
}
