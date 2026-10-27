package org.br.dietamais.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import org.br.dietamais.app.NavegacaoApp
import org.br.dietamais.app.AmbienteDieta
import org.br.dietamais.app.PerfilNavegacao
import org.br.dietamais.data.remoto.dto.Perfil
import org.br.dietamais.data.remoto.dto.StatusAssinatura
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.theme.Dimens
import org.br.dietamais.ui.nutri.pacientes.PacientesViewModel
import org.br.dietamais.ui.nutri.pacientes.PacientesRota
import org.br.dietamais.ui.auth.convite.ConviteViewModel
import org.br.dietamais.ui.auth.convite.ConviteTela
import org.br.dietamais.plataforma.LinksConvite
import org.br.dietamais.data.sync.EstadoLeituraSync
import org.br.dietamais.data.sync.ResultadoLeituraSync
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
@OptIn(ExperimentalComposeUiApi::class)
@Suppress("DEPRECATION")
fun FluxoAutenticacao(viewModel: AutenticacaoViewModel = koinViewModel(), convites: ConviteViewModel = koinViewModel(), ambiente: AmbienteDieta? = null) {
    val estado by viewModel.estado.collectAsState()
    val convite by convites.estado.collectAsState()
    val link by LinksConvite.pendente.collectAsState()
    val usuario = estado.usuario
    LaunchedEffect(usuario?.id, usuario?.perfil) { convites.sessaoMudou(usuario) }
    LaunchedEffect(convite.concluido) {
        if (convite.concluido) { viewModel.recarregarSessaoDoConvite(); convites.consumirConclusao() }
    }
    LaunchedEffect(link?.id, convite.aceitando) {
        link?.let { if (convites.abrir(it.link)) LinksConvite.consumir(it.id) }
    }
    BackHandler(enabled = convite.visivel) { if (!convite.aceitando) convites.fechar() }
    BackHandler(enabled = !convite.visivel && usuario == null && estado.tela != TelaAutenticacao.ENTRAR) { viewModel.voltar() }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when {
            convite.visivel -> ConviteTela(convite, usuario, convites::mudarCodigo, convites::consultar,
                convites::aceitarConsentimento, { convites.aguardarLogin(); if (usuario != null) viewModel.sair() else viewModel.voltar() },
                convites::abrirCadastro, convites::mudarCampo, convites::aceitar, convites::fechar,
                autenticando = estado.carregando || estado.restaurando, aoTermosUso = convites::aceitarTermosUso,
                aoAtualizarTermos = convites::atualizarTermos)
            estado.restaurando -> EstadoCarregando()
            usuario != null -> Column(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                    .heightIn(max = Dimens.alturaCabecalhoAutenticado).verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.margemCompacta)) {
                    estado.erro?.let { MensagemAutenticacaoTexto(it, alerta = true) }
                    if (!usuario.emailConfirmado) Text(stringResource(Res.string.auth_email_nao_confirmado),
                        color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                    estado.assinatura?.takeIf { it.status == StatusAssinatura.TESTE }?.let {
                        Text(stringResource(Res.string.auth_assinatura_teste, it.limitePacientes, it.fimPeriodoEm.toString().substringBefore('T')),
                            style = MaterialTheme.typography.bodySmall)
                    }
                    BotaoTexto(stringResource(Res.string.auth_sair), viewModel::sair, carregando = estado.carregando)
                    BotaoTexto(stringResource(Res.string.auth_tenho_codigo), { convites.abrir() }, habilitado = !estado.carregando)
                    if (ambiente != null) key(usuario.id, usuario.perfil) { IndicadorAmbiente(ambiente) }
                }
                Box(Modifier.weight(1f)) {
                    key(usuario.id, usuario.perfil) {
                        if (usuario.perfil == Perfil.NUTRICIONISTA) NavegacaoNutri()
                        else NavegacaoApp(PerfilNavegacao.PACIENTE)
                    }
                }
            }
            else -> when (estado.tela) {
                TelaAutenticacao.ENTRAR -> EntrarTela(estado, viewModel::mudarEmail, viewModel::mudarSenha,
                    viewModel::entrar, viewModel::abrirCadastro, viewModel::abrirRecuperacao, convites::retomar)
                TelaAutenticacao.CADASTRO -> CadastroNutriTela(estado, viewModel.termos, viewModel::mudarNome, viewModel::mudarCrn,
                    viewModel::mudarEmail, viewModel::mudarSenha, viewModel::aceitarTermos, viewModel::cadastrar, viewModel::voltar)
                TelaAutenticacao.RECUPERAR_SENHA -> RecuperarSenhaTela(estado, viewModel::mudarEmail, viewModel::mudarToken,
                    viewModel::mudarSenha, viewModel::recuperarSenha, viewModel::redefinirSenha, viewModel::voltar)
                TelaAutenticacao.ESCOLHER_PERFIL -> EscolherPerfilTela(estado, viewModel::escolherPerfil, viewModel::voltar)
            }
        }
    }
}

@Composable
private fun IndicadorAmbiente(ambiente: AmbienteDieta) {
    val conexao by ambiente.conectividade.estado.collectAsState()
    val estado by ambiente.sincronizador.estado.collectAsState()
    val escopo = rememberCoroutineScope()
    var falhaLocal by remember { mutableStateOf(false) }
    LaunchedEffect(estado.resultado) { if (estado.resultado == ResultadoLeituraSync.CONCLUIDO) falhaLocal = false }
    IndicadorLeitura(if (falhaLocal) EstadoLeituraSync(ResultadoLeituraSync.FALHOU) else estado, conexao) {
        falhaLocal = false
        escopo.launch {
            try { ambiente.sincronizarAgora(); falhaLocal = false }
            catch (cancelamento: CancellationException) { throw cancelamento }
            catch (_: Exception) { falhaLocal = true }
        }
    }
}

@Composable
private fun NavegacaoNutri() {
    val dono = remember { object : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() } }
    DisposableEffect(dono) { onDispose { dono.viewModelStore.clear() } }
    val pacientes = koinViewModel<PacientesViewModel>(viewModelStoreOwner = dono)
    NavegacaoApp(PerfilNavegacao.NUTRICIONISTA, conteudoPacientes = { classe, vinculo, abrir, voltar ->
        PacientesRota(pacientes, classe, vinculo, abrir, voltar)
    })
}
