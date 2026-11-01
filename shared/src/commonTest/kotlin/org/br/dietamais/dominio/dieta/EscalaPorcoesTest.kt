package org.br.dietamais.dominio.dieta

import kotlin.test.*

/** Mesmos vetores do `EscalaPorcoesTest` do backend (RN-MOD-02). */
class EscalaPorcoesTest {
    @Test fun fatorEhAlvoSobreBase() {
        assertEquals(1.25, EscalaPorcoes.fator(2000.0, 1600.0)); assertEquals(1.0, EscalaPorcoes.fator(1800.0, 1800.0))
    }
    @Test fun fatorInviavelOuSemBaseEhNulo() {
        assertNull(EscalaPorcoes.fator(200.0, 2000.0)); assertNull(EscalaPorcoes.fator(9000.0, 1500.0))
        assertNull(EscalaPorcoes.fator(1800.0, null)); assertNull(EscalaPorcoes.fator(1800.0, 0.0)); assertNull(EscalaPorcoes.fator(null, 1800.0))
    }
    @Test fun limitesDaFaixaValem() {
        assertEquals(0.25, EscalaPorcoes.fator(500.0, 2000.0)); assertEquals(4.0, EscalaPorcoes.fator(8000.0, 2000.0))
    }
    @Test fun arredondaParaMeiaPorcaoComMinimoDeMeia() {
        assertEquals(2.5, EscalaPorcoes.escalar(2.0, 1.25)); assertEquals(3.5, EscalaPorcoes.escalar(3.0, 1.1)); assertEquals(1.0, EscalaPorcoes.escalar(1.0, 0.9))
        assertEquals(0.5, EscalaPorcoes.escalar(1.0, 0.3)); assertEquals(0.5, EscalaPorcoes.escalar(0.5, 0.25))
    }
}
