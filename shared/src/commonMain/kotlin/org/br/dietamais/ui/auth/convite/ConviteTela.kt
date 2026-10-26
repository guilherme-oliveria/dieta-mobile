package org.br.dietamais.ui.auth.convite

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.KeyboardType
import org.br.dietamais.dominio.contas.permiteCadastro
import org.br.dietamais.data.remoto.dto.UsuarioResposta
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.auth.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

@Composable
fun ConviteTela(estado: EstadoConvite, usuario: UsuarioResposta?, aoCodigo: (String) -> Unit, aoConsultar: () -> Unit,
    aoConsentimento: (Boolean) -> Unit, aoEntrar: () -> Unit, aoCadastro: () -> Unit,
    aoCampo: (CampoCadastroPaciente, String) -> Unit, aoAceitar: () -> Unit, aoFechar: () -> Unit, autenticando: Boolean = false,
    aoTermosUso: (Boolean) -> Unit = {}, aoAtualizarTermos: () -> Unit = {}) {
    LayoutAutenticacao(stringResource(if (estado.cadastro) Res.string.convite_cadastro_titulo else Res.string.convite_titulo),
        stringResource(Res.string.convite_descricao), EstadoAutenticacao(restaurando = false)) {
        estado.erro?.let { mensagem -> Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }) {
            Text(textoMensagemConvite(mensagem), Modifier.padding(Dimens.paddingCampo), color = MaterialTheme.colorScheme.onErrorContainer)
        } }
        val convite = estado.convite
        if (convite == null) {
            CampoTexto(stringResource(Res.string.convite_codigo), estado.codigo, aoCodigo, habilitado = !estado.carregando)
            BotaoPrincipal(stringResource(Res.string.convite_consultar), aoConsultar, Modifier.fillMaxWidth(), carregando = estado.carregando)
        } else {
            Text(convite.nutricionista.nome, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            Text(convite.nutricionista.crn, style = MaterialTheme.typography.bodyMedium)
            BotaoTexto(stringResource(Res.string.convite_atualizar), aoConsultar, Modifier.fillMaxWidth(), habilitado = !estado.carregando)
            Text(stringResource(Res.string.convite_consentimento_titulo), style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() })
            Text(stringResource(Res.string.auth_versao_termos, convite.termo.versao), style = MaterialTheme.typography.bodySmall)
            CardDieta(Modifier.fillMaxWidth()) { SelectionContainer { Text(convite.termo.texto, style = MaterialTheme.typography.bodyMedium) } }
            Row(Modifier.fillMaxWidth().toggleable(estado.aceitouConsentimento, enabled = !estado.carregando && !autenticando,
                role = Role.Checkbox, onValueChange = aoConsentimento).heightIn(min = Dimens.alvoToque)
                .padding(vertical = Dimens.espacoPequeno), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                Checkbox(estado.aceitouConsentimento, onCheckedChange = null)
                Text(stringResource(Res.string.convite_aceitar_consentimento), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            }
            if (usuario != null) {
                Text(stringResource(Res.string.convite_conta_atual, usuario.nome, usuario.email), style = MaterialTheme.typography.bodySmall)
                BotaoPrincipal(stringResource(Res.string.convite_aceitar), aoAceitar, Modifier.fillMaxWidth(),
                    habilitado = estado.aceitouConsentimento && !autenticando, carregando = estado.aceitando)
                BotaoTexto(stringResource(Res.string.convite_outra_conta), aoEntrar, Modifier.fillMaxWidth(), habilitado = !estado.carregando && !autenticando)
            } else if (estado.cadastro) {
                val habilitado = estado.termosUso?.permiteCadastro() == true
                CampoTexto(stringResource(Res.string.auth_nome), estado.nome, { aoCampo(CampoCadastroPaciente.NOME, it) },
                    erro = estado.errosCampos["nome"]?.let { textoMensagemConvite(it) }, habilitado = habilitado && !estado.carregando)
                CampoTexto(stringResource(Res.string.auth_email), estado.email, { aoCampo(CampoCadastroPaciente.EMAIL, it) },
                    erro = estado.errosCampos["email"]?.let { textoMensagemConvite(it) }, habilitado = habilitado && !estado.carregando,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                CampoSenha(stringResource(Res.string.auth_senha), estado.senha, { aoCampo(CampoCadastroPaciente.SENHA, it) },
                    estado.errosCampos["senha"]?.let { textoMensagemConvite(it) }, habilitado && !estado.carregando, aoAceitar)
                Text(stringResource(Res.string.auth_senha_ajuda), style = MaterialTheme.typography.bodySmall)
                TermosUsoParte(estado.termosUso, estado.aceitouTermosUso, estado.carregando, aoTermosUso, aoAtualizarTermos)
                BotaoPrincipal(stringResource(Res.string.convite_criar_aceitar), aoAceitar, Modifier.fillMaxWidth(),
                    habilitado = habilitado && estado.aceitouConsentimento && estado.aceitouTermosUso && !autenticando, carregando = estado.aceitando)
                BotaoContornado(stringResource(Res.string.convite_entrar), aoEntrar, Modifier.fillMaxWidth(), habilitado = !autenticando)
            } else {
                Text(stringResource(Res.string.convite_login_ajuda), style = MaterialTheme.typography.bodyMedium)
                BotaoPrincipal(stringResource(Res.string.convite_entrar), aoEntrar, Modifier.fillMaxWidth(), habilitado = !autenticando)
                BotaoContornado(stringResource(Res.string.convite_criar_conta), aoCadastro, Modifier.fillMaxWidth(), habilitado = !autenticando)
            }
        }
        BotaoTexto(stringResource(Res.string.auth_voltar), aoFechar, Modifier.fillMaxWidth(), habilitado = !estado.aceitando)
    }
}
