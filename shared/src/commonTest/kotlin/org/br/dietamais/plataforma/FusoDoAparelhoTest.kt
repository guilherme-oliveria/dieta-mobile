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
        // Aparelho real devolve Regiao/Cidade; o simulador do CI roda em GMT/UTC, que o backend também aceita.
        assertTrue(id == "UTC" || id == "GMT" || id.contains('/'), "esperava um id IANA (Regiao/Cidade), veio '$id'")
        TimeZone.of(id)
    }
}
