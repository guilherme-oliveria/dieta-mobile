package org.br.dietamais.dominio.dieta

import org.br.dietamais.data.remoto.dto.AdequacaoNutrienteResposta
import kotlin.test.*

class MicronutrientesDriTest {
    private fun a(n: String, pct: Double, c: String) = AdequacaoNutrienteResposta(n, pct, 100.0, pct, c)

    @Test fun nomesLegiveisComFallback() {
        assertEquals("Cálcio", nomeDoNutriente("calcio")); assertEquals("Sódio", nomeDoNutriente("sodio"))
        assertEquals("Vitamina B12", nomeDoNutriente("vitamina_b12")); assertEquals("Vitamina B9", nomeDoNutriente("vitamina_b9"))
        assertEquals("Biotina", nomeDoNutriente("biotina")); assertEquals("Cromo total", nomeDoNutriente("cromo_total"))
    }

    @Test fun classeDesconhecidaViraOutraSemQuebrar() {
        assertEquals(ClasseDri.ABAIXO, classeDri("ABAIXO")); assertEquals(ClasseDri.ACIMA, classeDri(" acima "))
        assertEquals(ClasseDri.OUTRA, classeDri("MUITO_ACIMA")); assertEquals(ClasseDri.OUTRA, classeDri("OUTRA"))
    }

    @Test fun foraDaMetaVemPrimeiroEDepoisOAdequadoPorNome() {
        val linhas = linhasDri(listOf(a("ferro", 100.0, "ADEQUADO"), a("sodio", 160.0, "ACIMA"), a("calcio", 60.0, "ABAIXO"), a("zinco", 98.0, "ADEQUADO")))
        assertEquals(listOf("Cálcio", "Sódio", "Ferro", "Zinco"), linhas.map { it.nome })
        assertTrue(linhasDri(null).isEmpty()); assertTrue(linhasDri(emptyList()).isEmpty())
    }

    @Test fun nomeEUnidadeDoServidorTemPrioridadeEViramValor() {
        val linhas = linhasDri(listOf(AdequacaoNutrienteResposta("ferro", 8.0, 8.0, 100.0, "ADEQUADO", "Ferro (Fe)", "mg/dia"), AdequacaoNutrienteResposta("calcio", 600.0, 1000.0, 60.0, "ABAIXO")))
        assertEquals("Cálcio", linhas[0].nome); assertNull(linhas[0].valor) // sem unidade do servidor, só o percentual
        assertEquals("Ferro (Fe)", linhas[1].nome); assertEquals("8 mg", linhas[1].valor)
        assertEquals("2,5 l", textoValorDri(AdequacaoNutrienteResposta("agua", 2.5, 3.7, 67.6, "ABAIXO", null, "l/dia")))
    }
}
