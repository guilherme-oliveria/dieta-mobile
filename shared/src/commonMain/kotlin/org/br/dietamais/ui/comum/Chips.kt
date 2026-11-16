package org.br.dietamais.ui.comum

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.br.dietamais.ui.comum.icones.IconesDieta
import org.br.dietamais.ui.theme.Dimens

/**
 * Chip de escolha do padrão de design (padrao-ui.md, seção Escolhas): o selecionado ganha um check à esquerda, para não depender só da cor,
 * e o alvo de toque tem no mínimo [Dimens.alvoToque]. Mesma ordem de parâmetros do `FilterChip` do Material.
 */
@Composable
fun ChipEscolha(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FilterChip(
        selected = selected, onClick = onClick, label = label, enabled = enabled,
        modifier = modifier.heightIn(min = Dimens.alvoToque),
        leadingIcon = if (selected) {
            { Icon(IconesDieta.Check, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary) }
        } else null,
    )
}
