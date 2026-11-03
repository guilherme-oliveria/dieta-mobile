package org.br.dietamais.ui.notificacoes

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import kotlinx.datetime.TimeZone
import org.br.dietamais.ui.theme.DietaTheme
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class AvisosTelasTest {
    private val fuso = TimeZone.of("America/Sao_Paulo")

    @Test fun botaoMostraContagemDeNaoLidos() = runSkikoComposeUiTest(size = Size(390f, 200f)) {
        setContent { DietaTheme { BotaoAvisos(EstadoAvisos(avisos = listOf(avisoFalso(), avisoFalso(), avisoFalso(lido = true))), AcoesAvisos()) } }
        onNodeWithText("Avisos (2)").assertIsDisplayed()
    }

    @Test fun botaoSemNaoLidosNaoMostraNumero() = runSkikoComposeUiTest(size = Size(390f, 200f)) {
        setContent { DietaTheme { BotaoAvisos(EstadoAvisos(avisos = listOf(avisoFalso(lido = true))), AcoesAvisos()) } }
        onNodeWithText("Avisos").assertIsDisplayed()
    }

    @Test fun listaMostraAvisosComDataELocalEToqueAbreOAviso() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        val a = avisoFalso("/agenda", quando = "2026-10-28T17:30:00Z"); var tocado: Any? = null
        setContent { DietaTheme { AvisosTela(EstadoAvisos(avisos = listOf(a, avisoFalso(lido = true)), aberto = true), AcoesAvisos(aoTocar = { tocado = it }), fuso) } }
        onAllNodesWithText("Nova dieta").assertCountEquals(2)
        onNodeWithText("28/10/2026 às 14:30").assertIsDisplayed()
        onAllNodesWithText("Nova dieta")[0].performClick()
        assertEquals(a.id, tocado)
        onNodeWithText("Marcar todos como lidos").assertIsEnabled()
    }

    @Test fun vazioDesabilitaMarcarTodosEFechadoNaoDesenha() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        setContent { DietaTheme { AvisosTela(EstadoAvisos(aberto = true), AcoesAvisos()) } }
        onNodeWithText("Nenhum aviso por enquanto.").assertIsDisplayed()
        onNodeWithText("Marcar todos como lidos").assertIsNotEnabled()
    }

    @Test fun fechadoNaoDesenhaNada() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        setContent { DietaTheme { AvisosTela(EstadoAvisos(avisos = listOf(avisoFalso()), aberto = false), AcoesAvisos(), fuso) } }
        onNodeWithText("Nova dieta").assertDoesNotExist()
    }

    @Test fun mensagemDeSemConexaoApareceNaTela() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        setContent { DietaTheme { AvisosTela(EstadoAvisos(avisos = listOf(avisoFalso()), aberto = true, mensagem = MensagemAvisos.SEM_CONEXAO), AcoesAvisos(), fuso) } }
        onNodeWithText("Sem conexão. Tente marcar como lido quando estiver online.").assertIsDisplayed()
    }
}
