package org.br.dietamais.dominio.rotina

import kotlin.test.*
import kotlin.time.Instant

class AguaTest {
    @Test fun valorLivreAceitaDe10A2000() {
        assertEquals(250, lerMlAgua("250")); assertEquals(10, lerMlAgua(" 10 ")); assertEquals(2000, lerMlAgua("2000"))
        assertNull(lerMlAgua("9")); assertNull(lerMlAgua("2001")); assertNull(lerMlAgua("")); assertNull(lerMlAgua("abc")); assertNull(lerMlAgua("-50"))
    }

    @Test fun totalSomaOGravadoEOsPendentes() {
        val t = Instant.parse("2026-11-12T10:00:00Z")
        assertEquals(1000, totalAguaMl(500, listOf(AguaPendente(300, t), AguaPendente(200, t))))
        assertEquals(500, totalAguaMl(500, emptyList()))
    }
}
