package org.br.dietamais.ui.nutri.produtos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import org.br.dietamais.data.remoto.dto.ProdutoResposta
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.nutri.alimentos.textoMensagemAlimentos
import org.br.dietamais.ui.theme.*
import org.jetbrains.compose.resources.stringResource

class AcoesProdutos(val aoBusca: (String) -> Unit = {}, val aoAtivo: (Boolean) -> Unit = {}, val aoNovo: () -> Unit = {}, val aoEditar: (ProdutoResposta) -> Unit = {},
    val aoDesativar: (ProdutoResposta) -> Unit = {}, val aoMais: () -> Unit = {}, val aoAtualizar: () -> Unit = {}, val aoCampo: (CampoProduto, String) -> Unit = { _, _ -> },
    val aoSalvar: () -> Unit = {}, val aoFechar: () -> Unit = {}, val aoConfirmarDesativar: () -> Unit = {}, val aoDescartar: () -> Unit = {})

/** Lista de produtos e suplementos da nutri (MOB-70, RN-ALI-05): busca, ativos/inativos, paginação e ações. Sem artboard (ART-P3): ver `divergencias.md`. */
@Composable
fun ProdutosTela(estado: EstadoProdutos, a: AcoesProdutos) {
    val margem = Dimens.margemTela(currentWindowAdaptiveInfoV2().windowSizeClass)
    val habilitado = !estado.enviando
    LazyColumn(Modifier.fillMaxSize().testTag("lista-produtos"), contentPadding = PaddingValues(margem), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
                Text(stringResource(Res.string.prod_titulo), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                    BotaoPrincipal(stringResource(Res.string.prod_novo), a.aoNovo, habilitado = habilitado && !estado.semConexao)
                    BotaoTexto(stringResource(Res.string.sync_atualizar), a.aoAtualizar, carregando = estado.carregando, habilitado = habilitado)
                }
                if (estado.semConexao) FaixaOffline()
                MensagensProdutos(estado)
                CampoTexto(stringResource(Res.string.prod_busca), estado.busca, a.aoBusca)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                    ChipEscolha(estado.ativo, { a.aoAtivo(true) }, label = { Text(stringResource(Res.string.alimentos_ativos)) }, modifier = Modifier.heightIn(min = Dimens.alvoToque))
                    ChipEscolha(!estado.ativo, { a.aoAtivo(false) }, label = { Text(stringResource(Res.string.alimentos_inativos)) }, modifier = Modifier.heightIn(min = Dimens.alvoToque))
                }
            }
        }
        if (estado.lista.isEmpty()) item {
            when {
                estado.carregando -> EstadoCarregando()
                estado.erro != null -> EstadoErro(a.aoAtualizar, mensagem = textoMensagemAlimentos(estado.erro))
                else -> EstadoVazio(mensagem = stringResource(Res.string.prod_vazio))
            }
        }
        items(estado.lista, key = { it.id.toString() }) { p ->
            CardDieta(Modifier.fillMaxWidth()) {
                Text(p.nome, style = MaterialTheme.typography.titleMedium)
                listOfNotNull(p.marca, p.classificacao).takeIf { it.isNotEmpty() }?.let { Text(it.joinToString(" · "), style = MaterialTheme.typography.bodySmall) }
                val kcal = p.nutrientes[NutrientesProduto.ENERGIA]; val pro = p.nutrientes[NutrientesProduto.PROTEINA]
                if (kcal != null && pro != null) Text(stringResource(Res.string.prod_resumo, ProdutosViewModel.texto(kcal), ProdutosViewModel.texto(pro)), style = MaterialTheme.typography.bodyMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                    BotaoTexto(stringResource(Res.string.alimentos_acao_editar), { a.aoEditar(p) }, habilitado = habilitado && !estado.semConexao)
                    if (p.ativo) BotaoTexto(stringResource(Res.string.alimentos_acao_desativar), { a.aoDesativar(p) }, habilitado = habilitado && !estado.semConexao)
                }
            }
        }
        if (estado.temMais) item { BotaoContornado(stringResource(Res.string.prod_mais), a.aoMais, Modifier.fillMaxWidth(), habilitado = !estado.carregando) }
    }
    estado.desativar?.let { alvo ->
        AlertDialog(onDismissRequest = { if (!estado.enviando) a.aoFechar() }, title = { Text(stringResource(Res.string.prod_desativar_titulo)) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
                Text(alvo.nome); Text(stringResource(Res.string.prod_desativar_descricao))
                estado.erro?.let { Text(textoMensagemAlimentos(it), color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
            } },
            confirmButton = { BotaoTexto(stringResource(Res.string.alimentos_desativar_titulo), a.aoConfirmarDesativar, carregando = estado.enviando, habilitado = !estado.conflito) },
            dismissButton = { BotaoTexto(stringResource(Res.string.pacientes_cancelar), a.aoFechar, habilitado = !estado.enviando) })
    }
}

