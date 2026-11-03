package org.br.dietamais.ui.comum

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.window.core.layout.WindowSizeClass
import org.br.dietamais.ui.theme.Dimens

/** Área de leitura centrada; não contém navegação nem define o shell de MOB-03. */
@Composable
fun ConteudoLeitura(
    modifier: Modifier = Modifier,
    classeJanela: WindowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier.widthIn(max = Dimens.larguraLeitura).fillMaxWidth()
                .padding(horizontal = Dimens.margemTela(classeJanela)),
            verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio),
            content = content,
        )
    }
}
