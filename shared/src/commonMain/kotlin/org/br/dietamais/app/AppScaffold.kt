package org.br.dietamais.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.window.core.layout.WindowSizeClass
import org.br.dietamais.ui.comum.BarraNavegacaoInferior
import org.br.dietamais.ui.comum.ItemNavegacao
import org.br.dietamais.ui.comum.TrilhoNavegacao

@Composable
fun AppScaffold(
    itens: List<ItemNavegacao>,
    selecionada: String?,
    aoSelecionar: (String) -> Unit,
    classeJanela: WindowSizeClass,
    content: @Composable () -> Unit,
) {
    val compacta = !classeJanela.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = { if (compacta) BarraNavegacaoInferior(itens, selecionada, aoSelecionar) },
    ) { padding ->
        Row(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            if (!compacta) TrilhoNavegacao(itens, selecionada, aoSelecionar)
            Box(Modifier.weight(1f).fillMaxSize()) { content() }
        }
    }
}
