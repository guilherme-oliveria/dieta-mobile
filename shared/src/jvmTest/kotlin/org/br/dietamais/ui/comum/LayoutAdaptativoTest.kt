package org.br.dietamais.ui.comum

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.window.core.layout.WindowSizeClass
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.br.dietamais.ui.theme.DietaTheme

@OptIn(ExperimentalTestApi::class)
class LayoutAdaptativoTest {
    @Test
    fun compactaMantemMargensETodosOsBotoesAcessiveis() = verificar(390, 1180, 1f, 350f)

    @Test
    fun mediaLimitaLeituraECentraliza() = verificar(820, 1180, 1f, 592f)

    @Test
    fun expandidaLimitaLeituraECentraliza() = verificar(1180, 1180, 1f, 592f)

    @Test
    fun compactaComFonteDuzentosPorCentoMantemAcoesVisiveis() = verificar(390, 2400, 2f, 350f)

    private fun verificar(largura: Int, altura: Int, escala: Float, larguraBotao: Float) = runSkikoComposeUiTest(
        size = Size(largura.toFloat(), altura.toFloat()), density = Density(1f, escala),
    ) {
        setContent {
            DietaTheme { GaleriaComponentesBase(WindowSizeClass(largura, altura)) }
        }

        onNodeWithText("Comi").assertIsDisplayed()
        onNodeWithText("Troquei").assertIsDisplayed()
        onNodeWithText("Pulei").assertIsDisplayed()
        val botao = onNodeWithText("Continuar").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertEquals(larguraBotao, botao.width, 1f)
        assertTrue(botao.left >= 20f && botao.right <= largura - 20f)

        val arquivo = File("build/reports/componentes/base-$largura-fonte-$escala.png")
        arquivo.parentFile.mkdirs()
        ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", arquivo)
    }
}
