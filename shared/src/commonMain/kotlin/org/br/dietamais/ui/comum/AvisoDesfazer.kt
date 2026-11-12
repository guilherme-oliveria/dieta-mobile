package org.br.dietamais.ui.comum

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import org.br.dietamais.ui.theme.Dimens

/** Aviso temporário "… · Desfazer" (MOB-UX-04). Leitor de tela anuncia o texto; o botão tem alvo de 48 dp. */
@Composable
fun AvisoDesfazer(texto: String, rotuloDesfazer: String, aoDesfazer: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        color = MaterialTheme.colorScheme.inverseSurface, contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        shape = RoundedCornerShape(Dimens.espacoMedio)) {
        Row(Modifier.padding(start = Dimens.paddingCard, end = Dimens.espacoPequeno).heightIn(min = Dimens.alvoToque),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
            Text(texto, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            TextButton(aoDesfazer, Modifier.heightIn(min = Dimens.alvoToque)) {
                Text(rotuloDesfazer, color = MaterialTheme.colorScheme.inversePrimary)
            }
        }
    }
}
