package org.br.dietamais.ui.nutri

import androidx.compose.runtime.*
import androidx.compose.material3.Surface
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.ui.nutri.paciente.PacienteFichaTela
import org.br.dietamais.ui.nutri.pacientes.*
import org.br.dietamais.ui.theme.DietaTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.*
import kotlin.time.Instant
import kotlin.uuid.Uuid

@OptIn(ExperimentalTestApi::class)
class PacientesTelasTest {
    private val paciente = PacienteResposta(Uuid.random(), Uuid.random(), "Pessoa fictícia", status = StatusVinculo.CONVIDADO,
        versao = 0, criadoEm = Instant.parse("2026-01-01T00:00:00Z"), atualizadoEm = Instant.parse("2026-01-01T00:00:00Z"))
    private val convite = ConviteCriadoResposta("ABCD2345", "https://example.invalid/convite/ABCD2345", Instant.parse("2026-01-08T00:00:00Z"))

    @Test fun offlineDesabilitaCriacaoEConviteMasMantemFichaLegivel() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        setContent { DietaTheme { PacienteFichaTela(EstadoPacientes(selecionado = paciente, semConexao = true), {}, {}, {}) } }
        onNodeWithText("Pessoa fictícia").assertIsDisplayed()
        onNodeWithText("Gerar convite").performScrollTo().assertIsNotEnabled()
    }

    @Test fun tabsFuturasFicamVaziasSemFabricarDadosClinicos() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        var estado by mutableStateOf(EstadoPacientes(selecionado = paciente))
        setContent { DietaTheme { PacienteFichaTela(estado, { estado = estado.copy(abaFicha = it) }, {}, {}) } }
        onNodeWithText("Anamnese").performScrollTo().performClick()
        onNodeWithText("Nada por aqui ainda.").assertIsDisplayed()
        onNodeWithText("E-mail").assertDoesNotExist()
    }

    @Test fun conviteSoDisparaCompartilhamentoAoClicar() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        var texto: String? = null
        setContent { DietaTheme { ConviteConteudo(EstadoPacientes(convite = convite), { texto = it }, {}) } }
        assertNull(texto)
        onNodeWithText("Compartilhar convite").performScrollTo().performClick()
        assertContains(texto!!, convite.codigo)
        assertContains(texto!!, convite.link)
        assertFalse(texto!!.contains(paciente.nome))
    }

    @Test fun telasNosTresTamanhosEFonteAmpliada() {
        listOf(390 to 844, 820 to 1180, 1180 to 820).forEach { (w, h) ->
            listOf("lista", "ficha", "cadastro", "convite").forEach { capturar(it, w, h, 1f) }
        }
        listOf("lista", "ficha", "cadastro", "convite").forEach { capturar(it, 390, 844, 2f) }
    }

    private fun capturar(tela: String, largura: Int, altura: Int, fonte: Float) = runSkikoComposeUiTest(
        size = Size(largura.toFloat(), altura.toFloat()), density = Density(1f, fonte)) {
        setContent { DietaTheme { Surface {
            val estado = EstadoPacientes(lista = listOf(paciente), selecionado = paciente)
            when (tela) {
                "lista" -> PacientesTela(estado, {}, {}, {}, {}, {})
                "ficha" -> PacienteFichaTela(estado, {}, {}, {})
                "cadastro" -> CriarPacienteFormulario(estado.copy(formulario = FormularioPaciente()), { _, _ -> }, {}, {}, {})
                "convite" -> ConviteConteudo(estado.copy(convite = convite), {}, {})
            }
        } } }
        val acao = when (tela) { "lista" -> "Novo paciente"; "ficha" -> "Gerar convite"; "cadastro" -> "Cadastrar paciente"; else -> "Compartilhar convite" }
        val node = onNodeWithText(acao)
        if (tela != "lista") node.performScrollTo()
        node.assertIsDisplayed()
        val bounds = node.fetchSemanticsNode().boundsInRoot
        assertTrue(bounds.left >= 0 && bounds.right <= largura && bounds.bottom <= altura, "A ação deve caber com fonte $fonte")
        if (tela != "lista") onNodeWithText(when (tela) { "ficha" -> "Voltar para pacientes"; "cadastro" -> "Novo paciente"; else -> "Convide seu paciente" }).performScrollTo()
        val arquivo = File("build/reports/pacientes/$tela-$largura-fonte-$fonte.png")
        arquivo.parentFile.mkdirs()
        ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", arquivo)
    }
}
