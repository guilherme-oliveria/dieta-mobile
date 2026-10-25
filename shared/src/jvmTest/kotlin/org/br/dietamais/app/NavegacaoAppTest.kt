package org.br.dietamais.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.navigation.NavHostController
import androidx.window.core.layout.WindowSizeClass
import java.io.File
import javax.imageio.ImageIO
import org.br.dietamais.ui.theme.DietaTheme
import org.br.dietamais.ui.theme.Dimens
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class NavegacaoAppTest {
    @Test
    fun pacienteNavegaEntreAbasSemGanharDestinosDaNutri() = runSkikoComposeUiTest {
        setContent { DietaTheme { NavegacaoApp(PerfilNavegacao.PACIENTE) } }

        onNode(hasText("Dieta") and hasClickAction()).performClick().assertIsSelected()
        onNode(hasText("Compras") and hasClickAction()).performClick().assertIsSelected()
        onNodeWithText("Pacientes").assertDoesNotExist()
        onNodeWithText("Conta").assertDoesNotExist()
    }

    @Test
    fun trocaDePerfilRecriaGrafoSemRestaurarAbaDoPerfilAnterior() = runSkikoComposeUiTest {
        var perfil by mutableStateOf(PerfilNavegacao.PACIENTE)
        setContent { DietaTheme { NavegacaoApp(perfil) } }
        onNode(hasText("Compras") and hasClickAction()).performClick().assertIsSelected()
        runOnUiThread { perfil = PerfilNavegacao.NUTRICIONISTA }
        onNode(hasText("Início") and hasClickAction()).assertIsSelected()
        onNodeWithText("Compras").assertDoesNotExist()
        runOnUiThread { perfil = PerfilNavegacao.PACIENTE }
        onNode(hasText("Hoje") and hasClickAction()).assertIsSelected()
    }

    @Test
    fun fichaMantemAbaPacientesERetornaNaPilhaCorreta() = runSkikoComposeUiTest {
        lateinit var controlador: NavHostController
        setContent {
            DietaTheme {
                NavegacaoApp(PerfilNavegacao.NUTRICIONISTA, aoPrepararNavegacao = { controlador = it })
            }
        }
        onNode(hasText("Pacientes") and hasClickAction()).performClick()
        runOnUiThread { controlador.navigate(RotaNutri.FichaPaciente("00000000-0000-7000-8000-000000000001")) }
        onNode(hasText("Pacientes") and hasClickAction()).assertIsSelected()
        onNodeWithText("Ficha do paciente").assertIsDisplayed()
        onNode(hasText("Agenda") and hasClickAction()).performClick().assertIsSelected()
        onNode(hasText("Pacientes") and hasClickAction()).performClick().assertIsSelected()
        onNodeWithText("Ficha do paciente").assertIsDisplayed()
        runOnUiThread { assertTrue(controlador.popBackStack()) }
        onNodeWithText("Ficha do paciente").assertDoesNotExist()
        onNode(hasText("Pacientes") and hasClickAction()).assertIsSelected()
    }

    @Test
    fun redimensionarJanelaMantemFichaEPilhaSelecionadas() = runSkikoComposeUiTest(size = Size(1180f, 820f)) {
        var classe by mutableStateOf(WindowSizeClass(390, 820))
        lateinit var controlador: NavHostController
        setContent {
            DietaTheme {
                NavegacaoApp(PerfilNavegacao.NUTRICIONISTA, classe, aoPrepararNavegacao = { controlador = it })
            }
        }
        onNode(hasText("Pacientes") and hasClickAction()).performClick()
        runOnUiThread { controlador.navigate(RotaNutri.FichaPaciente("00000000-0000-7000-8000-000000000002")) }
        onNodeWithText("Ficha do paciente").assertIsDisplayed()
        runOnUiThread { classe = WindowSizeClass(1180, 820) }
        onNodeWithText("Ficha do paciente").assertIsDisplayed()
        onNode(hasText("Pacientes") and hasClickAction()).assertIsSelected()
        runOnUiThread { classe = WindowSizeClass(390, 820) }
        onNodeWithText("Ficha do paciente").assertIsDisplayed()
        runOnUiThread { assertTrue(controlador.popBackStack()) }
        onNodeWithText("Ficha do paciente").assertDoesNotExist()
    }

    @Test
    fun sessaoAusenteMostraEntradaSemLiberarAbas() = runSkikoComposeUiTest {
        setContent { DietaTheme { NavegacaoApp(perfil = null) } }
        onNodeWithText("Entrar").assertIsDisplayed()
        onNodeWithContentDescription("Navegação principal").assertDoesNotExist()
        onNodeWithText("Pacientes").assertDoesNotExist()
    }

    @Test
    fun compactoTemBarraInferior() = verificarLayout(390, 844, PerfilNavegacao.PACIENTE)

    @Test
    fun medioTemTrilhoEPainelUnico() = verificarLayout(820, 1180, PerfilNavegacao.NUTRICIONISTA)

    @Test
    fun expandidoTemTrilhoEListaDetalhe() = verificarLayout(1180, 820, PerfilNavegacao.NUTRICIONISTA)

    @Test
    fun compactoComFonteDuzentosPorCentoMantemAbasClicaveis() = verificarLayout(390, 1100, PerfilNavegacao.NUTRICIONISTA, 2f)

    private fun verificarLayout(largura: Int, altura: Int, perfil: PerfilNavegacao, fonte: Float = 1f) = runSkikoComposeUiTest(
        size = Size(largura.toFloat(), altura.toFloat()), density = Density(1f, fonte),
    ) {
        setContent {
            DietaTheme { NavegacaoApp(perfil, WindowSizeClass(largura, altura)) }
        }
        if (perfil == PerfilNavegacao.NUTRICIONISTA) {
            onNode(hasText("Pacientes") and hasClickAction()).performClick().assertIsSelected()
        }
        val navegacao = onNodeWithContentDescription("Navegação principal").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        if (largura < 600) {
            assertTrue(navegacao.top > altura / 2f)
            assertTrue(navegacao.width > largura / 2f)
        } else {
            assertTrue(navegacao.left == 0f)
            assertTrue(navegacao.width < largura / 2f)
        }
        if (largura >= 840) onNodeWithText("Selecione um paciente para ver os detalhes.").assertIsDisplayed()
        else onNodeWithText("Selecione um paciente para ver os detalhes.").assertDoesNotExist()

        if (fonte > 1f) {
            listOf("Início", "Pacientes", "Agenda", "Alimentos", "Conta").forEach {
                onNode(hasText(it) and hasClickAction()).assertIsDisplayed()
                val texto = onNode(hasText(it) and hasAnyAncestor(hasContentDescription("Navegação principal")), useUnmergedTree = true)
                assertTrue(texto.getUnclippedBoundsInRoot().bottom.value <= altura, "O rótulo $it não pode ser cortado")
                val layouts = mutableListOf<TextLayoutResult>()
                texto.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { acao -> acao(layouts) }
                val alturaNecessaria = layouts.maxOf { layout -> layout.size.height } + Dimens.iconePadrao.value + 2 * Dimens.espacoPequeno.value
                assertTrue(navegacao.height >= alturaNecessaria, "O rótulo $it precisa caber com o ícone e espaçamento")
            }
        }
        val arquivo = File("build/reports/navegacao/shell-$largura-fonte-$fonte.png")
        arquivo.parentFile.mkdirs()
        ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", arquivo)
    }
}
