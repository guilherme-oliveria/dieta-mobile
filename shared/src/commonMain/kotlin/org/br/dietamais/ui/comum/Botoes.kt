package org.br.dietamais.ui.comum

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import org.br.dietamais.recursos.Res
import org.br.dietamais.recursos.carregando
import org.br.dietamais.ui.theme.Dimens
import org.br.dietamais.ui.theme.FormasDieta
import org.jetbrains.compose.resources.stringResource

@Composable
fun BotaoPrincipal(
    texto: String,
    aoClicar: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    carregando: Boolean = false,
    cores: ButtonColors = ButtonDefaults.buttonColors(),
) {
    val fonte = remember { MutableInteractionSource() }
    Button(
        onClick = aoClicar, enabled = habilitado && !carregando,
        modifier = modifier.anelDeFoco(fonte, MaterialTheme.colorScheme.onBackground).botaoDieta(carregando), shape = FormasDieta.botao, interactionSource = fonte,
        colors = cores, contentPadding = paddingBotao(),
    ) { ConteudoBotao(texto, carregando) }
}

@Composable
fun BotaoContornado(
    texto: String,
    aoClicar: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    carregando: Boolean = false,
    cores: ButtonColors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
    borda: BorderStroke = BorderStroke(Dimens.borda, MaterialTheme.colorScheme.outline),
) {
    val fonte = remember { MutableInteractionSource() }
    OutlinedButton(
        onClick = aoClicar, enabled = habilitado && !carregando,
        modifier = modifier.anelDeFoco(fonte, MaterialTheme.colorScheme.primary).botaoDieta(carregando), shape = FormasDieta.botao, interactionSource = fonte,
        colors = cores, border = borda, contentPadding = paddingBotao(),
    ) { ConteudoBotao(texto, carregando) }
}

@Composable
fun BotaoTexto(
    texto: String,
    aoClicar: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    carregando: Boolean = false,
    /** Opção escolhida de um grupo (dia, período): fundo verde claro e `selected` na acessibilidade, em vez de parecer desabilitada. */
    selecionado: Boolean = false,
) {
    val fonte = remember { MutableInteractionSource() }
    TextButton(
        onClick = aoClicar, enabled = habilitado && !carregando,
        modifier = modifier.anelDeFoco(fonte, MaterialTheme.colorScheme.primary).botaoDieta(carregando).semantics { selected = selecionado },
        shape = FormasDieta.botao, interactionSource = fonte,
        colors = if (selecionado) ButtonDefaults.textButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) else ButtonDefaults.textButtonColors(),
        contentPadding = paddingBotao(),
    ) { ConteudoBotao(texto, carregando) }
}

@Composable
private fun Modifier.botaoDieta(carregando: Boolean): Modifier {
    val estado = stringResource(Res.string.carregando)
    return heightIn(min = Dimens.alturaBotao).minimumInteractiveComponentSize().semantics {
        if (carregando) stateDescription = estado
    }
}

private fun paddingBotao() = PaddingValues(horizontal = Dimens.paddingCampo, vertical = Dimens.espacoMedio)

@Composable
private fun ConteudoBotao(texto: String, carregando: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno), verticalAlignment = Alignment.CenterVertically) {
        if (carregando) {
            CircularProgressIndicator(
                modifier = Modifier.size(Dimens.iconePequeno),
                strokeWidth = Dimens.progressoTraco,
                color = androidx.compose.material3.LocalContentColor.current,
            )
        }
        Text(texto, style = MaterialTheme.typography.labelLarge)
    }
}

/** Contorno de 2 dp quando o botão recebe o foco do teclado (WCAG 2.4.7; `plataformas.md`). O véu do M3 sozinho é fraco demais. */
@Composable
private fun Modifier.anelDeFoco(fonte: MutableInteractionSource, cor: Color): Modifier {
    val focado by fonte.collectIsFocusedAsState()
    return if (focado) border(Dimens.anelFoco, cor, FormasDieta.botao) else this
}
