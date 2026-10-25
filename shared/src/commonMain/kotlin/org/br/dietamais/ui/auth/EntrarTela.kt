package org.br.dietamais.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.*
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.comum.icones.IconesDieta
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

@Composable
fun EntrarTela(estado: EstadoAutenticacao, aoMudarEmail: (String) -> Unit, aoMudarSenha: (String) -> Unit,
    aoEntrar: () -> Unit, aoCadastrar: () -> Unit, aoRecuperar: () -> Unit) {
    var mostrarAvisoConvite by remember { mutableStateOf(false) }
    LayoutAutenticacao(stringResource(Res.string.app_nome), stringResource(Res.string.auth_apresentacao), estado, marca = true) {
        CampoTexto(stringResource(Res.string.auth_email), estado.email, aoMudarEmail, erro = estado.erroCampo("email"), habilitado = !estado.carregando,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next))
        CampoSenha(stringResource(Res.string.auth_senha), estado.senha, aoMudarSenha, estado.erroCampo("senha"), !estado.carregando, aoEntrar)
        BotaoTexto(stringResource(Res.string.auth_esqueci_senha), aoRecuperar, Modifier.fillMaxWidth(), habilitado = !estado.carregando)
        BotaoPrincipal(stringResource(Res.string.entrar), aoEntrar, Modifier.fillMaxWidth(), carregando = estado.carregando)
        HorizontalDivider()
        Text(stringResource(Res.string.auth_primeira_vez), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        BotaoContornado(stringResource(Res.string.auth_tenho_codigo), { mostrarAvisoConvite = true }, Modifier.fillMaxWidth(), habilitado = !estado.carregando)
        if (mostrarAvisoConvite) Text(stringResource(Res.string.auth_convite_depois), style = MaterialTheme.typography.bodySmall)
        BotaoContornado(stringResource(Res.string.auth_sou_nutri), aoCadastrar, Modifier.fillMaxWidth(), habilitado = !estado.carregando)
        Text(stringResource(Res.string.auth_privacidade), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
