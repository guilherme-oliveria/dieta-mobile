package org.br.dietamais.plataforma

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class FusoDoAparelhoTest {
    @Test
    fun devolveIdentificadorIanaQueOBackendAceita() {
        val id = idFusoDoAparelho()
        assertNotEquals("SYSTEM", id)
        assertTrue(id == "UTC" || id.contains('/'), "esperava um id IANA (Regiao/Cidade), veio '$id'")
        TimeZone.of(id)
    }
}
