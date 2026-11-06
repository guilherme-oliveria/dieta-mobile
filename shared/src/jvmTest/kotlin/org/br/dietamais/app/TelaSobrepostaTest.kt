package org.br.dietamais.app

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.window.core.layout.WindowSizeClass
import org.br.dietamais.ui.comum.ItemNavegacao
import org.br.dietamais.ui.comum.TelaSobreposta
import org.br.dietamais.ui.comum.icones.IconesDieta
import org.br.dietamais.ui.theme.DietaTheme
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class TelaSobrepostaTest {
    private val itens = listOf(ItemNavegacao("a", "Aba A", IconesDieta.Casa), ItemNavegacao("b", "Aba B", IconesDieta.Pessoa))

    @Test
    fun telaCheiaFicaAcimaDaAbaMasAbaixoDoMenu() = runSkikoComposeUiTest {
        var aberta by mutableStateOf(false)
        setContent {
            DietaTheme {
                AppScaffold(itens, "a", {}, WindowSizeClass(390, 820)) {
                    Column {
                        Text("Conteúdo da aba")
                        if (aberta) TelaSobreposta({ aberta = false }) { Text("Montar dieta") }
                    }
                }
            }
        }
        onNodeWithText("Conteúdo da aba").assertIsDisplayed()
        runOnUiThread { aberta = true }
        onNodeWithText("Montar dieta").assertIsDisplayed()
        onNodeWithText("Aba A").assertIsDisplayed()
        // o que fica por baixo sai da acessibilidade
        onNodeWithText("Conteúdo da aba").assertDoesNotExist()
        runOnUiThread { aberta = false }
        onNodeWithText("Montar dieta").assertDoesNotExist()
        onNodeWithText("Conteúdo da aba").assertIsDisplayed()
    }

    @Test
    fun semShellCaiNoDialogDeTelaCheia() = runSkikoComposeUiTest {
        setContent { DietaTheme { TelaSobreposta({}) { Text("Montar dieta") } } }
        onNodeWithText("Montar dieta").assertIsDisplayed()
    }
}
