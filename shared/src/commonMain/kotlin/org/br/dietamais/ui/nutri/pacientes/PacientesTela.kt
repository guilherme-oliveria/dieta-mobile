package org.br.dietamais.ui.nutri.pacientes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.br.dietamais.dominio.pacientes.*
import org.br.dietamais.dominio.agenda.formatarData
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.theme.*
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PacientesTela(estado: EstadoPacientes, aoBusca: (String) -> Unit, aoFiltro: (StatusVinculo?) -> Unit,
    aoNovo: () -> Unit, aoAtualizar: () -> Unit, aoSelecionar: (PacienteResposta) -> Unit, aoFiltroAtencao: (FiltroAtencao?) -> Unit = {}, aoFixar: (kotlin.uuid.Uuid) -> Unit = {},
    /** Largura grande (tablet, desktop): os chips de filtro ficam visíveis em vez de atrás do chip "Filtros" (MOB-UX-01). */ largo: Boolean = false) {
    val grupos = remember(estado.lista) { agruparPorInicial(estado.lista) }
    // MOB-67c: atalhos só na visão sem busca nem filtro (a lista filtrada mostra o que foi pedido)
    val atalhosVisiveis = estado.busca.isBlank() && estado.filtro == null && estado.filtroAtencao == null && (estado.atalhos.fixados.isNotEmpty() || estado.atalhos.recentes.isNotEmpty())
    val posicoes = remember(grupos, atalhosVisiveis) { grupos.runningFold(3 + (if (atalhosVisiveis) 1 else 0)) { acc, g -> acc + 1 + g.pacientes.size }.dropLast(1) }
    val estadoLista = rememberLazyListState()
    val escopo = rememberCoroutineScope()
    var filtrosAbertos by rememberSaveable { mutableStateOf(false) }
    val filtrosAtivos = contarFiltrosAtivos(estado.filtro, estado.filtroAtencao)
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(Modifier.fillMaxSize().testTag("lista-pacientes"), estadoLista, contentPadding = PaddingValues(start = Dimens.margemCompacta, end = Dimens.margemCompacta, bottom = Dimens.margemCompacta), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
            item(key = "cabecalho") {
                Column(Modifier.padding(top = Dimens.margemCompacta), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
                    Text(stringResource(Res.string.aba_pacientes), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
                    estado.assinatura?.let { Text(stringResource(Res.string.pacientes_uso_plano, it.pacientesOcupados, it.limitePacientes), style = MaterialTheme.typography.bodySmall) }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                        BotaoPrincipal(stringResource(Res.string.pacientes_novo), aoNovo, habilitado = !estado.atualizando && !estado.envioEmAndamento)
                        BotaoTexto(stringResource(Res.string.pacientes_atualizar), aoAtualizar, carregando = estado.atualizando)
                    }
                }
            }
            // MOB-UX-01: a busca fica fixa no topo ao rolar; os filtros ficam atrás do chip "Filtros" no celular
            stickyHeader(key = "busca") {
                Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.background) {
                    Column(Modifier.padding(vertical = Dimens.espacoPequeno), verticalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno), verticalAlignment = Alignment.CenterVertically) {
                            CampoTexto(stringResource(Res.string.pacientes_busca), estado.busca, aoBusca, Modifier.weight(1f))
                            if (!largo) ChipEscolha(selected = filtrosAtivos > 0, onClick = { filtrosAbertos = true },
                                label = { Text(if (filtrosAtivos > 0) stringResource(Res.string.pacientes_filtros_contagem, filtrosAtivos) else stringResource(Res.string.pacientes_filtros)) })
                        }
                        if (largo) FiltrosPacientes(estado, aoFiltro, aoFiltroAtencao, titulos = false)
                    }
                }
            }
            item(key = "resumo") {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
                    if (estado.resumos != null) Text(stringResource(Res.string.pacientes_ativos_atencao, estado.ativos, estado.precisamAtencao), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    if (estado.lista.size >= LIMITE_INDICE) IndiceInicialPacientes(grupos.map { it.inicial }) { i -> escopo.launch { estadoLista.animateScrollToItem(posicoes[grupos.indexOfFirst { it.inicial == i }]) } }
                    if (estado.semConexao) FaixaOffline()
                    estado.erro?.let { MensagemPaciente(it, alerta = true) }
                    estado.aviso?.let { MensagemPaciente(it) }
                }
            }
            if (atalhosVisiveis) item(key = "atalhos") {
                Column(Modifier.testTag("atalhos-pacientes"), verticalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                    if (estado.atalhos.fixados.isNotEmpty()) {
                        Text(stringResource(Res.string.pacientes_fixados), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                            estado.atalhos.fixados.forEach { p -> ChipEscolha(selected = estado.selecionado?.vinculoId == p.vinculoId, onClick = { aoSelecionar(p) },
                                modifier = Modifier.heightIn(min = Dimens.alvoToque), label = { Text(p.nome) }) }
                        }
                    }
                    if (estado.atalhos.recentes.isNotEmpty()) {
                        Text(stringResource(Res.string.pacientes_recentes), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                            estado.atalhos.recentes.forEach { p -> ChipEscolha(selected = estado.selecionado?.vinculoId == p.vinculoId, onClick = { aoSelecionar(p) },
                                modifier = Modifier.heightIn(min = Dimens.alvoToque), label = { Text(p.nome) }) }
                        }
                    }
                }
            }
            when {
                estado.atualizando && estado.lista.isEmpty() -> item { EstadoCarregando() }
                estado.lista.isEmpty() && estado.erro != null -> item { EstadoErro(aoAtualizar, mensagem = textoMensagemPaciente(estado.erro)) }
                estado.lista.isEmpty() -> item { EstadoVazio(mensagem = stringResource(Res.string.pacientes_lista_vazia)) }
                else -> {
                    grupos.forEach { grupo ->
                    item(key = "inicial-${grupo.inicial}") {
                        Text(grupo.inicial.toString(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.semantics { heading() })
                    }
                    items(grupo.pacientes, key = { it.vinculoId.toString() }) { paciente ->
                        Surface(shape = FormasDieta.card, color = MaterialTheme.colorScheme.surface, border = BorderStroke(Dimens.borda, MaterialTheme.colorScheme.outlineVariant)) {
                            Row(Modifier.fillMaxWidth().selectable(estado.selecionado?.vinculoId == paciente.vinculoId, role = Role.Button,
                                onClick = { aoSelecionar(paciente) }).padding(Dimens.paddingCampo).semantics(mergeDescendants = true) {},
                                horizontalArrangement = Arrangement.spacedBy(Dimens.espacoMedio), verticalAlignment = Alignment.CenterVertically) {
                                AvatarPaciente(paciente.nome)
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMinimo)) {
                                    Text(paciente.nome, style = MaterialTheme.typography.bodyLarge)
                                    StatusPaciente(paciente.status)
                                    estado.resumos?.get(paciente.vinculoId)?.let { r ->
                                        r.proximaConsulta?.let { Text(stringResource(Res.string.pacientes_proximo_retorno, formatarData(it.toLocalDateTime(TimeZone.currentSystemDefault()).date)), style = MaterialTheme.typography.bodySmall) }
                                        r.ultimaAvaliacaoEm?.let { Text(stringResource(Res.string.pacientes_ultima_avaliacao, formatarData(it.toLocalDateTime(TimeZone.currentSystemDefault()).date)), style = MaterialTheme.typography.bodySmall) }
                                    }
                                }
                                BotaoTexto(stringResource(if (paciente.vinculoId in estado.fixadosIds) Res.string.pacientes_desafixar else Res.string.pacientes_fixar), { aoFixar(paciente.vinculoId) })
                            }
                        }
                    }
                    }
                }
            }
        }
    }
    if (filtrosAbertos) ModalBottomSheet(onDismissRequest = { filtrosAbertos = false }) {
        Column(Modifier.padding(horizontal = Dimens.margemCompacta).padding(bottom = Dimens.margemAmpla), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
            Text(stringResource(Res.string.pacientes_filtros), style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            FiltrosPacientes(estado, aoFiltro, aoFiltroAtencao, titulos = true)
            BotaoPrincipal(stringResource(Res.string.pacientes_filtros_concluir), { filtrosAbertos = false }, Modifier.fillMaxWidth())
        }
    }
}

