package org.br.dietamais.ui.nutri.avaliacao

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.br.dietamais.data.repositorio.PortaPdf
import org.br.dietamais.data.repositorio.RecursoPdf
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.BotaoPdf
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import kotlin.uuid.Uuid

/** Aba Avaliações da ficha da nutri: histórico ou, com o formulário aberto, a tela de edição. */
@Composable
fun AvaliacoesNutriRota(viewModel: AvaliacoesViewModel, pacienteId: Uuid) {
    val estado by viewModel.estado.collectAsState()
    LaunchedEffect(pacienteId) { viewModel.abrir(pacienteId) }
    val pdf = koinInject<PortaPdf>()
    if (estado.formulario != null) AvaliacaoTela(estado, AcoesAvaliacao(viewModel::mudarMedida, viewModel::mudarParecer, viewModel::mudarCompartilhada,
        viewModel::mudarJustificativa, viewModel::salvar, viewModel::fechar))
    else HistoricoAvaliacoesTela(estado, AcoesHistorico(viewModel::nova, viewModel::editar, viewModel::alternarSelecao, viewModel::comparar,
        viewModel::voltarDaComparacao, viewModel::recarregar,
        pdf = { a -> BotaoPdf(stringResource(Res.string.pdf_avaliacao), !estado.semConexao, { pdf.link(RecursoPdf.AVALIACAO, a.id) }) }))
}

/** Lista somente leitura do paciente (avaliações compartilhadas, vindas do sync). */
@Composable
fun AvaliacoesPacienteRota(viewModel: AvaliacoesViewModel) {
    val estado by viewModel.estado.collectAsState()
    LaunchedEffect(Unit) { viewModel.abrirComoPaciente() }
    val pdf = koinInject<PortaPdf>()
    HistoricoAvaliacoesTela(estado, AcoesHistorico(aoSelecionar = viewModel::alternarSelecao, aoComparar = viewModel::comparar, aoVoltarComparacao = viewModel::voltarDaComparacao,
        pdf = { a -> BotaoPdf(stringResource(Res.string.pdf_avaliacao), !estado.semConexao, { pdf.link(RecursoPdf.AVALIACAO, a.id) }) }),
        podeEditar = false)
}
