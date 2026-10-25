package org.br.dietamais.ui.comum

import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.window.core.layout.WindowSizeClass
import org.br.dietamais.ui.theme.DietaTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ComponentesBaseTest {
    @Test
    fun botaoEmCarregamentoImpedeEnvioDuplicadoEMantemRotulo() = runSkikoComposeUiTest {
        var envios = 0
        setContent {
            var carregando by remember { mutableStateOf(false) }
            DietaTheme {
                BotaoPrincipal("Salvar", carregando = carregando, aoClicar = {
                    envios++
                    carregando = true
                })
            }
        }

        onNodeWithText("Salvar").performClick()
        onNodeWithText("Salvar").assertIsNotEnabled().assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Carregando"))
        assertEquals(1, envios)
    }

    @Test
    fun campoDecimalPreservaVirgulaEAnunciaErro() = runSkikoComposeUiTest {
        var recebido = ""
        setContent {
            var valor by remember { mutableStateOf("") }
            DietaTheme {
                CampoNumero(
                    rotulo = "Quantidade",
                    valor = valor,
                    aoMudar = { valor = it; recebido = it },
                    erro = "Confira a quantidade",
                )
            }
        }

        onNodeWithContentDescription("Quantidade").performTextInput("1,5")
        assertEquals("1,5", recebido)
        onNodeWithContentDescription("Quantidade")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, "Confira a quantidade"))
        onNodeWithText("Confira a quantidade").assertIsDisplayed()
    }

    @Test
    fun cartaoDeRefeicaoEntregaCadaAcaoAoChamador() = runSkikoComposeUiTest {
        val acoes = mutableListOf<String>()
        setContent {
            DietaTheme {
                CardProximaRefeicao(
                    nome = "Almoço", horario = "12:00", energia = "737 kcal",
                    descricao = "Arroz, feijão e salada",
                    aoComer = { acoes += "comer" },
                    aoTrocar = { acoes += "trocar" },
                    aoPular = { acoes += "pular" },
                )
            }
        }

        onNodeWithText("Comi").performClick()
        onNodeWithText("Troquei").performClick()
        onNodeWithText("Pulei").performClick()
        assertEquals(listOf("comer", "trocar", "pular"), acoes)
    }

    @Test
    fun botaoCresceComFonteDuzentosPorCentoSemCortarTexto() = runSkikoComposeUiTest(
        size = Size(320f, 500f), density = Density(1f, 2f),
    ) {
        setContent {
            DietaTheme {
                BotaoPrincipal("Confirmar e continuar para a próxima etapa", aoClicar = {})
            }
        }

        val node = onNodeWithText("Confirmar e continuar para a próxima etapa")
        node.assertIsDisplayed()
        val altura = node.fetchSemanticsNode().boundsInRoot.height
        assertTrue(altura > 52f, "O botão precisa crescer para acomodar o texto em 200%")
    }

    @Test
    fun estadosVazioEErroDisparamSuasAcoes() = runSkikoComposeUiTest {
        var criacoes = 0
        var tentativas = 0
        setContent {
            DietaTheme {
                Column {
                    EstadoVazio(textoAcao = "Criar", aoAcionar = { criacoes++ })
                    EstadoErro(aoTentarNovamente = { tentativas++ })
                }
            }
        }
        onNodeWithText("Criar").performClick()
        onNodeWithText("Tentar novamente").performClick()
        assertEquals(1, criacoes)
        assertEquals(1, tentativas)
    }

    @Test
    fun todosOsEstilosDeBotaoImpedemAcaoDuranteCarregamento() = runSkikoComposeUiTest {
        setContent {
            DietaTheme {
                Column {
                    BotaoPrincipal("Principal", {}, carregando = true)
                    BotaoContornado("Contornado", {}, carregando = true)
                    BotaoTexto("Texto", {}, carregando = true)
                }
            }
        }
        listOf("Principal", "Contornado", "Texto").forEach {
            onNodeWithText(it).assertIsNotEnabled()
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Carregando"))
        }
    }

    @Test
    fun estadoErroCentralizaAcaoNaAreaDeLeitura() = runSkikoComposeUiTest(size = Size(820f, 500f)) {
        setContent {
            DietaTheme {
                ConteudoLeitura(classeJanela = WindowSizeClass(820, 500)) {
                    EstadoErro(aoTentarNovamente = {})
                }
            }
        }
        val acao = onNodeWithText("Tentar novamente").fetchSemanticsNode().boundsInRoot
        assertEquals(410f, acao.center.x, 1f)
    }
}
