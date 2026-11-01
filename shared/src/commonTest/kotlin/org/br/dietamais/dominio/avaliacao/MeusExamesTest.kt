package org.br.dietamais.dominio.avaliacao

import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.dominio.graficos.segmentoDoValor
import kotlin.test.*

class MeusExamesTest {
    private fun f(rotulo: String, min: Double?, max: Double?, minIncl: Boolean = false, maxIncl: Boolean = false) = FaixaExameDto(rotulo, min, minIncl, max, maxIncl)

    @Test fun classeDoRotuloReconheceAsDaPlanilhaEAceitaRotuloNovo() {
        assertEquals(ClasseFaixa.NORMAL, classeDoRotulo("normal")); assertEquals(ClasseFaixa.ABAIXO, classeDoRotulo("baixo")); assertEquals(ClasseFaixa.ABAIXO, classeDoRotulo("limite inferior"))
        assertEquals(ClasseFaixa.ATENCAO, classeDoRotulo("limite superior")); assertEquals(ClasseFaixa.ATENCAO, classeDoRotulo("int. à glicose")); assertEquals(ClasseFaixa.ATENCAO, classeDoRotulo("limítrofe"))
        assertEquals(ClasseFaixa.ALTO, classeDoRotulo("alto")); assertEquals(ClasseFaixa.ALTO, classeDoRotulo("muito alto")); assertEquals(ClasseFaixa.ALTO, classeDoRotulo("diabetes"))
        assertEquals(ClasseFaixa.NORMAL, classeDoRotulo("Ótimo")); assertEquals(ClasseFaixa.OUTRA, classeDoRotulo("rótulo inventado"))
    }
    @Test fun reguaDaGlicemiaIgnoraOPontoEFechaAsPontasAbertas() {
        val glicemia = listOf(f("baixo", null, 70.0), f("limite inferior", 70.0, 70.0, true, true), f("normal", 70.0, 100.0), f("int. à glicose", 100.0, 125.0, true, true), f("diabetes", 125.0, null))
        val s = segmentosDoExame(glicemia)!!
        assertEquals(listOf("baixo", "normal", "int. à glicose", "diabetes"), s.map { it.rotulo })
        assertTrue(s.zipWithNext().all { (a, b) -> a.ate == b.de || b.rotulo == "diabetes" }); assertTrue(s.all { it.ate > it.de })
        assertEquals("normal", segmentoDoValor(92.0, s)?.rotulo); assertEquals("diabetes", segmentoDoValor(130.0, s)?.rotulo); assertEquals("baixo", segmentoDoValor(50.0, s)?.rotulo)
    }
    @Test fun semFaixaNumericaNaoHaRegua() { assertNull(segmentosDoExame(emptyList())); assertNull(segmentosDoExame(listOf(f("ponto", 5.0, 5.0)))) }
    @Test fun rotuloLegivelCapitaliza() = assertEquals("Limite superior", rotuloLegivel(" limite superior"))
}
