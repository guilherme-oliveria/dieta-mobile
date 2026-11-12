package org.br.dietamais.dominio.rotina

import kotlinx.datetime.LocalDate
import kotlin.test.*

class DiaHojeTest {
    private val hoje = LocalDate(2026, 11, 10)

    @Test fun nomeiaHojeOntemEOsDemais() {
        assertEquals(TipoDia.HOJE, tipoDia(hoje, hoje))
        assertEquals(TipoDia.ONTEM, tipoDia(LocalDate(2026, 11, 9), hoje))
        assertEquals(TipoDia.OUTRO, tipoDia(LocalDate(2026, 11, 8), hoje))
        assertEquals(TipoDia.OUTRO, tipoDia(LocalDate(2026, 11, 11), hoje))
        assertEquals(TipoDia.OUTRO, tipoDia(hoje, null))
    }

    @Test fun ontemAtravessaOMes() = assertEquals(TipoDia.ONTEM, tipoDia(LocalDate(2026, 10, 31), LocalDate(2026, 11, 1)))

    @Test fun diaMesTemDoisDigitos() { assertEquals("09/11", diaMes(LocalDate(2026, 11, 9))); assertEquals("01/02", diaMes(LocalDate(2026, 2, 1))) }
}
