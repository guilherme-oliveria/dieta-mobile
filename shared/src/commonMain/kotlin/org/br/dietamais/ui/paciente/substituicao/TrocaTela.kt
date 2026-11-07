package org.br.dietamais.ui.paciente.substituicao

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.br.dietamais.dominio.rotina.Substituto
import org.br.dietamais.dominio.rotina.substitutosDoItem
import org.br.dietamais.data.remoto.dto.ItemRefeicaoResposta
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.nutri.dieta.formatar
import org.br.dietamais.ui.paciente.dieta.EstadoDietaPaciente
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource
import kotlin.uuid.Uuid

private const val TAMANHO_PAGINA = 30

private sealed interface LinhaTroca {
    val chave: String
    class Cabecalho(val item: ItemRefeicaoResposta, val total: Int, val manter: Boolean) : LinhaTroca { override val chave = "c-${item.id}" }
    class Opcao(val item: ItemRefeicaoResposta, val substituto: Substituto) : LinhaTroca { override val chave = "o-${item.id}-${substituto.alimento.id}" }
    class SemOpcao(val item: ItemRefeicaoResposta) : LinhaTroca { override val chave = "s-${item.id}" }
}

private fun semAcentos(s: String) = s.lowercase().map { when (it) {
    'á', 'à', 'â', 'ã', 'ä' -> 'a'; 'é', 'ê', 'è', 'ë' -> 'e'; 'í', 'ì', 'î', 'ï' -> 'i'
    'ó', 'ô', 'õ', 'ò', 'ö' -> 'o'; 'ú', 'ü', 'ù', 'û' -> 'u'; 'ç' -> 'c'; else -> it
} }.joinToString("")

/**
 * Troca de alimento (RN-SUB): primeiro os substitutos da lista do paciente para o item/grupo, com busca, lista preguiçosa paginada
 * (30 por vez) e voltar na barra do topo e no voltar do sistema. Vira check-in TROCADO.
 */
@Composable
fun TrocaTela(estado: EstadoDietaPaciente, aoEscolher: (Uuid, Uuid?) -> Unit, aoConfirmar: () -> Unit, aoCancelar: () -> Unit) {
    val troca = estado.troca ?: return
    val dieta = estado.dieta ?: return
    val refeicao = dieta.refeicoes.firstOrNull { it.id == troca.refeicaoId } ?: return
    TelaSobreposta(aoFechar = aoCancelar) {
        var busca by remember(troca.refeicaoId, troca.itemId) { mutableStateOf("") }
        var limite by remember(troca.refeicaoId, troca.itemId, busca) { mutableStateOf(TAMANHO_PAGINA) }
        val termo = semAcentos(busca.trim())
        val linhas = remember(refeicao, troca.itemId, troca.escolhas, dieta.listaSubstituicao, termo) {
            refeicao.itens.filter { troca.itemId == null || it.id == troca.itemId }.flatMap { item ->
                val todas = substitutosDoItem(item, dieta.listaSubstituicao)
                val opcoes = todas.filter { termo.isEmpty() || semAcentos(it.alimento.nome).contains(termo) }
                when {
                    todas.isEmpty() -> listOf(LinhaTroca.SemOpcao(item))
                    termo.isNotEmpty() && opcoes.isEmpty() -> emptyList()
                    else -> listOf<LinhaTroca>(LinhaTroca.Cabecalho(item, opcoes.size, troca.escolhas[item.id] != null)) + opcoes.map { LinhaTroca.Opcao(item, it) }
                }
            }
        }
        val visiveis = linhas.take(limite)
        val lista = rememberLazyListState()
        LaunchedEffect(lista, linhas.size) {
            snapshotFlow { lista.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }.collect { ultimo ->
                if (ultimo >= visiveis.size - 5 && limite < linhas.size) limite += TAMANHO_PAGINA
            }
        }
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {
                BarraTopo(stringResource(Res.string.pac_troca_titulo), aoCancelar, stringResource(Res.string.pac_troca_voltar), subtitulo = refeicao.nome)
                Column(Modifier.weight(1f).fillMaxWidth().wrapContentWidth(androidx.compose.ui.Alignment.CenterHorizontally).widthIn(max = Dimens.larguraLeitura).fillMaxWidth()) {
                    if (estado.erro != null) Text(stringResource(Res.string.pac_erro_geral), color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(Dimens.paddingCard).semantics { liveRegion = LiveRegionMode.Assertive })
                    CampoTexto(stringResource(Res.string.pac_lista_buscar), busca, { busca = it.take(200) }, Modifier.fillMaxWidth().padding(horizontal = Dimens.margemCompacta, vertical = Dimens.espacoPequeno))
                    if (linhas.isEmpty()) Text(stringResource(Res.string.pac_lista_busca_vazia), style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(Dimens.margemCompacta))
                    LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = lista, contentPadding = PaddingValues(Dimens.margemCompacta),
                        verticalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                        items(visiveis, key = { it.chave }) { linha ->
                            when (linha) {
                                is LinhaTroca.Cabecalho -> Column {
                                    Text(linha.item.textoLivre ?: linha.item.nomeAlimento.orEmpty(), style = MaterialTheme.typography.titleMedium)
                                    if (linha.item.alimentoId == null) Text(stringResource(Res.string.pac_troca_medida_grupo), style = MaterialTheme.typography.bodySmall)
                                    if (linha.manter) BotaoTexto(stringResource(Res.string.pac_troca_manter), { aoEscolher(linha.item.id, null) })
                                }
                                is LinhaTroca.SemOpcao -> Column {
                                    Text(linha.item.textoLivre ?: linha.item.nomeAlimento.orEmpty(), style = MaterialTheme.typography.titleMedium)
                                    Text(stringResource(Res.string.pac_troca_sem_opcao), style = MaterialTheme.typography.bodySmall)
                                }
                                is LinhaTroca.Opcao -> {
                                    val o = linha.substituto
                                    val marcado = o.alimento.id == troca.escolhas[linha.item.id]
                                    BotaoTexto(
                                        (if (marcado) "✓ " else "") + if (linha.item.alimentoId == null) stringResource(Res.string.pac_troca_opcao_grupo, o.alimento.nome)
                                        else stringResource(Res.string.pac_troca_opcao, o.alimento.nome, o.alimento.medidaCaseira, formatar(o.gramas)),
                                        { aoEscolher(linha.item.id, o.alimento.id) }, selecionado = marcado)
                                }
                            }
                        }
                    }
                    Column(Modifier.padding(Dimens.margemCompacta), verticalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                        BotaoPrincipal(stringResource(Res.string.pac_troca_confirmar), aoConfirmar, Modifier.fillMaxWidth(), habilitado = troca.escolhas.isNotEmpty())
                        BotaoTexto(stringResource(Res.string.pac_troca_cancelar), aoCancelar)
                    }
                }
            }
        }
    }
}
