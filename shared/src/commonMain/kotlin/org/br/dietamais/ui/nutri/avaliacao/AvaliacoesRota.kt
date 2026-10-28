package org.br.dietamais.ui.nutri.avaliacao

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlin.uuid.Uuid

/** Aba Avaliações da ficha da nutri: histórico ou, com o formulário aberto, a tela de edição. */
@Composable
fun AvaliacoesNutriRota(viewModel: AvaliacoesViewModel, pacienteId: Uuid) {
    val estado by viewModel.estado.collectAsState()
    LaunchedEffect(pacienteId) { viewModel.abrir(pacienteId) }
    if (estado.formulario != null) AvaliacaoTela(estado, AcoesAvaliacao(viewModel::mudarMedida, viewModel::mudarParecer, viewModel::mudarCompartilhada,
        viewModel::mudarJustificativa, viewModel::salvar, viewModel::fechar))
    else HistoricoAvaliacoesTela(estado, AcoesHistorico(viewModel::nova, viewModel::editar, viewModel::alternarSelecao, viewModel::comparar,
        viewModel::voltarDaComparacao, viewModel::recarregar))
}

/** Lista somente leitura do paciente (avaliações compartilhadas, vindas do sync). */
@Composable
fun AvaliacoesPacienteRota(viewModel: AvaliacoesViewModel) {
    val estado by viewModel.estado.collectAsState()
    LaunchedEffect(Unit) { viewModel.abrirComoPaciente() }
    HistoricoAvaliacoesTela(estado, AcoesHistorico(aoSelecionar = viewModel::alternarSelecao, aoComparar = viewModel::comparar, aoVoltarComparacao = viewModel::voltarDaComparacao),
        podeEditar = false)
}
