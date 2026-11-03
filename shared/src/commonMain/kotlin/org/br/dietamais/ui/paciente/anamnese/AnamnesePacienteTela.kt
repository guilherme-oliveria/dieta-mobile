package org.br.dietamais.ui.paciente.anamnese

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.br.dietamais.dominio.anamnese.*
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.nutri.anamnese.textoMensagemAnamnese
import org.br.dietamais.ui.nutri.anamnese.MensagemAnamnese
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource
import kotlin.uuid.Uuid

data class AcoesAnamnesePaciente(val aoAbrir: (Uuid) -> Unit = {}, val aoMudar: (Uuid, EntradaResposta) -> Unit = { _, _ -> }, val aoProximo: () -> Unit = {},
    val aoVoltar: () -> Unit = {}, val aoConcluir: () -> Unit = {}, val aoSair: () -> Unit = {}, val aoDispensar: () -> Unit = {})

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
fun AnamnesePacienteTela(estado: EstadoAnamnesePaciente, acoes: AcoesAnamnesePaciente) {
    val questionario = estado.aberto ?: return
    val passos = estado.passos
    val indice = estado.passoAtual
    val passo = passos.getOrNull(indice)
    val ultimo = indice >= passos.size - 1
    val (respondidas, total) = progresso(questionario.blocos, estado.respostas)
    val habilitado = !estado.concluindo
    Dialog(onDismissRequest = acoes.aoSair, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Column(Modifier.widthIn(max = Dimens.larguraLeitura).fillMaxSize().padding(Dimens.margemCompacta), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
                    Text(questionario.nome, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                    LinearProgressIndicator(progress = { if (total == 0) 0f else respondidas.toFloat() / total }, Modifier.fillMaxWidth())
                    Text(stringResource(Res.string.anamnese_pac_progresso, respondidas, total), style = MaterialTheme.typography.bodySmall)
                    if (estado.semConexao) FaixaOffline()
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
                        if (passo != null) {
                            Text(stringResource(Res.string.anamnese_pac_passo, indice + 1, passos.size) + " · " + passo.titulo,
                                style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                            passo.perguntas.forEach { p ->
                                val entrada = estado.entradas[p.chave] ?: entradaInicial(p, questionario.respostas[p.chave])
                                CampoPergunta(p, entrada, estado.erros[p.chave], habilitado) { acoes.aoMudar(p.chave, it) }
                            }
                        }
                    }
                    estado.mensagem?.let { Text(textoMensagemAnamnese(it), color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }) }
                    if (ultimo && estado.semConexao) Text(stringResource(Res.string.anamnese_pac_offline_concluir), style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno), verticalAlignment = Alignment.CenterVertically) {
                        BotaoTexto(stringResource(Res.string.anamnese_pac_sair), acoes.aoSair, habilitado = habilitado)
                        Spacer(Modifier.weight(1f))
                        if (indice > 0) BotaoContornado(stringResource(Res.string.anamnese_pac_anterior), acoes.aoVoltar, habilitado = habilitado)
                        if (ultimo) BotaoPrincipal(stringResource(Res.string.anamnese_pac_concluir), acoes.aoConcluir, habilitado = !estado.semConexao, carregando = estado.concluindo)
                        else BotaoPrincipal(stringResource(Res.string.anamnese_pac_proximo), acoes.aoProximo, habilitado = habilitado)
                    }
                }
            }
        }
    }
}

@Composable
fun AnamnesePacienteRota(viewModel: AnamnesePacienteViewModel) {
    val estado by viewModel.estado.collectAsState()
    val acoes = AcoesAnamnesePaciente(viewModel::abrir, viewModel::mudar, viewModel::proximo, viewModel::voltar, viewModel::concluir, viewModel::fechar, viewModel::limparMensagem)
    BannerQuestionario(estado, acoes)
    AnamnesePacienteTela(estado, acoes)
}
