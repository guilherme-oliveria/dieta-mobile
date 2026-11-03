package org.br.dietamais.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.*
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

@Composable
fun CadastroNutriTela(estado: EstadoAutenticacao, termos: TermosCadastro?, aoNome: (String) -> Unit, aoCrn: (String) -> Unit,
    aoEmail: (String) -> Unit, aoSenha: (String) -> Unit, aoAceitar: (Boolean) -> Unit, aoCadastrar: () -> Unit, aoVoltar: () -> Unit) {
    var lerTermos by remember { mutableStateOf(false) }
    LayoutAutenticacao(stringResource(Res.string.auth_cadastro_titulo), stringResource(Res.string.auth_cadastro_descricao), estado) {
        CampoTexto(stringResource(Res.string.auth_nome), estado.nome, aoNome, erro = estado.erroCampo("nome"), habilitado = !estado.carregando,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next))
        CampoTexto(stringResource(Res.string.auth_crn), estado.crn, aoCrn, erro = estado.erroCampo("crn"), habilitado = !estado.carregando)
        Text(stringResource(Res.string.auth_crn_ajuda), style = MaterialTheme.typography.bodySmall)
        CampoTexto(stringResource(Res.string.auth_email), estado.email, aoEmail, erro = estado.erroCampo("email"), habilitado = !estado.carregando,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next))
        CampoSenha(stringResource(Res.string.auth_senha), estado.senha, aoSenha, estado.erroCampo("senha"), !estado.carregando, aoCadastrar)
        Text(stringResource(Res.string.auth_senha_ajuda), style = MaterialTheme.typography.bodySmall)
        if (termos == null) MensagemAutenticacaoTexto(MensagemAutenticacao.TERMOS_INDISPONIVEIS, alerta = true)
        else {
            BotaoTexto(stringResource(Res.string.auth_ler_termos), { lerTermos = true }, Modifier.fillMaxWidth(), habilitado = !estado.carregando)
            Row(Modifier.fillMaxWidth().toggleable(estado.aceitouTermos, enabled = !estado.carregando, role = Role.Checkbox, onValueChange = aoAceitar)
                .heightIn(min = Dimens.alvoToque).padding(vertical = Dimens.espacoPequeno), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                Checkbox(estado.aceitouTermos, onCheckedChange = null)
                Text(stringResource(Res.string.auth_aceitar_termos), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            }
            estado.erroCampo("aceitouTermos")?.let { Text(it, color = MaterialTheme.colorScheme.onErrorContainer) }
        }
        BotaoPrincipal(stringResource(Res.string.auth_criar_conta), aoCadastrar, Modifier.fillMaxWidth(), habilitado = termos != null, carregando = estado.carregando)
        BotaoTexto(stringResource(Res.string.auth_voltar), aoVoltar, Modifier.fillMaxWidth())
    }
    if (lerTermos && termos != null) AlertDialog(onDismissRequest = { lerTermos = false },
        title = { Text(stringResource(Res.string.auth_termos_titulo)) },
        text = { Column(Modifier.heightIn(max = Dimens.alturaTermos).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
            Text(stringResource(Res.string.auth_versao_termos, termos.versao)); Text(termos.texto)
        } }, confirmButton = { BotaoTexto(stringResource(Res.string.auth_fechar), { lerTermos = false }) })
}
