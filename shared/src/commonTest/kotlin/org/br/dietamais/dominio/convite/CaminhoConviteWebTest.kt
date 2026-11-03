package org.br.dietamais.dominio.convite

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CaminhoConviteWebTest {
    @Test fun leOCodigoDoCaminhoDoConvite() {
        assertEquals("ABCD2345", codigoDoCaminhoWeb("/convite/ABCD2345"))
        assertEquals("ABCD2345", codigoDoCaminhoWeb("/convites/abcd2345/"))
    }

    @Test fun ignoraOutrosCaminhos() {
        assertNull(codigoDoCaminhoWeb("/"))
        assertNull(codigoDoCaminhoWeb("/pacientes/ABCD2345"))
        assertNull(codigoDoCaminhoWeb("/convite/ABCD234"))
        assertNull(codigoDoCaminhoWeb("/convite/ABCD2345/extra"))
        assertNull(codigoDoCaminhoWeb("/convite/ABCD1O45"))
    }
}
