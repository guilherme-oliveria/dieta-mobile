package org.br.dietamais

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import org.br.dietamais.ui.theme.DietaTheme

/** MOB-40: ponto de entrada da web. O app de verdade entra em MOB-42 (sessão no navegador). */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    document.getElementById("carregando")?.let { it.parentNode?.removeChild(it) }
    ComposeViewport(document.body!!) {
        DietaTheme { Text("Dieta+ no navegador", style = MaterialTheme.typography.headlineMedium) }
    }
}
