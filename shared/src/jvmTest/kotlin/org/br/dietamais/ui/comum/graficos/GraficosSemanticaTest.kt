package org.br.dietamais.ui.comum.graficos

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import org.br.dietamais.dominio.graficos.*
import org.br.dietamais.ui.theme.DietaTheme
import kotlin.test.Test

/** Cada gráfico diz o valor por extenso para leitor de tela, e o "ver como tabela" troca o desenho pelos números. */
@OptIn(ExperimentalTestApi::class)
class GraficosSemanticaTest {
    @Test
    fun roscaDescreveCadaFatiaComPercentual() = runSkikoComposeUiTest {
        setContent { DietaTheme { GraficoRosca(listOf(ItemGrafico("Carboidrato", 50.0), ItemGrafico("Proteína", 20.0), ItemGrafico("Gordura", 30.0))) } }
        onNodeWithContentDescription("Carboidrato: 50%; Proteína: 20%; Gordura: 30%").assertIsDisplayed()
    }

    @Test
    fun reguaDizOTrechoDoValorEmPalavras() = runSkikoComposeUiTest {
        val trechos = listOf(TrechoRegua(SegmentoFaixa("Normal", 18.5, 25.0), androidx.compose.ui.graphics.Color.Green), TrechoRegua(SegmentoFaixa("Sobrepeso", 25.0, 30.0), androidx.compose.ui.graphics.Color.Yellow))
        setContent { DietaTheme { ReguaFaixas(trechos, 27.52, "kg/m²") } }
        onNodeWithContentDescription("27,52 kg/m²: Sobrepeso. Normal de 18,5 a 25, Sobrepeso de 25 a 30").assertIsDisplayed()
    }

    @Test
    fun medidorEBarrasTemDescricaoComUnidade() = runSkikoComposeUiTest {
        setContent { DietaTheme { Column { Medidor(26.5, 0.0, 50.0, "% de gordura", "%", faixaIdeal = 14.0..25.0); GraficoBarras(listOf(ItemGrafico("Almoço", 800.0)), unidade = "kcal", meta = 700.0) } } }
        onNodeWithContentDescription("% de gordura: 26,5 %, de 0 a 50, faixa ideal 14 a 25").assertIsDisplayed()
        onNodeWithContentDescription("Almoço: 800 kcal; meta: 700 kcal").assertIsDisplayed()
    }

    @Test
    fun verComoTabelaMostraOsNumerosEOcultaOGrafico() = runSkikoComposeUiTest {
        setContent { DietaTheme { GraficoComTabela(listOf("Peso" to "102,5 kg")) { GraficoRosca(listOf(ItemGrafico("A", 1.0))) } } }
        onNodeWithContentDescription("A: 100%").assertIsDisplayed()
        onNodeWithText("Ver como tabela").performClick()
        onNodeWithText("102,5 kg").assertIsDisplayed()
        onNodeWithText("Ocultar tabela").assertIsDisplayed()
    }
}
