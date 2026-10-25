package org.br.dietamais.ui.comum

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.window.core.layout.WindowSizeClass
import org.br.dietamais.ui.theme.Dimens

@Composable
fun LayoutListaDetalhe(
    classeJanela: WindowSizeClass,
    mostrarDetalhe: Boolean,
    lista: @Composable () -> Unit,
    detalhe: @Composable () -> Unit,
) {
    if (classeJanela.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)) {
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.width(Dimens.larguraListaPacientes).fillMaxHeight()) { lista() }
            VerticalDivider(color = androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant)
            Box(Modifier.weight(1f).fillMaxHeight()) { detalhe() }
        }
    } else {
        Box(Modifier.fillMaxSize()) { if (mostrarDetalhe) detalhe() else lista() }
    }
}
