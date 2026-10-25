package org.br.dietamais.ui.auth

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import org.br.dietamais.data.remoto.dto.Perfil
import org.br.dietamais.ui.theme.DietaTheme
import org.br.dietamais.App
import org.br.dietamais.plataforma.FabricaDriver
import org.br.dietamais.plataforma.IdentidadeDispositivo
import org.br.dietamais.data.remoto.CofreMemoria
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain

@OptIn(ExperimentalTestApi::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AutenticacaoTelasTest {
    @Test fun appComDiRealAbreEntradaENavegaParaCadastroERecuperacao() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        val pasta = java.nio.file.Files.createTempDirectory("dietamais-auth-teste").toFile()
        var visivel by mutableStateOf(true)
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            setContent {
                if (visivel) {
                    val fabrica = remember { FabricaDriver(pasta.path) }
                    val cofre = remember { CofreMemoria() }
                    val identidade = remember { IdentidadeDispositivo() }
                    App(fabricaDriver = fabrica, cofreSessao = cofre, identidadeDispositivo = identidade)
                }
            }
            onNodeWithText("Sou nutricionista").performScrollTo().performClick()
            onNodeWithText("Crie sua conta").assertIsDisplayed()
            onNodeWithText("Voltar para entrar").performScrollTo().performClick()
            onNodeWithText("Esqueci a senha").performScrollTo().performClick()
            onNodeWithText("Recupere sua senha").assertIsDisplayed()
        } finally {
            try {
                // O diretório contém apenas o SQLite fictício criado por este teste.
                runOnUiThread { visivel = false }
                waitForIdle()
                check(pasta.parentFile.canonicalFile == File(System.getProperty("java.io.tmpdir")).canonicalFile && pasta.name.startsWith("dietamais-auth-teste"))
                pasta.deleteRecursively()
            } finally { Dispatchers.resetMain() }
        }
    }
    @Test fun senhaComecaProtegidaEAlternaVisibilidadeSemAlterarValor() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        var estado by mutableStateOf(EstadoAutenticacao(restaurando = false))
        setContent { DietaTheme { EntrarTela(estado, { estado = estado.copy(email = it) }, { estado = estado.copy(senha = it) }, {}, {}, {}) } }
        onNodeWithContentDescription("Senha").performTextInput("SENHA_FICTICIA")
        onNodeWithContentDescription("Senha").assert(SemanticsMatcher.expectValue(SemanticsProperties.Password, Unit))
        onNodeWithContentDescription("Mostrar senha").performClick()
        assertEquals("SENHA_FICTICIA", estado.senha)
        onNodeWithContentDescription("Ocultar senha").assertIsDisplayed().performClick()
        onNodeWithContentDescription("Senha").assert(SemanticsMatcher.expectValue(SemanticsProperties.Password, Unit))
    }

    @Test fun souNutricionistaDisparaCadastroSemLiberarShell() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        var cadastro = false
        setContent { DietaTheme { EntrarTela(EstadoAutenticacao(restaurando = false), {}, {}, {}, { cadastro = true }, {}) } }
        onNodeWithText("Sou nutricionista").performScrollTo().performClick()
        assertTrue(cadastro)
        onNodeWithText("Pacientes").assertDoesNotExist()
    }

    @Test fun cadastroSemTermosDesabilitaEnvioEExplicaIndisponibilidade() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        setContent { DietaTheme { CadastroNutriTela(EstadoAutenticacao(restaurando = false), null, {}, {}, {}, {}, {}, {}, {}) } }
        onNodeWithText("Criar conta").performScrollTo().assertIsNotEnabled()
        onNodeWithText("Os termos de uso estão indisponíveis. Tente novamente mais tarde.").performScrollTo().assertIsDisplayed()
    }

    @Test fun aceiteTemAlvoDeToqueELeituraDisponivelAntesDeAceitar() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        var estado by mutableStateOf(EstadoAutenticacao(restaurando = false))
        setContent { DietaTheme { CadastroNutriTela(estado, TermosCadastro("TESTE", "Texto fictício de teste."), {}, {}, {}, {},
            { estado = estado.copy(aceitouTermos = it) }, {}, {}) } }
        val aceite = onNodeWithText("Li e aceito os termos de uso").performScrollTo()
        assertTrue(aceite.fetchSemanticsNode().boundsInRoot.height >= 48f)
        onNodeWithText("Ler termos de uso").performScrollTo().performClick()
        onNodeWithText("Texto fictício de teste.").assertIsDisplayed()
        assertFalse(estado.aceitouTermos)
        onNodeWithText("Fechar").performClick()
        aceite.performClick()
        assertTrue(estado.aceitouTermos)
    }

    @Test fun escolhaNuncaMostraPerfilDesconhecido() = runSkikoComposeUiTest {
        var selecionado: Perfil? = null
        setContent { DietaTheme { EscolherPerfilTela(EstadoAutenticacao(restaurando = false, perfis = Perfil.entries), { selecionado = it }, {}) } }
        onNodeWithText("Como paciente").performClick()
        assertEquals(Perfil.PACIENTE, selecionado)
        onNodeWithText("ADMIN").assertDoesNotExist()
        onNodeWithText("OUTRO").assertDoesNotExist()
    }

    @Test fun telasNosTresTamanhosEFonteDuzentosPorCento() {
        listOf(390 to 844, 820 to 1180, 1180 to 820).forEach { (largura, altura) ->
            TelaAutenticacao.entries.forEach { tela -> capturar(tela, largura, altura, 1f) }
        }
        TelaAutenticacao.entries.forEach { capturar(it, 390, 844, 2f) }
    }

    private fun capturar(tela: TelaAutenticacao, largura: Int, altura: Int, fonte: Float) = runSkikoComposeUiTest(
        size = Size(largura.toFloat(), altura.toFloat()), density = Density(1f, fonte),
    ) {
        val estado = EstadoAutenticacao(tela = tela, restaurando = false, perfis = listOf(Perfil.NUTRICIONISTA, Perfil.PACIENTE))
        setContent { DietaTheme {
            when (tela) {
                TelaAutenticacao.ENTRAR -> EntrarTela(estado, {}, {}, {}, {}, {})
                TelaAutenticacao.CADASTRO -> CadastroNutriTela(estado, TermosCadastro("TESTE", "Termos fictícios apenas para teste visual."), {}, {}, {}, {}, {}, {}, {})
                TelaAutenticacao.RECUPERAR_SENHA -> RecuperarSenhaTela(estado, {}, {}, {}, {}, {}, {})
                TelaAutenticacao.ESCOLHER_PERFIL -> EscolherPerfilTela(estado, {}, {})
            }
        } }
        val acao = when (tela) {
            TelaAutenticacao.ENTRAR -> "Sou nutricionista"
            TelaAutenticacao.CADASTRO -> "Criar conta"
            TelaAutenticacao.RECUPERAR_SENHA, TelaAutenticacao.ESCOLHER_PERFIL -> "Voltar para entrar"
        }
        onNodeWithText(acao).performScrollTo().assertIsDisplayed()
        val bounds = onNodeWithText(acao).fetchSemanticsNode().boundsInRoot
        assertTrue(bounds.left >= 0f && bounds.right <= largura && bounds.bottom <= altura,
            "A ação $acao deve caber no viewport, inclusive com fonte ampliada")
        // Retorna ao topo para a evidência visual da composição inicial.
        onNodeWithText(when (tela) {
            TelaAutenticacao.ENTRAR -> "Dieta+"
            TelaAutenticacao.CADASTRO -> "Crie sua conta"
            TelaAutenticacao.RECUPERAR_SENHA -> "Recupere sua senha"
            TelaAutenticacao.ESCOLHER_PERFIL -> "Como você quer entrar?"
        }).performScrollTo()
        val arquivo = File("build/reports/autenticacao/${tela.name.lowercase()}-$largura-fonte-$fonte.png")
        arquivo.parentFile.mkdirs()
        ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", arquivo)
    }
}