@Composable
fun FormularioProdutoTela(estado: EstadoProdutos, a: AcoesProdutos) {
    val f = estado.formulario ?: return
    val habilitado = !estado.enviando && !estado.conflito
    fun erro(campo: String) = estado.errosCampos[campo]
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = Dimens.larguraLeitura).fillMaxWidth().verticalScroll(rememberScrollState()).padding(Dimens.margemCompacta), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
                Text(stringResource(if (f.versao == null) Res.string.prod_novo else Res.string.prod_editar), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
                MensagensProdutos(estado)
                CampoTexto(stringResource(Res.string.prod_nome), f.nome, { a.aoCampo(CampoProduto.NOME, it) }, erro = erro("nome")?.let { textoMensagemAlimentos(it) }, habilitado = habilitado)
                CampoTexto(stringResource(Res.string.prod_marca), f.marca, { a.aoCampo(CampoProduto.MARCA, it) }, habilitado = habilitado)
                CampoTexto(stringResource(Res.string.prod_classificacao), f.classificacao, { a.aoCampo(CampoProduto.CLASSIFICACAO, it) }, habilitado = habilitado)
                CampoTexto(stringResource(Res.string.prod_medida), f.medida, { a.aoCampo(CampoProduto.MEDIDA, it) }, habilitado = habilitado)
                CampoNumero(stringResource(Res.string.prod_medida_gramas), f.medidaGramas, { a.aoCampo(CampoProduto.MEDIDA_GRAMAS, it) }, erro = erro("medidaGramas")?.let { textoMensagemAlimentos(it) }, habilitado = habilitado)
                Text(stringResource(Res.string.prod_por100), style = MaterialTheme.typography.titleSmall)
                CampoNumero(stringResource(Res.string.prod_energia), f.energia, { a.aoCampo(CampoProduto.ENERGIA, it) }, erro = erro(NutrientesProduto.ENERGIA)?.let { textoMensagemAlimentos(it) }, habilitado = habilitado)
                CampoNumero(stringResource(Res.string.prod_carboidrato), f.carboidrato, { a.aoCampo(CampoProduto.CARBOIDRATO, it) }, erro = erro(NutrientesProduto.CARBOIDRATO)?.let { textoMensagemAlimentos(it) }, habilitado = habilitado)
                CampoNumero(stringResource(Res.string.prod_proteina), f.proteina, { a.aoCampo(CampoProduto.PROTEINA, it) }, erro = erro(NutrientesProduto.PROTEINA)?.let { textoMensagemAlimentos(it) }, habilitado = habilitado)
                CampoNumero(stringResource(Res.string.prod_lipidio), f.lipidio, { a.aoCampo(CampoProduto.LIPIDIO, it) }, erro = erro(NutrientesProduto.LIPIDIO)?.let { textoMensagemAlimentos(it) }, habilitado = habilitado)
                BotaoPrincipal(stringResource(Res.string.prod_salvar), a.aoSalvar, Modifier.fillMaxWidth(), habilitado = habilitado && !estado.semConexao, carregando = estado.enviando)
                if (estado.conflito) BotaoContornado(stringResource(Res.string.alimentos_descartar_atualizar), a.aoDescartar, Modifier.fillMaxWidth(), habilitado = !estado.enviando)
                BotaoTexto(stringResource(Res.string.pacientes_cancelar), a.aoFechar, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun MensagensProdutos(estado: EstadoProdutos) {
    estado.erro?.let { Text(textoMensagemAlimentos(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
    estado.aviso?.let { Text(textoMensagemAlimentos(it), style = MaterialTheme.typography.bodySmall, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
}

@Composable
fun ProdutosRota(viewModel: ProdutosViewModel) {
    val estado = viewModel.estado.collectAsState().value
    androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.carregar() }
    val a = AcoesProdutos(viewModel::mudarBusca, viewModel::mudarAtivo, viewModel::novo, viewModel::editar, viewModel::pedirDesativacao, viewModel::maisResultados,
        viewModel::recarregar, viewModel::mudarCampo, viewModel::salvar, viewModel::fechar, viewModel::confirmarDesativacao, viewModel::descartarEAtualizar)
    if (estado.formulario != null) FormularioProdutoTela(estado, a) else ProdutosTela(estado, a)
}
