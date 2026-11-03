package org.br.dietamais.ui

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import org.br.dietamais.data.remoto.JsonContrato
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.remoto.recursoContrato
import org.br.dietamais.ui.nutri.alimentos.*
import org.br.dietamais.ui.theme.DietaTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class AlimentosTelasTest {
    private val grupo = JsonContrato.decodeFromString<GruposAlimentosResposta>(recursoContrato("fase-2/listarGruposAlimentos/resposta.json")).itens.single()
    private val alimento = JsonContrato.decodeFromString<AlimentoResposta>(recursoContrato("fase-2/criarAlimento/resposta.json"))
    private fun estado() = EstadoAlimentos(grupos = listOf(grupo), lista = listOf(alimento))
    @Test fun offlinePreservaItemEMantemCriarEditarDesativar() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        setContent { DietaTheme { AlimentosTela(estado().copy(semConexao = true)) } }
        onNodeWithText("Novo alimento").assertIsEnabled()
        onNodeWithTag("lista-alimentos").performScrollToNode(hasText("Alimento fictício"))
        onNodeWithText("Alimento fictício").assertIsDisplayed()
        onNodeWithText("Editar").assertIsEnabled(); onNodeWithText("Desativar").assertIsEnabled()
    }
    @Test fun formularioAceitaDecimalLocalEMantemAcaoExplicita() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        var estado by mutableStateOf(estado().copy(formulario = FormularioAlimento(grupoId = grupo.id)))
        var salvou = false
        setContent { DietaTheme { FormularioAlimentoTela(estado,
            aoCampo = { campo, valor -> if (campo == CampoAlimento.GRAMAS) estado = estado.copy(formulario = estado.formulario!!.copy(gramas = valor)) },
            aoSalvar = { salvou = true }) } }
        onNodeWithContentDescription("Gramas por porção").performScrollTo().performTextInput("50,5")
        assertEquals("50,5", estado.formulario!!.gramas); assertFalse(salvou)
        onNodeWithText("Salvar alimento").performScrollTo().performClick(); assertTrue(salvou)
    }
    @Test fun confirmarDesativacaoExigeAcaoEExplicaSnapshotsPreservados() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        var confirmou = false
        setContent { DietaTheme { ConfirmarDesativacao(alimento, false, { confirmou = true }, {}) } }
        onNodeWithText("As dietas já publicadas mantêm este alimento. Ele deixa de aparecer nas opções ativas.").assertIsDisplayed()
        assertFalse(confirmou); onNode(hasText("Desativar alimento") and hasClickAction()).performClick(); assertTrue(confirmou)
    }
    @Test fun telasNosTresTamanhosEFonteDuzentosPorCento() {
        listOf(390 to 844, 820 to 1180, 1180 to 820).forEach { (w, h) -> capturar(w, h, 1f) }
        capturar(390, 844, 2f)
    }
    private fun capturar(largura: Int, altura: Int, fonte: Float) {
        listOf(false, true).forEach { formulario -> runSkikoComposeUiTest(size = Size(largura.toFloat(), altura.toFloat()), density = Density(1f, fonte)) {
            val valor = estado().copy(formulario = if (formulario) FormularioAlimento(grupoId = grupo.id, nome = alimento.nome,
                medida = alimento.medidaCaseira, gramas = "50") else null)
            setContent { DietaTheme { if (formulario) FormularioAlimentoTela(valor) else AlimentosTela(valor) } }
            val acao = if (formulario) "Salvar alimento" else "Novo alimento"
            onNodeWithText(acao).performScrollTo().assertIsDisplayed()
            val bounds = onNodeWithText(acao).fetchSemanticsNode().boundsInRoot
            assertTrue(bounds.left >= 0 && bounds.right <= largura && bounds.bottom <= altura)
            val arquivo = File("build/reports/alimentos/${if (formulario) "formulario" else "lista"}-$largura-fonte-$fonte.png")
            arquivo.parentFile.mkdirs(); ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", arquivo)
        } }
    }
}
