package org.br.dietamais.ui.paciente.substituicao

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.window.core.layout.WindowSizeClass
import org.br.dietamais.dominio.rotina.GrupoListaPaciente
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.paciente.dieta.EstadoDietaPaciente
import org.br.dietamais.ui.paciente.dieta.numeroPaciente
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource
import kotlin.uuid.Uuid
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class AcoesListaPaciente(val aoVoltar: () -> Unit = {}, val aoBuscar: (String) -> Unit = {},
    val aoFiltrar: (Uuid?) -> Unit = {}, val aoExpandir: (Uuid) -> Unit = {}, val aoTentarNovamente: () -> Unit = {})

/** Lista publicada e já filtrada pelo servidor. A tela não carrega catálogo ou faz chamadas de rede. */
@Composable
fun ListaSubstituicaoTela(estado: EstadoDietaPaciente, acoes: AcoesListaPaciente,
    classe: WindowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass) {
    val ampla = classe.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(Dimens.paddingCard), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
        BotaoTexto(stringResource(Res.string.pac_lista_voltar), acoes.aoVoltar)
        Text(stringResource(Res.string.pac_lista_titulo), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        estado.dieta?.let {
            Text(stringResource(Res.string.pac_lista_nutri, it.nutricionista.nome), style = MaterialTheme.typography.bodySmall)
            val data = it.publicadoEm.toLocalDateTime(TimeZone.currentSystemDefault()).date.toString().split('-').reversed().joinToString("/")
            Text(stringResource(Res.string.pac_lista_publicada, data), style = MaterialTheme.typography.bodySmall)
        }
        if (estado.semConexao) FaixaOffline()
        if (estado.erro != null) EstadoErro(acoes.aoTentarNovamente, mensagem = stringResource(Res.string.pac_erro_geral))
        when {
            estado.erro != null && estado.gruposLista.isEmpty() -> Unit
            !estado.carregado -> Column(verticalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                Text(stringResource(Res.string.carregando), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                repeat(3) { Surface(Modifier.fillMaxWidth().heightIn(min = Dimens.alvoToque), color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {} }
            }
            estado.gruposLista.isEmpty() -> Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                EstadoVazio(mensagem = stringResource(Res.string.pac_lista_vazia))
                Text(stringResource(Res.string.pac_lista_vazia_ajuda), style = MaterialTheme.typography.bodyMedium)
            }
            else -> {
                CampoTexto(stringResource(Res.string.pac_lista_buscar), estado.buscaLista, acoes.aoBuscar, Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                    BotaoTexto(stringResource(Res.string.pac_lista_todos), { acoes.aoFiltrar(null) }, selecionado = estado.grupoListaFiltro == null)
                    estado.gruposLista.forEachIndexed { i, g ->
                        BotaoTexto(nomeGrupo(g, i), { acoes.aoFiltrar(g.id) }, selecionado = estado.grupoListaFiltro == g.id)
                    }
                }
                val grupos = estado.gruposListaVisiveis
                if (grupos.isEmpty()) Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    EstadoVazio(mensagem = stringResource(Res.string.pac_lista_busca_vazia))
                    Text(stringResource(Res.string.pac_lista_busca_ajuda))
                    BotaoTexto(stringResource(Res.string.pac_lista_limpar), { acoes.aoBuscar(""); acoes.aoFiltrar(null) })
                }
                else if (ampla) Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
                    Column(Modifier.width(Dimens.larguraGruposSubstituicao).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                        grupos.forEach { g -> CabecalhoGrupo(g, estado.gruposLista.indexOfFirst { it.id == g.id }, g.id == (grupos.firstOrNull { it.id == estado.grupoListaAberto } ?: grupos.first()).id, { acoes.aoExpandir(g.id) }) }
                    }
                    val selecionado = grupos.firstOrNull { it.id == estado.grupoListaAberto } ?: grupos.first()
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        CardDieta(Modifier.fillMaxWidth()) {
                            Text(nomeGrupo(selecionado, estado.gruposLista.indexOfFirst { it.id == selecionado.id }), style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                            ReferenciaGrupo(selecionado)
                            Text(stringResource(Res.string.pac_lista_equivalencia), style = MaterialTheme.typography.bodySmall)
                            ItensGrupo(selecionado, emTabela = true)
                        }
                    }
                }
                else Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).testTag("lista-substituicao"), verticalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                    grupos.forEach { g ->
                        CardDieta(Modifier.fillMaxWidth()) {
                            val aberto = estado.grupoListaAberto == g.id
                            CabecalhoGrupo(g, estado.gruposLista.indexOfFirst { it.id == g.id }, aberto, { acoes.aoExpandir(g.id) })
                            if (aberto) ItensGrupo(g)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun nomeGrupo(g: GrupoListaPaciente, indice: Int) = g.nome ?: stringResource(Res.string.pac_lista_grupo_sem_nome, indice + 1)

@Composable
private fun CabecalhoGrupo(g: GrupoListaPaciente, indice: Int, aberto: Boolean, aoClicar: () -> Unit) {
    val descricao = stringResource(if (aberto) Res.string.pac_lista_expandido else Res.string.pac_lista_recolhido)
    Column {
        BotaoTexto(nomeGrupo(g, indice), aoClicar, Modifier.fillMaxWidth().semantics { heading(); stateDescription = descricao }, selecionado = aberto)
        ReferenciaGrupo(g)
        Text(stringResource(Res.string.pac_lista_itens, g.alimentos.size), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ReferenciaGrupo(g: GrupoListaPaciente) {
    Text(g.kcalPorPorcao?.let { stringResource(Res.string.pac_lista_porcao, numeroPaciente(it)) } ?: stringResource(Res.string.pac_lista_sem_energia), style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun ItensGrupo(g: GrupoListaPaciente, emTabela: Boolean = false) {
    if (emTabela) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
        Text(stringResource(Res.string.pac_lista_coluna_alimento), Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
        Text(stringResource(Res.string.pac_lista_coluna_medida), Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
        Text(stringResource(Res.string.pac_lista_coluna_gramas), Modifier.weight(0.6f), style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End)
    }
    g.alimentos.forEach { a ->
        HorizontalDivider()
        if (emTabela) Row(Modifier.fillMaxWidth().heightIn(min = Dimens.alvoToque).padding(vertical = Dimens.espacoPequeno), horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
            Text(a.nome, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            Text(a.medidaCaseira, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(Res.string.pac_lista_gramas, numeroPaciente(a.gramasPorPorcao)), Modifier.weight(0.6f), style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.End)
        } else
        Column(Modifier.fillMaxWidth().padding(vertical = Dimens.espacoPequeno), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMinimo)) {
            Text(a.nome, style = MaterialTheme.typography.titleSmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                Text(a.medidaCaseira, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(Res.string.pac_lista_gramas, numeroPaciente(a.gramasPorPorcao)), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
