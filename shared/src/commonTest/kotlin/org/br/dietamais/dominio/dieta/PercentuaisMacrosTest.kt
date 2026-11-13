package org.br.dietamais.dominio.dieta

import kotlin.test.*

class PercentuaisMacrosTest {
    @Test fun calculaAParteDeCadaMacroNasCalorias() {
        // 260 g de carboidrato (1040 kcal), 110 g de proteína (440) e 59 g de gordura (531): 2011 kcal
        assertEquals(PercentuaisMacros(52, 22, 26), percentuaisMacros(260.0, 110.0, 59.0))
    }

    @Test fun semMacroNaoHaPercentual() = assertNull(percentuaisMacros(0.0, 0.0, 0.0))

    @Test fun kcalComPontoDeMilhar() {
        assertEquals("1.850", kcalInteiro(1850.0)); assertEquals("2.000", kcalInteiro(1999.6)); assertEquals("980", kcalInteiro(980.2))
        assertEquals("0", kcalInteiro(0.0)); assertEquals("12.345", kcalInteiro(12345.0)); assertEquals("1.000.000", kcalInteiro(1_000_000.0))
    }
}
