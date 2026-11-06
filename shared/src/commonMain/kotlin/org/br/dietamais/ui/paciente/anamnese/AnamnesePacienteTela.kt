package org.br.dietamais.ui.paciente.anamnese

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.window.core.layout.WindowSizeClass
import androidx.compose.ui.semantics.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.br.dietamais.dominio.anamnese.*
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.comum.anamnese.*
import org.br.dietamais.ui.nutri.anamnese.textoMensagemAnamnese
import org.br.dietamais.ui.nutri.anamnese.MensagemAnamnese
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource
import kotlin.uuid.Uuid

data class AcoesAnamnesePaciente(val aoAbrir: (Uuid) -> Unit = {}, val aoMudar: (Uuid, EntradaResposta) -> Unit = { _, _ -> }, val aoProximo: () -> Unit = {},
    val aoVoltar: () -> Unit = {}, val aoConcluir: () -> Unit = {}, val aoSair: () -> Unit = {}, val aoDispensar: () -> Unit = {}, val aoAtualizarResultados: () -> Unit = {})

/** Faixa no topo do Hoje enquanto houver questionário pendente (e o aviso de conclusão depois). */
@Composable
fun BannerQuestionario(estado: EstadoAnamnesePaciente, acoes: AcoesAnamnesePaciente) {
    val pendente = estado.pendentes.firstOrNull()
    if (estado.aberto != null) return
    if (pendente == null && estado.mensagem != MensagemAnamnese.CONCLUIDO) return
    CardDieta(Modifier.fillMaxWidth().padding(horizontal = Dimens.paddingCard, vertical = Dimens.espacoPequeno)) {
        if (pendente == null) {
            Text(textoMensagemAnamnese(MensagemAnamnese.CONCLUIDO), color = MaterialTheme.colorScheme.primary, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            BotaoTexto(stringResource(Res.string.anamnese_pac_dispensar), acoes.aoDispensar)
        } else {
            Text(stringResource(Res.string.anamnese_pac_banner), style = MaterialTheme.typography.titleSmall)
            Text(pendente.nome, style = MaterialTheme.typography.bodyMedium)
            BotaoPrincipal(stringResource(Res.string.anamnese_pac_banner_abrir), { acoes.aoAbrir(pendente.id) })
        }
    }
}

/** Assistente em tela cheia: um bloco por passo, com progresso. Salvar parcial funciona offline; só enviar exige rede. */
@Composable
@OptIn(ExperimentalLayoutApi::class)
fun AnamnesePacienteTela(estado: EstadoAnamnesePaciente, acoes: AcoesAnamnesePaciente, classe: WindowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass) {
    val questionario = estado.aberto ?: return
    val passos = estado.passos
    val indice = estado.passoAtual
    val passo = passos.getOrNull(indice)
    val ultimo = indice >= passos.size - 1
    val (respondidas, total) = progresso(questionario.blocos, estado.respostas)
    val habilitado = !estado.concluindo
    val ampla = classe.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) && LocalDensity.current.fontScale <= 1.3f
    val assuntos = passo?.perguntas.orEmpty().mapNotNull { assuntoAnamnese(it.codigo) }.toSet()
    TelaSobreposta(aoFechar = acoes.aoSair) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {
                BarraTopo(questionario.nome, acoes.aoSair, stringResource(Res.string.anamnese_pac_sair), subtitulo = stringResource(Res.string.ana_salvamento_automatico))
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    Column(Modifier.widthIn(max = if (ampla) Dimens.larguraAnamneseAmpla else Dimens.larguraLeitura).fillMaxSize().padding(horizontal = Dimens.paddingCard, vertical = Dimens.espacoMedio),
                        verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
                        if (passo != null) Text(stringResource(Res.string.anamnese_pac_passo, indice + 1, passos.size) + " · " + passo.titulo,
                            style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading(); liveRegion = LiveRegionMode.Polite })
                        LinearProgressIndicator(progress = { if (passos.isEmpty()) 0f else (indice + 1f) / passos.size }, Modifier.fillMaxWidth())
                        Text(stringResource(Res.string.anamnese_pac_progresso, respondidas, total), style = MaterialTheme.typography.bodySmall)
                        if (estado.semConexao) FaixaOffline()
                        if (assuntos.isNotEmpty()) BotaoTexto(stringResource(Res.string.ana_atualizar_resultados), acoes.aoAtualizarResultados,
                            habilitado = habilitado && !estado.semConexao && !estado.atualizandoResultados)
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(Dimens.paddingCard)) {
                            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
                                if (passo != null) {
                                    CamposAnamneseEstruturada(passo.perguntas, estado.entradas, questionario.respostas, estado.erros, habilitado, acoes.aoMudar)
                                    if (!ampla) ResultadosAnamnese(estado.derivados, assuntos)
                                }
                            }
                            if (ampla && assuntos.isNotEmpty()) Column(Modifier.width(Dimens.larguraResultadosAnamnese).verticalScroll(rememberScrollState())) { ResultadosAnamnese(estado.derivados, assuntos) }
                        }
                        estado.mensagem?.let { Text(textoMensagemAnamnese(it), color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }) }
                        if (ultimo && estado.semConexao) Text(stringResource(Res.string.anamnese_pac_offline_concluir), style = MaterialTheme.typography.bodySmall)
                    }
                }
                RodapeAcoes {
                    if (indice > 0) BotaoContornado(stringResource(Res.string.anamnese_pac_anterior), acoes.aoVoltar, habilitado = habilitado)
                    if (ultimo) BotaoPrincipal(stringResource(Res.string.anamnese_pac_concluir), acoes.aoConcluir, Modifier.weight(1f), habilitado = !estado.semConexao, carregando = estado.concluindo)
                    else BotaoPrincipal(stringResource(Res.string.anamnese_pac_proximo), acoes.aoProximo, Modifier.weight(1f), habilitado = habilitado)
                }
            }
        }
    }
}

@Composable
fun AnamnesePacienteRota(viewModel: AnamnesePacienteViewModel) {
    val estado by viewModel.estado.collectAsState()
    val acoes = AcoesAnamnesePaciente(viewModel::abrir, viewModel::mudar, viewModel::proximo, viewModel::voltar, viewModel::concluir, viewModel::fechar, viewModel::limparMensagem, viewModel::atualizarResultados)
    BannerQuestionario(estado, acoes)
    AnamnesePacienteTela(estado, acoes)
}
