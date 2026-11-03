package org.br.dietamais.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.*
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.theme.Dimens
import org.br.dietamais.ui.comum.icones.IconesDieta
import org.br.dietamais.ui.theme.FormasDieta
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LayoutAutenticacao(titulo: String, descricao: String, estado: EstadoAutenticacao, marca: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding().verticalScroll(rememberScrollState()), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = Dimens.larguraFormulario).fillMaxWidth().padding(Dimens.margemCompacta)
            .padding(vertical = Dimens.espacoEntrada), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
            if (marca) Surface(color = MaterialTheme.colorScheme.primary, shape = FormasDieta.card, modifier = Modifier.size(Dimens.marcaEntrada)) {
                Box(contentAlignment = Alignment.Center) { Icon(IconesDieta.Folha, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(Dimens.iconeEstado)) }
            }
            Text(titulo, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
            Text(descricao, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            estado.aviso?.let { MensagemAutenticacaoTexto(it) }
            estado.erro?.let { MensagemAutenticacaoTexto(it, alerta = true) }
            content()
        }
    }
    }
}

@Composable
internal fun CampoSenha(rotulo: String, valor: String, aoMudar: (String) -> Unit, erro: String?, habilitado: Boolean, aoEnviar: () -> Unit) {
    var visivel by remember { mutableStateOf(false) }
    CampoTexto(rotulo, valor, aoMudar, erro = erro, habilitado = habilitado,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        visualTransformation = if (visivel) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardActions = KeyboardActions(onDone = { aoEnviar() }),
        trailingIcon = { IconButton(onClick = { visivel = !visivel }, enabled = habilitado) {
            Icon(if (visivel) IconesDieta.OlhoFechado else IconesDieta.Olho,
                contentDescription = stringResource(if (visivel) Res.string.auth_ocultar_senha else Res.string.auth_mostrar_senha))
        } })
}

@Composable
internal fun MensagemAutenticacaoTexto(mensagem: MensagemAutenticacao, alerta: Boolean = false) {
    Surface(color = if (alerta) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer,
        shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }) {
        Text(textoMensagem(mensagem), Modifier.padding(Dimens.paddingCampo), color = if (alerta) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer,
            style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
internal fun textoMensagem(mensagem: MensagemAutenticacao): String = stringResource(when (mensagem) {
    MensagemAutenticacao.OBRIGATORIO -> Res.string.auth_erro_obrigatorio
    MensagemAutenticacao.EMAIL_INVALIDO -> Res.string.auth_erro_email
    MensagemAutenticacao.SENHA_CURTA -> Res.string.auth_erro_senha
    MensagemAutenticacao.CRN_INVALIDO -> Res.string.auth_erro_crn
    MensagemAutenticacao.ACEITE_OBRIGATORIO -> Res.string.auth_erro_aceite
    MensagemAutenticacao.TERMOS_INDISPONIVEIS -> Res.string.auth_termos_indisponiveis
    MensagemAutenticacao.CREDENCIAIS_INVALIDAS -> Res.string.auth_erro_credenciais
    MensagemAutenticacao.LOGIN_BLOQUEADO -> Res.string.auth_erro_bloqueado
    MensagemAutenticacao.TAXA_EXCEDIDA -> Res.string.auth_erro_taxa
    MensagemAutenticacao.EMAIL_JA_CADASTRADO -> Res.string.auth_erro_email_cadastrado
    MensagemAutenticacao.PERFIL_INVALIDO -> Res.string.auth_erro_perfil
    MensagemAutenticacao.TOKEN_INVALIDO -> Res.string.auth_erro_token
    MensagemAutenticacao.CAMPO_INVALIDO -> Res.string.auth_erro_campo
    MensagemAutenticacao.SEM_CONEXAO -> Res.string.auth_erro_rede
    MensagemAutenticacao.COFRE_INDISPONIVEL -> Res.string.auth_erro_cofre
    MensagemAutenticacao.RESPOSTA_INVALIDA -> Res.string.auth_erro_resposta
    MensagemAutenticacao.ERRO_INESPERADO -> Res.string.auth_erro_generico
    MensagemAutenticacao.CADASTRO_CONCLUIDO -> Res.string.auth_cadastro_concluido
    MensagemAutenticacao.RECUPERACAO_ENVIADA -> Res.string.auth_recuperacao_enviada
    MensagemAutenticacao.SENHA_REDEFINIDA -> Res.string.auth_senha_redefinida
})

@Composable
internal fun EstadoAutenticacao.erroCampo(campo: String) = errosCampos[campo]?.let { textoMensagem(it) }