/** Chips de situação e, havendo resumos, de atenção (MOB-UX-01): inline em tela larga, na folha "Filtros" no celular. */
@Composable
private fun FiltrosPacientes(estado: EstadoPacientes, aoFiltro: (StatusVinculo?) -> Unit, aoFiltroAtencao: (FiltroAtencao?) -> Unit, titulos: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
        if (titulos) Text(stringResource(Res.string.pacientes_filtros_situacao), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
            listOf(null, StatusVinculo.CONVIDADO, StatusVinculo.ATIVO, StatusVinculo.ENCERRADO).forEach { filtro ->
                ChipEscolha(selected = estado.filtro == filtro, onClick = { aoFiltro(filtro) }, modifier = Modifier.heightIn(min = Dimens.alvoToque),
                    label = { Text(stringResource(when (filtro) {
                        null -> Res.string.pacientes_todos
                        StatusVinculo.CONVIDADO -> Res.string.pacientes_convidados
                        StatusVinculo.ATIVO -> Res.string.pacientes_ativos
                        else -> Res.string.pacientes_encerrados
                    })) })
            }
        }
        if (estado.resumos != null) {
            if (titulos) Text(stringResource(Res.string.pacientes_filtros_atencao), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                FiltroAtencao.entries.forEach { f ->
                    ChipEscolha(selected = estado.filtroAtencao == f, onClick = { aoFiltroAtencao(f) }, modifier = Modifier.heightIn(min = Dimens.alvoToque),
                        label = { Text(stringResource(when (f) {
                            FiltroAtencao.RETORNO_HOJE -> Res.string.pacientes_atencao_retorno_hoje
                            FiltroAtencao.AVALIACAO_VENCIDA -> Res.string.pacientes_atencao_avaliacao_vencida
                            FiltroAtencao.SEM_RETORNO -> Res.string.pacientes_atencao_sem_retorno
                        })) })
                }
            }
        }
    }
}

