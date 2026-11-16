package org.br.dietamais.ui.comum

import kotlin.test.Test
import kotlin.test.assertEquals

class SepararUnidadeTest {
    @Test fun separaUnidadeDoRotulo() = assertEquals("Peso" to "kg", separarUnidade("Peso (kg)"))
    @Test fun semParentesesFicaIgual() = assertEquals("Estatura" to null, separarUnidade("Estatura"))
    @Test fun opcoesComBarraNaoViramUnidade() = assertEquals("Sexo (M/F)" to null, separarUnidade("Sexo (M/F)"))
    @Test fun unidadeLongaNaoViraUnidade() = assertEquals("Idade (em anos completos)" to null, separarUnidade("Idade (em anos completos)"))
}
