package org.br.dietamais.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.*
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun RecuperarSenhaTela(estado: EstadoAutenticacao, aoEmail: (String) -> Unit, aoToken: (String) -> Unit,
    aoSenha: (String) -> Unit, aoEnviar: () -> Unit, aoRedefinir: () -> Unit, aoVoltar: () -> Unit) {
    var temToken by remember { mutableStateOf(false) }
    LayoutAutenticacao(stringResource(Res.string.auth_recuperar_titulo), stringResource(Res.string.auth_recuperar_descricao), estado) {
        CampoTexto(stringResource(Res.string.auth_email), estado.email, aoEmail, erro = estado.erroCampo("email"), habilitado = !estado.carregando,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        BotaoPrincipal(stringResource(Res.string.auth_enviar_link), aoEnviar, Modifier.fillMaxWidth(), carregando = estado.carregando)
        BotaoTexto(stringResource(Res.string.auth_ja_tem_token), { temToken = true }, Modifier.fillMaxWidth(), habilitado = !estado.carregando)
        if (temToken) {
            CampoTexto(stringResource(Res.string.auth_token), estado.tokenRecuperacao, aoToken, erro = estado.erroCampo("token"), habilitado = !estado.carregando,
                visualTransformation = PasswordVisualTransformation())
            Text(stringResource(Res.string.auth_token_ajuda), style = MaterialTheme.typography.bodySmall)
            CampoSenha(stringResource(Res.string.auth_nova_senha), estado.senha, aoSenha, estado.erroCampo("senha"), !estado.carregando, aoRedefinir)
            BotaoPrincipal(stringResource(Res.string.auth_redefinir), aoRedefinir, Modifier.fillMaxWidth(), carregando = estado.carregando)
        }
        BotaoTexto(stringResource(Res.string.auth_voltar), aoVoltar, Modifier.fillMaxWidth())
    }
}
