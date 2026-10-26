package org.br.dietamais.dominio.convite

import kotlin.test.*

class CodigoConviteTest {
    @Test fun normalizaCodigoDigitadoSemAceitarCaracteresAmbiguos() {
        assertEquals("ABCD2345", extrairCodigoConvite(" abcd2345 "))
        listOf("ABCD1234", "ABCDO234", "ABCDI234", "ABCDEFG", "ABCD 2345").forEach { assertNull(extrairCodigoConvite(it)) }
    }
    @Test fun aceitaEsquemaOficialEAlternativaSingular() {
        assertEquals("ABCD2345", extrairCodigoConvite("dietamais://convites/ABCD2345"))
        assertEquals("ABCD2345", extrairCodigoConvite("dietamais://convite/ABCD2345"))
    }
    @Test fun httpsExigeDominioConfiguradoEPathDeConvite() {
        val dominios = setOf("convites.example.invalid")
        assertEquals("ABCD2345", extrairCodigoConvite("https://convites.example.invalid/convite/ABCD2345", dominios))
        assertNull(extrairCodigoConvite("https://convites.example.invalid/convite/ABCD2345"))
        listOf("https://falso.example.invalid/convite/ABCD2345", "https://convites.example.invalid.evil/convite/ABCD2345",
            "https://user@convites.example.invalid/convite/ABCD2345", "https://convites.example.invalid:444/convite/ABCD2345",
            "https://convites.example.invalid/convite/ABCD2345?codigo=ZZZZ2345", "https://convites.example.invalid/convite/ABCD2345#f",
            "https://convites.example.invalid/convite/%41BCD2345", "http://convites.example.invalid/convite/ABCD2345",
            "javascript:ABCD2345", "dietamais://outro/ABCD2345", "dietamais://convites/ABCD2345/extra").forEach {
            assertNull(extrairCodigoConvite(it, dominios), "Link fora do escopo deve ser recusado")
        }
    }
    @Test fun entradaExcessivaNaoEProcessada() { assertNull(extrairCodigoConvite("A".repeat(2049))) }
}
