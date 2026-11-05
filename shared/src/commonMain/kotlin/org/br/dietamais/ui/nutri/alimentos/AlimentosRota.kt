package org.br.dietamais.ui.nutri.alimentos

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.ChipEscolha
import org.br.dietamais.ui.nutri.produtos.ProdutosRota
import org.br.dietamais.ui.nutri.produtos.ProdutosViewModel
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

/** MOB-70: com [produtos], a aba Alimentos ganha a escolha Alimentos | Produtos (os produtos e suplementos da nutri, BE-48). */
@Composable
@OptIn(ExperimentalComposeUiApi::class)
fun AlimentosRota(viewModel: AlimentosViewModel, produtos: ProdutosViewModel? = null) {
    val estado by viewModel.estado.collectAsState()
    var verProdutos by remember { mutableStateOf(false) }
    val formularioProdutoAberto = produtos?.estado?.collectAsState()?.value?.let { it.formulario != null || it.desativar != null } == true
    BackHandler(enabled = estado.formulario != null || estado.desativar != null) { viewModel.fechar() }
    BackHandler(enabled = verProdutos && formularioProdutoAberto) { produtos?.fechar() }
    val tabs = produtos != null && estado.formulario == null && estado.desativar == null && !(verProdutos && formularioProdutoAberto)
    Column(Modifier.fillMaxSize()) {
        if (tabs) Row(Modifier.padding(horizontal = Dimens.margemCompacta, vertical = Dimens.espacoPequeno), horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
            ChipEscolha(!verProdutos, { verProdutos = false }, label = { Text(stringResource(Res.string.alimentos_aba_alimentos)) }, modifier = Modifier.heightIn(min = Dimens.alvoToque))
            ChipEscolha(verProdutos, { verProdutos = true }, label = { Text(stringResource(Res.string.prod_aba)) }, modifier = Modifier.heightIn(min = Dimens.alvoToque))
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (verProdutos && produtos != null) ProdutosRota(produtos)
            else {
                if (estado.formulario != null) FormularioAlimentoTela(estado, viewModel::mudarCampo, viewModel::mudarGrupoFormulario,
                    viewModel::salvar, viewModel::fechar, { if (estado.conflito) viewModel.descartarEAtualizar() else viewModel.atualizar() })
                else AlimentosTela(estado, viewModel::mudarBusca, viewModel::mudarGrupo, viewModel::mudarAtivo, viewModel::novo,
                    viewModel::editar, viewModel::pedirDesativacao, viewModel::atualizar)
                estado.desativar?.let { ConfirmarDesativacao(it, estado.enviando, viewModel::confirmarDesativacao, viewModel::fechar,
                    habilitado = !estado.conflito, erro = estado.erro) }
            }
        }
    }
}
