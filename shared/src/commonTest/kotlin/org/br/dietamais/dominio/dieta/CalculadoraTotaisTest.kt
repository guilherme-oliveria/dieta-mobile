package org.br.dietamais.dominio.dieta

import kotlin.test.*

class CalculadoraTotaisTest {
    private val pao = MacrosPorcao(150.0, 28.3333, 4.5, 1.25, 50.0)

    @Test fun itemMultiplicaPorcoesEArredondaMeioParaCima() {
        val t = CalculadoraTotais.item(pao, 1.5)
        assertEquals(22500, t.kcal); assertEquals(4250, t.carboidratos); assertEquals(675, t.proteinas); assertEquals(188, t.gorduras)
    }
    @Test fun somaUsaItensJaArredondadosEVazioDaZero() {
        val soma = CalculadoraTotais.somar(listOf(CalculadoraTotais.item(pao, 1.0), CalculadoraTotais.item(pao, 2.0)))
        assertEquals(45000, soma.kcal); assertEquals(450.0, soma.kcalReais)
        assertEquals(TotaisCent(), CalculadoraTotais.somar(emptyList()))
        assertEquals(7500, CalculadoraTotais.gramas(pao, 1.5))
    }
    @Test fun porcoesFracionadasComQuatroCasas() {
        assertEquals(1500, CalculadoraTotais.item(MacrosPorcao(150.0, 0.0, 0.0, 0.0), 0.1).kcal)
        assertEquals(5, CalculadoraTotais.item(MacrosPorcao(45.0, 0.0, 0.0, 0.0), 0.0011).kcal)
        assertEquals(0, CalculadoraTotais.item(MacrosPorcao(45.0, 0.0, 0.0, 0.0), 0.0001).kcal)
    }
    /** Dieta fictícia de ≈2510 kcal montada com as kcal por porção do catálogo (a mesma da carga de exemplo do backend). */
    @Test fun dietaFicticiaDe2510Kcal() {
        fun g(kcal: Double) = MacrosPorcao(kcal, 0.0, 0.0, 0.0)
        val itens = listOf(150.0 to 2.0, 55.0 to 2.0, 70.0 to 1.0, 70.0 to 2.0, 30.0 to 2.0, 80.0 to 4.0, 45.0 to 3.0, 200.0 to 3.0, 50.0 to 3.0, 45.0 to 1.0,
            150.0 to 1.0, 55.0 to 1.0, 70.0 to 1.0, 250.0 to 1.0, 55.0 to 1.0)
        val total = CalculadoraTotais.somar(itens.map { (k, p) -> CalculadoraTotais.item(g(k), p) })
        assertEquals(2510.0, total.kcalReais)
    }
}
