package org.br.dietamais.ui.paciente.evolucao

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.nutri.avaliacao.AvaliacoesPacienteRota
import org.br.dietamais.ui.nutri.avaliacao.AvaliacoesViewModel
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource
import kotlin.uuid.Uuid

private fun acoes(vm: EvolucaoViewModel) = AcoesEvolucao(vm::escolherSerie, vm::escolherIntervalo, vm::abrirRegistroPeso, vm::mudarPeso, vm::registrarPeso, vm::fecharRegistroPeso)

/** Aba da ficha da nutri: gráficos de um paciente, só leitura. */
@Composable
fun EvolucaoRota(viewModel: EvolucaoViewModel, pacienteId: Uuid) {
    val estado by viewModel.estado.collectAsState()
    LaunchedEffect(pacienteId) { viewModel.abrir(pacienteId) }
    EvolucaoTela(estado, acoes(viewModel), rolavel = false) // dentro da ficha, que já rola
}

/** Aba Evolução do paciente: gráficos (com registro de peso) e, em outra guia, as avaliações compartilhadas. */
@Composable
fun EvolucaoPacienteRota(viewModel: EvolucaoViewModel, avaliacoes: AvaliacoesViewModel) {
    val estado by viewModel.estado.collectAsState()
    LaunchedEffect(Unit) { viewModel.abrir(null) }
    var guia by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().padding(horizontal = Dimens.margemCompacta)) {
        Text(stringResource(Res.string.aba_evolucao), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = Dimens.espacoMedio).semantics { heading() })
        TabRow(selectedTabIndex = guia) {
            Tab(guia == 0, { guia = 0 }, text = { Text(stringResource(Res.string.evolucao_aba_graficos)) })
            Tab(guia == 1, { guia = 1 }, text = { Text(stringResource(Res.string.resultados_aba)) })
            Tab(guia == 2, { guia = 2 }, text = { Text(stringResource(Res.string.exames_aba)) })
            Tab(guia == 3, { guia = 3 }, text = { Text(stringResource(Res.string.evolucao_aba_avaliacoes)) })
        }
        when (guia) {
            0 -> EvolucaoTela(estado, acoes(viewModel))
            1 -> MeusResultadosRota(avaliacoes)
            2 -> MeusExamesRota(avaliacoes)
            else -> AvaliacoesPacienteRota(avaliacoes)
        }
    }
}
