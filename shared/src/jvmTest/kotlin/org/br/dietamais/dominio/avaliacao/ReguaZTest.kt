package org.br.dietamais.dominio.avaliacao

import kotlin.test.*

class ReguaZTest {
    @Test fun posicaoVaiDeZeroAUm() {
        assertEquals(0f, ReguaZ.posicao(-3.0)); assertEquals(0.5f, ReguaZ.posicao(0.0)); assertEquals(1f, ReguaZ.posicao(3.0))
    }
    @Test fun foraDaReguaFicaNaPonta() {
        assertEquals(0f, ReguaZ.posicao(-5.2)); assertEquals(1f, ReguaZ.posicao(4.0))
    }
    @Test fun intervaloUsaOPontoMedioOuAPontaExistente() {
        assertEquals(0.5, ReguaZ.valor(0.0, 1.0)); assertEquals(-2.0, ReguaZ.valor(-2.0, null)); assertEquals(1.5, ReguaZ.valor(null, 1.5)); assertNull(ReguaZ.valor(null, null))
    }
}
