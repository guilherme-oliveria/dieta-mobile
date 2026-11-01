package org.br.dietamais.ui.paciente.dieta

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler

@Composable
@OptIn(ExperimentalComposeUiApi::class)
fun RetornoDietaPaciente(estado: EstadoDietaPaciente, aoFecharLista: () -> Unit, aoFecharRefeicao: () -> Unit) {
    BackHandler(enabled = estado.listaAberta || estado.refeicaoAberta != null) {
        if (estado.listaAberta) aoFecharLista() else aoFecharRefeicao()
    }
}
