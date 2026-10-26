package org.br.dietamais.dominio.convite

import org.br.dietamais.plataforma.LinksConvite
import org.br.dietamais.plataforma.receberLinkConvite
import kotlin.test.*

class LinksConviteTest {
    @Test fun entradaFriaFicaPendenteEConsumoAntigoNaoApagaLinkMaisRecente() {
        receberLinkConvite("dietamais://convites/ABCD2345")
        val primeira = assertNotNull(LinksConvite.pendente.value)
        receberLinkConvite("dietamais://convite/ZZZZ2345")
        val segunda = assertNotNull(LinksConvite.pendente.value)
        LinksConvite.consumir(primeira.id)
        assertEquals(segunda, LinksConvite.pendente.value)
        LinksConvite.consumir(segunda.id)
        assertNull(LinksConvite.pendente.value)
        assertFalse(primeira.toString().contains("ABCD2345"))
    }
}
