package org.br.dietamais.ui.auth

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.ui.auth.convite.*
import org.br.dietamais.ui.theme.DietaTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.*
import kotlin.time.Instant
import kotlin.uuid.Uuid

@OptIn(ExperimentalTestApi::class)
class ConviteTelasTest {
    private val convite = ConviteResposta(NutricionistaResumoResposta(Uuid.random(), "Nutri fictícia", "CRN-1 0000"),
        TermoConsentimentoResposta("TESTE", "Consentimento fictício apenas para teste."), Instant.parse("2030-01-01T00:00:00Z"))
    private val usuario = UsuarioResposta(Uuid.random(), "Conta fictícia", "conta@example.invalid", true,
        listOf(Perfil.PACIENTE), Perfil.PACIENTE, pacienteId = Uuid.random(), fuso = "America/Sao_Paulo")

    @Test fun aceiteNaoVemMarcadoETemAlvoDeToqueAntesDeContinuar() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        var estado by mutableStateOf(EstadoConvite(visivel = true, codigo = "ABCD2345", convite = convite))
        var aceites = 0
        setContent { Tela(estado, usuario, { estado = estado.copy(aceitouConsentimento = it) }, { aceites++ }) }
        onNodeWithText("Aceitar convite").performScrollTo().assertIsNotEnabled()
        val checkbox = onNodeWithText("Li e autorizo o atendimento descrito no consentimento clínico").performScrollTo()
        assertTrue(checkbox.fetchSemanticsNode().boundsInRoot.height >= 48f)
        assertEquals(0, aceites)
        checkbox.performClick()
        onNodeWithText("Aceitar convite").performScrollTo().assertIsEnabled().performClick()
        assertEquals(1, aceites)
    }
    @Test fun cadastroNovoPermaneceDesabilitadoSemTermosPublicados() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        setContent { Tela(EstadoConvite(visivel = true, codigo = "ABCD2345", convite = convite, cadastro = true)) }
        onNodeWithText("Criar conta e aceitar convite").performScrollTo().assertIsNotEnabled()
        onNodeWithText("O cadastro aguarda a aprovação e publicação dos termos de uso. Você pode entrar com uma conta existente.").performScrollTo().assertIsDisplayed()
    }
    @Test fun termosPublicadosTemAceiteSeparadoDoConsentimentoClinico() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        val termos = TermosUsoResposta("TERMOS-TESTE", "PUBLICADO", true, true, true, Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2026-01-02T00:00:00Z"), "Texto fictício apenas de teste.", "/legal/terms-of-use")
        var estado by mutableStateOf(EstadoConvite(visivel = true, codigo = "ABCD2345", convite = convite, cadastro = true, termosUso = termos))
        setContent { Tela(estado, aoConsentimento = { estado = estado.copy(aceitouConsentimento = it) },
            aoTermosUso = { estado = estado.copy(aceitouTermosUso = it) }) }
        onNodeWithText("Li e aceito os termos de uso").performScrollTo().performClick()
        onNodeWithText("Criar conta e aceitar convite").performScrollTo().assertIsNotEnabled()
        onNodeWithText("Li e autorizo o atendimento descrito no consentimento clínico").performScrollTo().performClick()
        onNodeWithText("Criar conta e aceitar convite").performScrollTo().assertIsEnabled()
    }
    @Test fun telasNosTresTamanhosEFonteDuzentosPorCento() {
        listOf(390 to 844, 820 to 1180, 1180 to 820).forEach { (w, h) ->
            listOf("codigo", "consentimento", "cadastro", "autenticado").forEach { capturar(it, w, h, 1f) }
        }
        listOf("codigo", "consentimento", "cadastro", "autenticado").forEach { capturar(it, 390, 844, 2f) }
    }
    @Composable private fun Tela(estado: EstadoConvite, usuario: UsuarioResposta? = null,
        aoConsentimento: (Boolean) -> Unit = {}, aoAceitar: () -> Unit = {}, aoTermosUso: (Boolean) -> Unit = {}) = DietaTheme {
        ConviteTela(estado, usuario, {}, {}, aoConsentimento, {}, {}, { _, _ -> }, aoAceitar, {}, aoTermosUso = aoTermosUso)
    }
    private fun capturar(tela: String, largura: Int, altura: Int, fonte: Float) = runSkikoComposeUiTest(
        size = Size(largura.toFloat(), altura.toFloat()), density = Density(1f, fonte)) {
        val estado = EstadoConvite(visivel = true, codigo = "ABCD2345", convite = convite.takeUnless { tela == "codigo" }, cadastro = tela == "cadastro")
        setContent { Tela(estado, usuario.takeIf { tela == "autenticado" }) }
        val acao = when (tela) { "codigo" -> "Consultar convite"; "cadastro" -> "Criar conta e aceitar convite"; "autenticado" -> "Aceitar convite"; else -> "Já tenho uma conta" }
        val node = onNodeWithText(acao).performScrollTo().assertIsDisplayed()
        val bounds = node.fetchSemanticsNode().boundsInRoot
        assertTrue(bounds.left >= 0 && bounds.right <= largura && bounds.bottom <= altura)
        onNodeWithText(if (tela == "cadastro") "Crie sua conta de paciente" else "Seu convite").performScrollTo()
        val arquivo = File("build/reports/convite/$tela-$largura-fonte-$fonte.png")
        arquivo.parentFile.mkdirs()
        ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", arquivo)
    }
}
