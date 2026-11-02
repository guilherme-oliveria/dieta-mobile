package org.br.dietamais.ui.paciente.dieta

import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.br.dietamais.ui.paciente.substituicao.*

/** Conteúdo instalado exclusivamente na aba Dieta dos shells nativo e web. */
@Composable
fun DietaPacienteRota(vm: DietaPacienteViewModel, pdf: @Composable (EstadoDietaPaciente) -> Unit = {}) {
    val estado by vm.estado.collectAsState()
    val ciclo by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    RetornoDietaPaciente(estado, vm::fecharLista, { vm.abrirRefeicao(null) }, vm::cancelarTroca, ativo = ciclo == Lifecycle.State.RESUMED)
    DietaPacienteTela(estado, vm::abrirRefeicao, vm::escolher, pdf = pdf, aoAbrirLista = vm::abrirLista,
        acoesLista = AcoesListaPaciente(vm::fecharLista, vm::buscarNaLista, vm::filtrarGrupoLista, vm::expandirGrupoLista, vm::tentarNovamente),
        aoTrocar = vm::abrirTroca, aoMarcar = vm::marcar, aoTrocarRefeicao = vm::abrirTroca)
    TrocaTela(estado, vm::escolherSubstituto, vm::confirmarTroca, vm::cancelarTroca)
}
