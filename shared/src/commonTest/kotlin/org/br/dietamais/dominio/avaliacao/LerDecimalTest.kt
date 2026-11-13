package org.br.dietamais.dominio.avaliacao

import kotlin.test.*

class LerDecimalTest {
    @Test fun virgulaEPontoSaoSeparadorDecimal() {
        assertEquals(72.5, lerDecimal("72,5")); assertEquals(72.5, lerDecimal("72.5")); assertEquals(72.5, lerDecimal(" 72,5 "))
    }

    @Test fun vazioOuInvalidoNaoViraNumero() {
        assertNull(lerDecimal("")); assertNull(lerDecimal("abc")); assertNull(lerDecimal("1,2,3"))
    }
}
