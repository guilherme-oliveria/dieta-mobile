package org.br.dietamais.ui.comum

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import org.br.dietamais.ui.comum.icones.IconesDieta
import org.br.dietamais.ui.theme.Dimens

/**
 * Barra superior do padrão de design (padrao-ui.md): seta de voltar num círculo contornado, título e subtítulo.
 * [descricaoVoltar] é o texto lido pelo leitor de tela na seta.
 */
@Composable
fun BarraTopo(titulo: String, aoVoltar: (() -> Unit)?, descricaoVoltar: String, modifier: Modifier = Modifier, subtitulo: String? = null) {
    Column(modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = Dimens.paddingCard, vertical = Dimens.espacoPequeno).heightIn(min = Dimens.alvoToque),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
            if (aoVoltar != null) Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface, border = BorderStroke(Dimens.borda, MaterialTheme.colorScheme.outline)) {
                IconButton(aoVoltar, Modifier.size(Dimens.alvoToque), colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)) {
                    Icon(IconesDieta.Voltar, contentDescription = descricaoVoltar)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() })
                if (subtitulo != null) Text(subtitulo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/** Barra de ações fixa no pé da tela: botão principal ocupando o resto, secundário contornado à esquerda. */
@Composable
fun RodapeAcoes(modifier: Modifier = Modifier, conteudo: @Composable RowScope.() -> Unit) {
    Column(modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).navigationBarsPadding().imePadding()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(Modifier.fillMaxWidth().padding(horizontal = Dimens.paddingCard, vertical = Dimens.espacoMedio),
            horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno), verticalAlignment = Alignment.CenterVertically, content = conteudo)
    }
}

/** Separa "Peso (kg)" em rótulo "Peso" e unidade "kg", para a unidade ficar dentro do campo (padrão de design). */
fun separarUnidade(rotulo: String): Pair<String, String?> {
    val m = Regex("^(.*\\S)\\s*\\(([^()]{1,6})\\)$").find(rotulo) ?: return rotulo to null
    if ('/' in m.groupValues[2] && m.groupValues[2].length <= 3 && m.groupValues[2].all { it.isUpperCase() || it == '/' }) return rotulo to null
    return m.groupValues[1] to m.groupValues[2]
}
