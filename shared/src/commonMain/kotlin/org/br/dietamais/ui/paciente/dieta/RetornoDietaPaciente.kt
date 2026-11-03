package org.br.dietamais.ui.paciente.dieta

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler

@Composable
@OptIn(ExperimentalComposeUiApi::class)
fun RetornoDietaPaciente(estado: EstadoDietaPaciente, aoFecharLista: () -> Unit, aoFecharRefeicao: () -> Unit,
    aoFecharTroca: () -> Unit, ativo: Boolean) {
    BackHandler(enabled = ativo && (estado.troca != null || estado.listaAberta || estado.refeicaoAberta != null)) {
        // O diálogo sempre tem prioridade; nunca fecha o detalhe que está atrás dele.
        when {
            estado.troca != null -> aoFecharTroca()
            estado.listaAberta -> aoFecharLista()
            else -> aoFecharRefeicao()
        }
    }
}
