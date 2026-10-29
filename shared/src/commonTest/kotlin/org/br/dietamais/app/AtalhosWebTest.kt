package org.br.dietamais.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AtalhosWebTest {
    @Test fun numerosSeguemAOrdemDasAbas() {
        assertEquals(RotaNutri.Inicio, rotaDoAtalho(PerfilNavegacao.NUTRICIONISTA, 1))
        assertEquals(RotaNutri.Conta, rotaDoAtalho(PerfilNavegacao.NUTRICIONISTA, 5))
        assertEquals(RotaPaciente.Hoje, rotaDoAtalho(PerfilNavegacao.PACIENTE, 1))
        assertEquals(RotaPaciente.Evolucao, rotaDoAtalho(PerfilNavegacao.PACIENTE, 4))
    }

    @Test fun numeroForaDasAbasNaoFazNada() {
        assertNull(rotaDoAtalho(PerfilNavegacao.NUTRICIONISTA, 0))
        assertNull(rotaDoAtalho(PerfilNavegacao.PACIENTE, 6))
    }
}
