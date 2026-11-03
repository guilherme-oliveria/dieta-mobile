package org.br.dietamais.ui.nutri.dieta

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.PortaPdf
import org.br.dietamais.data.repositorio.RecursoPdf
import org.koin.compose.koinInject
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

/** Aba Dieta da ficha: lista de dietas do paciente e montador (MOB-12/13). */
@Composable
fun DietasPacienteRota(viewModel: MontadorDietaViewModel, paciente: PacienteResposta) {
    val estado by viewModel.estado.collectAsState()
    LaunchedEffect(paciente.id) { viewModel.abrirPaciente(paciente.id) }
    val pdf = koinInject<PortaPdf>()
    DietasListaTela(estado, viewModel::nova, viewModel::abrir, viewModel::duplicar, viewModel::recarregar,
        pdf = { d -> BotaoPdf(stringResource(Res.string.pdf_dieta), !estado.semConexao, { pdf.link(RecursoPdf.DIETA, d.id) }) })
    // tela cheia em diálogo: a ficha já rola na vertical, então o montador não pode aninhar outra rolagem
    if (estado.rascunho != null) Dialog(onDismissRequest = viewModel::fechar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        MontarDietaTela(estado, AcoesMontador(viewModel::fechar, viewModel::mudarTitulo, viewModel::mudarOrientacao, viewModel::adicionarRefeicao,
            viewModel::removerRefeicao, viewModel::moverRefeicao, viewModel::mudarNomeRefeicao, viewModel::mudarHorario, viewModel::abrirSeletor,
            viewModel::mudarBusca, viewModel::mudarGrupoFiltro, viewModel::adicionarItem, viewModel::removerItem, viewModel::mudarPorcoes,
            viewModel::salvar, viewModel::pedirPublicacao, viewModel::confirmarPublicacao, viewModel::cancelarPublicacao,
            { estado.dietas.firstOrNull { it.id == estado.rascunho?.dietaId }?.let(viewModel::duplicar); viewModel.fechar() }))
    }
}

@Composable
fun DietasListaTela(estado: EstadoDieta, aoNova: () -> Unit, aoAbrir: (DietaResposta) -> Unit, aoDuplicar: (DietaResposta) -> Unit, aoRecarregar: () -> Unit,
    pdf: @Composable (DietaResposta) -> Unit = {}) {
    val habilitado = !estado.semConexao && !estado.enviando && !estado.carregando
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
        if (estado.semConexao) FaixaOffline()
        estado.erro?.let { Text(textoMensagemDieta(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer) }
        estado.aviso?.let { Text(textoMensagemDieta(it), style = MaterialTheme.typography.bodySmall) }
        BotaoPrincipal(stringResource(Res.string.dieta_nova), aoNova, habilitado = habilitado && estado.alimentos.isNotEmpty())
        if (estado.alimentos.isEmpty()) Text(stringResource(Res.string.dieta_sem_alimentos), style = MaterialTheme.typography.bodySmall)
        if (estado.carregando) EstadoCarregando()
        else if (estado.dietas.isEmpty()) EstadoVazio(mensagem = stringResource(Res.string.dieta_vazia_lista))
        estado.dietas.forEach { dieta ->
            CardDieta(Modifier.fillMaxWidth()) {
                Text(dieta.titulo, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(textoStatus(dieta.status)), style = MaterialTheme.typography.labelLarge)
                Text(stringResource(Res.string.dieta_kcal, formatar(dieta.totais.kcal)), style = MaterialTheme.typography.bodyMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                    BotaoTexto(stringResource(if (dieta.status == StatusDieta.RASCUNHO) Res.string.dieta_editar else Res.string.dieta_ver_acao), { aoAbrir(dieta) })
                    BotaoTexto(stringResource(Res.string.dieta_duplicar), { aoDuplicar(dieta) }, habilitado = habilitado)
                    if (dieta.status == StatusDieta.PUBLICADA || dieta.status == StatusDieta.SUBSTITUIDA) pdf(dieta)
                }
            }
        }
        if (estado.erro != null && estado.dietas.isEmpty()) BotaoTexto(stringResource(Res.string.sync_atualizar), aoRecarregar)
    }
}