/** Quantos pacientes na lista pedem o índice A-Z (ART-P1-01): com poucos, a rolagem basta. */
private const val LIMITE_INDICE = 12

/** Índice A-Z: só as iniciais que existem; cada uma leva ao grupo. */
@Composable
private fun IndiceInicialPacientes(iniciais: List<Char>, aoEscolher: (Char) -> Unit) {
    // uma linha só, rolável: com 26 letras em quebra de linha o primeiro paciente descia da tela (MOB-UX-01)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Dimens.espacoMinimo)) {
        iniciais.forEach { i ->
            TextButton(onClick = { aoEscolher(i) }, modifier = Modifier.defaultMinSize(minWidth = Dimens.alvoToque, minHeight = Dimens.alvoToque)) { Text(i.toString()) }
        }
    }
}

@Composable
fun AvatarPaciente(nome: String) {
    val iniciais = nome.trim().split(Regex("\\s+")).take(2).mapNotNull { it.firstOrNull() }.joinToString("").uppercase()
    Surface(Modifier.size(Dimens.avatarPaciente), color = MaterialTheme.colorScheme.primaryContainer, shape = FormasDieta.pilula) {
        Box(contentAlignment = Alignment.Center) { Text(iniciais, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.clearAndSetSemantics {}) }
    }
}
