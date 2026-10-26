package org.br.dietamais.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import org.br.dietamais.app.NavegacaoApp
import org.br.dietamais.app.PerfilNavegacao
import org.br.dietamais.data.remoto.dto.Perfil
import org.br.dietamais.data.remoto.dto.StatusAssinatura
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
@OptIn(ExperimentalComposeUiApi::class)
@Suppress("DEPRECATION")
fun FluxoAutenticacao(viewModel: AutenticacaoViewModel = koinViewModel()) {
    val estado by viewModel.estado.collectAsState()
    val usuario = estado.usuario
    BackHandler(enabled = usuario == null && estado.tela != TelaAutenticacao.ENTRAR) { viewModel.voltar() }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when {
            estado.restaurando -> EstadoCarregando()
            usuario != null -> Column(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                    .padding(horizontal = Dimens.margemCompacta)) {
                    estado.erro?.let { MensagemAutenticacaoTexto(it, alerta = true) }
                    if (!usuario.emailConfirmado) Text(stringResource(Res.string.auth_email_nao_confirmado),
                        color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                    estado.assinatura?.takeIf { it.status == StatusAssinatura.TESTE }?.let {
                        Text(stringResource(Res.string.auth_assinatura_teste, it.limitePacientes, it.fimPeriodoEm.toString().substringBefore('T')),
                            style = MaterialTheme.typography.bodySmall)
                    }
                    BotaoTexto(stringResource(Res.string.auth_sair), viewModel::sair, carregando = estado.carregando)
                }
                Box(Modifier.weight(1f)) {
                    NavegacaoApp(when (usuario.perfil) {
                        Perfil.NUTRICIONISTA -> PerfilNavegacao.NUTRICIONISTA
                        Perfil.PACIENTE -> PerfilNavegacao.PACIENTE
                        else -> null
                    })
                }
            }
            else -> when (estado.tela) {
                TelaAutenticacao.ENTRAR -> EntrarTela(estado, viewModel::mudarEmail, viewModel::mudarSenha,
                    viewModel::entrar, viewModel::abrirCadastro, viewModel::abrirRecuperacao)
                TelaAutenticacao.CADASTRO -> CadastroNutriTela(estado, viewModel.termos, viewModel::mudarNome, viewModel::mudarCrn,
                    viewModel::mudarEmail, viewModel::mudarSenha, viewModel::aceitarTermos, viewModel::cadastrar, viewModel::voltar)
                TelaAutenticacao.RECUPERAR_SENHA -> RecuperarSenhaTela(estado, viewModel::mudarEmail, viewModel::mudarToken,
                    viewModel::mudarSenha, viewModel::recuperarSenha, viewModel::redefinirSenha, viewModel::voltar)
                TelaAutenticacao.ESCOLHER_PERFIL -> EscolherPerfilTela(estado, viewModel::escolherPerfil, viewModel::voltar)
            }
        }
    }
}
