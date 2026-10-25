package org.br.dietamais.ui.comum

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import org.br.dietamais.recursos.Res
import org.br.dietamais.recursos.app_nome
import org.br.dietamais.recursos.navegacao_principal
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

data class ItemNavegacao(val chave: String, val rotulo: String, val icone: ImageVector)

@Composable
fun BarraNavegacaoInferior(
    itens: List<ItemNavegacao>,
    selecionada: String?,
    aoSelecionar: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val descricao = stringResource(Res.string.navegacao_principal)
    val borda = MaterialTheme.colorScheme.outlineVariant
    val cores = MaterialTheme.colorScheme
    val escalaFonte = LocalDensity.current.fontScale.coerceAtLeast(1f)
    NavigationBar(
        modifier = modifier.heightIn(min = Dimens.alturaBarraNavegacao * escalaFonte)
            .semantics { contentDescription = descricao }.drawBehind {
            drawLine(borda, Offset.Zero, Offset(size.width, 0f), Dimens.borda.toPx())
        },
        containerColor = MaterialTheme.colorScheme.surface, tonalElevation = Dimens.nenhum,
    ) {
        itens.forEach { item ->
            NavigationBarItem(
                selected = item.chave == selecionada, onClick = { aoSelecionar(item.chave) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = cores.primary, selectedTextColor = cores.primary,
                    indicatorColor = cores.primaryContainer,
                    unselectedIconColor = cores.onSurfaceVariant, unselectedTextColor = cores.onSurfaceVariant,
                ),
                icon = { Icon(item.icone, contentDescription = null) },
                label = { Text(item.rotulo, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center) },
            )
        }
    }
}

@Composable
fun TrilhoNavegacao(
    itens: List<ItemNavegacao>,
    selecionada: String?,
    aoSelecionar: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val descricao = stringResource(Res.string.navegacao_principal)
    val borda = MaterialTheme.colorScheme.outlineVariant
    val cores = MaterialTheme.colorScheme
    val escalaFonte = LocalDensity.current.fontScale.coerceAtLeast(1f)
    NavigationRail(
        modifier = modifier.fillMaxHeight().width(Dimens.larguraTrilho * escalaFonte)
            .semantics { contentDescription = descricao }.drawBehind {
            drawLine(borda, Offset(size.width, 0f), Offset(size.width, size.height), Dimens.borda.toPx())
        },
        containerColor = MaterialTheme.colorScheme.surface,
        header = {
            Text(
                stringResource(Res.string.app_nome), color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(Dimens.paddingCard),
            )
        },
    ) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            itens.forEach { item ->
                NavigationRailItem(
                    selected = item.chave == selecionada, onClick = { aoSelecionar(item.chave) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = Dimens.alturaItemTrilho * escalaFonte),
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = cores.primary, selectedTextColor = cores.primary,
                        indicatorColor = cores.primaryContainer,
                        unselectedIconColor = cores.onSurfaceVariant, unselectedTextColor = cores.onSurfaceVariant,
                    ),
                    icon = { Icon(item.icone, contentDescription = null) },
                    label = { Text(item.rotulo, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center) },
                )
            }
        }
    }
}
