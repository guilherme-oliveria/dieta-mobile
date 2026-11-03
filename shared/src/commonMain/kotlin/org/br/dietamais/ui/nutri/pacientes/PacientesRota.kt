package org.br.dietamais.ui.nutri.pacientes

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.window.core.layout.WindowSizeClass
import org.br.dietamais.data.remoto.dto.PacienteResposta
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.nutri.paciente.PacienteFichaTela
import org.jetbrains.compose.resources.stringResource
import kotlin.uuid.Uuid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PacientesRota(viewModel: PacientesViewModel, classe: WindowSizeClass, vinculoId: String?,
    aoAbrir: (String, String) -> Unit, aoVoltar: () -> Unit,
    abaConteudo: @Composable (AbaFicha, PacienteResposta) -> Unit = { _, _ -> }) {
    val estado by viewModel.estado.collectAsState()
    LaunchedEffect(vinculoId) { viewModel.selecionar(vinculoId?.let(Uuid::parse)) }
    LayoutListaDetalhe(classe, mostrarDetalhe = vinculoId != null,
        lista = { PacientesTela(estado, viewModel::mudarBusca, viewModel::mudarFiltro, viewModel::abrirCriacao,
            viewModel::atualizar, { aoAbrir(it.id.toString(), it.vinculoId.toString()) }) },
        detalhe = {
            if (vinculoId != null) PacienteFichaTela(estado, viewModel::mudarAba, aoVoltar, viewModel::gerarConvite, abaConteudo)
            else EstadoVazio(mensagem = stringResource(Res.string.selecione_paciente))
        })
    if (estado.formulario != null) ModalBottomSheet(onDismissRequest = viewModel::fecharCriacao) {
        CriarPacienteFormulario(estado, viewModel::mudarCampo, viewModel::mudarSexo, viewModel::criar, viewModel::fecharCriacao, viewModel::atualizar)
    }
    if (estado.convite != null) ModalBottomSheet(onDismissRequest = viewModel::fecharConvite) {
        ConviteConteudo(estado, viewModel::compartilharConvite, viewModel::fecharConvite)
    }
    if (estado.limiteAtingido) FolhaLimitePlano(estado.assinatura, viewModel::fecharLimite)
}
