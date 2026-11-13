package org.br.dietamais.ui.nutri

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.window.core.layout.WindowSizeClass
import org.br.dietamais.data.remoto.dto.MetasDieta
import org.br.dietamais.dominio.dieta.TotaisCent
import org.br.dietamais.dominio.dieta.TotaisPorcoes
import org.br.dietamais.ui.nutri.dieta.*
import org.br.dietamais.ui.theme.DietaTheme
import kotlin.test.Test

/** MOB-UX-09: no celular o total do dia fica fixo no topo e as ações no pé; no tablet o cartão de totais segue na coluna lateral. */
@OptIn(ExperimentalTestApi::class)
class TotaisFixosMontadorTest {
    private val celular = WindowSizeClass(390, 844)
    private val tablet = WindowSizeClass(1024, 768)
    private val totais = TotaisCent(kcal = 185_000, carboidratos = 26_000, proteinas = 11_000, gorduras = 5_900)

    @Test fun montadorNoCelularMostraFaixaFixaESalvarPublicarNoPe() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        val estado = EstadoDieta(rascunho = RascunhoDieta(titulo = "Plano", alterado = true), totais = totais)
        setContent { DietaTheme { MontarDietaTela(estado, AcoesMontador(), celular) } }
        onNodeWithTag("faixa-totais").assertIsDisplayed()
        onNodeWithText("1.850 kcal · C 52% · P 22% · L 26%").assertIsDisplayed()
        onNodeWithText("Salvar rascunho").assertIsDisplayed(); onNodeWithText("Publicar").assertIsDisplayed()
        onNodeWithText("Salve o rascunho para poder publicar.").assertExists()
    }

    @Test fun montadorNoTabletMantemOCartaoENaoMostraAFaixa() = runSkikoComposeUiTest(size = Size(1024f, 768f)) {
        val estado = EstadoDieta(rascunho = RascunhoDieta(titulo = "Plano", alterado = true), totais = totais)
        setContent { DietaTheme { MontarDietaTela(estado, AcoesMontador(), tablet) } }
        onNodeWithTag("faixa-totais").assertDoesNotExist()
        onNodeWithTag("totais-dieta").assertIsDisplayed()
        onNodeWithText("Salvar rascunho").assertIsDisplayed()
    }

    @Test fun planejadorNoCelularMostraFaixaComMetaESalvarNoPe() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        val dia = TotaisPorcoes(1850.0, 260.0, 110.0, 59.0, emptyMap())
        val estado = EstadoPlanejador(rascunho = RascunhoPlano(titulo = "Plano", metas = MetasDieta(2000.0, 50.0, 20.0, 30.0), alterado = true), totalDia = dia)
        setContent { DietaTheme { PlanejadorPorcoesTela(estado, AcoesPlanejador(), celular) } }
        onNodeWithText("1.850 de 2.000 kcal · C 52% · P 22% · L 26%").assertIsDisplayed()
        onNodeWithText("Salvar").assertIsDisplayed().assertIsEnabled()
    }

    @Test fun planejadorNoTabletNaoMostraAFaixa() = runSkikoComposeUiTest(size = Size(1024f, 768f)) {
        val estado = EstadoPlanejador(rascunho = RascunhoPlano(titulo = "Plano", alterado = true))
        setContent { DietaTheme { PlanejadorPorcoesTela(estado, AcoesPlanejador(), tablet) } }
        onNodeWithTag("faixa-totais").assertDoesNotExist()
        onNodeWithTag("resultado-vivo").assertIsDisplayed()
    }
}
