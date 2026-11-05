package org.br.dietamais.dominio.pacientes

import kotlin.test.*
import kotlin.uuid.Uuid

class FixadosTest {
    private val a = Uuid.random(); private val b = Uuid.random(); private val c = Uuid.random()

    @Test fun fixarEDesafixar() {
        val f = FixadosRecentes().alternarFixado(a).alternarFixado(b)
        assertEquals(listOf(b, a), f.fixados)
        assertEquals(listOf(b), f.alternarFixado(a).fixados)
    }
    @Test fun recentesFicamEmOrdemSemRepetirELimitados() {
        var f = FixadosRecentes()
        repeat(7) { f = f.registrarAbertura(Uuid.random()) }
        assertEquals(FixadosRecentes.LIMITE_RECENTES, f.recentes.size)
        val g = FixadosRecentes().registrarAbertura(a).registrarAbertura(b).registrarAbertura(a)
        assertEquals(listOf(a, b), g.recentes)
    }
    @Test fun atalhosIgnoramQuemSumiuEOFixadoNaoRepeteNosRecentes() {
        val g = FixadosRecentes(fixados = listOf(a, Uuid.random()), recentes = listOf(a, b, c))
        val r = montarAtalhos(listOf(a, b), g) { it }
        assertEquals(listOf(a), r.fixados); assertEquals(listOf(b), r.recentes)
    }
}
