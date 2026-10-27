package org.br.dietamais.ui.nutri.alimentos

import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler

@Composable
@OptIn(ExperimentalComposeUiApi::class)
fun AlimentosRota(viewModel: AlimentosViewModel) {
    val estado by viewModel.estado.collectAsState()
    BackHandler(enabled = estado.formulario != null || estado.desativar != null) { viewModel.fechar() }
    if (estado.formulario != null) FormularioAlimentoTela(estado, viewModel::mudarCampo, viewModel::mudarGrupoFormulario,
        viewModel::salvar, viewModel::fechar, { if (estado.conflito) viewModel.descartarEAtualizar() else viewModel.atualizar() })
    else AlimentosTela(estado, viewModel::mudarBusca, viewModel::mudarGrupo, viewModel::mudarAtivo, viewModel::novo,
        viewModel::editar, viewModel::pedirDesativacao, viewModel::atualizar)
    estado.desativar?.let { ConfirmarDesativacao(it, estado.enviando, viewModel::confirmarDesativacao, viewModel::fechar,
        habilitado = !estado.semConexao && !estado.conflito, erro = estado.erro) }
}
