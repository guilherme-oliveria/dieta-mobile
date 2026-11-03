package org.br.dietamais.dominio.graficos

import kotlin.test.*

class GeometriaGraficosTest {
    @Test fun roscaDistribuiGrausProporcionaisComecandoNoTopo() {
        val f = fatiasRosca(listOf("Carboidrato" to 50.0, "Proteína" to 20.0, "Gordura" to 30.0))
        assertEquals(3, f.size)
        assertEquals(-90f, f[0].inicioGraus); assertEquals(180f, f[0].varreduraGraus); assertEquals(50.0, f[0].percentual)
        assertEquals(90f, f[1].inicioGraus); assertEquals(72f, f[1].varreduraGraus)
        assertEquals(360f, f.sumOf { it.varreduraGraus.toDouble() }.toFloat(), 0.01f)
    }
    @Test fun roscaIgnoraZeroNegativoENaoFinitoMantendoIndiceDeCor() {
        val f = fatiasRosca(listOf("a" to 0.0, "b" to 10.0, "c" to -3.0, "d" to Double.NaN, "e" to 10.0))
        assertEquals(listOf(1, 4), f.map { it.indice }); assertEquals(180f, f[0].varreduraGraus)
        assertTrue(fatiasRosca(listOf("a" to 0.0)).isEmpty()); assertTrue(fatiasRosca(emptyList()).isEmpty())
    }
    @Test fun fracaoNaFaixaFicaNasPontas() {
        assertEquals(0.5f, fracaoNaFaixa(15.0, 10.0, 20.0)); assertEquals(0f, fracaoNaFaixa(5.0, 10.0, 20.0)); assertEquals(1f, fracaoNaFaixa(99.0, 10.0, 20.0))
        assertEquals(0f, fracaoNaFaixa(1.0, 5.0, 5.0)); assertEquals(0f, fracaoNaFaixa(Double.NaN, 0.0, 1.0))
    }
    private val imc = listOf(SegmentoFaixa("Baixo", 10.0, 18.5), SegmentoFaixa("Normal", 18.5, 25.0), SegmentoFaixa("Sobrepeso", 25.0, 30.0), SegmentoFaixa("Obesidade", 30.0, 40.0))
    @Test fun reguaAchaOTrechoEMarcaAPosicao() {
        assertEquals("Normal", segmentoDoValor(18.5, imc)?.rotulo); assertEquals("Baixo", segmentoDoValor(18.49, imc)?.rotulo)
        assertEquals("Obesidade", segmentoDoValor(40.0, imc)?.rotulo); assertNull(segmentoDoValor(40.1, imc)); assertNull(segmentoDoValor(9.0, imc))
        assertEquals((27.52 - 10.0).toFloat() / 30f, posicaoNaRegua(27.52, imc), 0.0001f)
        assertEquals(1f, posicaoNaRegua(55.0, imc)); assertEquals(1f, largurasRegua(imc).sum(), 0.0001f)
    }
    @Test fun eixoTemFolgaEAceitaValorUnico() {
        val (lo, hi) = limitesEixo(listOf(70.0, 80.0)); assertEquals(69.0, lo, 0.0001); assertEquals(81.0, hi, 0.0001)
        assertEquals(9.0 to 11.0, limitesEixo(listOf(10.0))); assertEquals(0.0 to 1.0, limitesEixo(emptyList()))
    }
    @Test fun curvaInterpolaEPosicionaOPontoEntrePercentis() {
        val p3 = listOf(PontoCurva(0, 2.5), PontoCurva(12, 7.5)); val p50 = listOf(PontoCurva(0, 3.3), PontoCurva(12, 9.6)); val p97 = listOf(PontoCurva(0, 4.4), PontoCurva(12, 12.0))
        assertEquals(5.0, valorNaCurva(p3, 6)!!, 0.0001); assertNull(valorNaCurva(p3, 13)); assertEquals(7.5, valorNaCurva(p3, 12))
        assertEquals(-1, curvaAbaixo(2.0, 6, listOf(p3, p50, p97))); assertEquals(1, curvaAbaixo(7.0, 6, listOf(p3, p50, p97))); assertEquals(2, curvaAbaixo(9.0, 6, listOf(p3, p50, p97)))
    }
}
