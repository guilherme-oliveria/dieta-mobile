package org.br.dietamais.ui.comum

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.icones.IconesDieta
import org.br.dietamais.ui.theme.Dimens
import org.br.dietamais.ui.theme.FormasDieta
import org.jetbrains.compose.resources.stringResource

@Composable
fun EstadoVazio(
    modifier: Modifier = Modifier,
    mensagem: String = stringResource(Res.string.vazio_padrao),
    icone: ImageVector = IconesDieta.Lista,
    textoAcao: String? = null,
    aoAcionar: (() -> Unit)? = null,
) {
    EstadoMensagem(modifier, mensagem, icone, textoAcao, aoAcionar)
}

@Composable
fun EstadoErro(
    aoTentarNovamente: () -> Unit,
    modifier: Modifier = Modifier,
    mensagem: String = stringResource(Res.string.erro_padrao),
) {
    EstadoMensagem(
        modifier, mensagem, IconesDieta.Alerta,
        stringResource(Res.string.tentar_novamente), aoTentarNovamente, alerta = true,
    )
}

@Composable
private fun EstadoMensagem(
    modifier: Modifier,
    mensagem: String,
    icone: ImageVector,
    textoAcao: String?,
    aoAcionar: (() -> Unit)?,
    alerta: Boolean = false,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(Dimens.paddingCard).semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio),
    ) {
        Icon(
            icone, contentDescription = null, modifier = Modifier.size(Dimens.iconeEstado),
            tint = if (alerta) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            mensagem, style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = if (alerta) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (textoAcao != null && aoAcionar != null) BotaoTexto(textoAcao, aoAcionar)
    }
}

@Composable
fun EstadoCarregando(modifier: Modifier = Modifier, mensagem: String = stringResource(Res.string.carregando)) {
    Column(
        modifier = modifier.fillMaxWidth().padding(Dimens.paddingCard).semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio),
    ) {
        CircularProgressIndicator(modifier = Modifier.size(Dimens.iconeEstado), strokeWidth = Dimens.progressoTraco)
        Text(mensagem, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
    }
}

@Composable
fun FaixaOffline(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = FormasDieta.campo,
    ) {
        Row(
            modifier = Modifier.padding(Dimens.paddingCampo),
            horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(IconesDieta.SemInternet, contentDescription = null, modifier = Modifier.size(Dimens.iconePequeno))
            Text(stringResource(Res.string.offline_mensagem), style = MaterialTheme.typography.bodySmall)
        }
    }
}
