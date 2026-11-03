package org.br.dietamais.ui.notificacoes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.datetime.TimeZone
import org.br.dietamais.data.remoto.dto.AvisoResposta
import org.br.dietamais.dominio.agenda.diaLocal
import org.br.dietamais.dominio.agenda.formatarData
import org.br.dietamais.dominio.agenda.formatarHora
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource
import kotlin.uuid.Uuid

data class AcoesAvisos(val aoAbrir: () -> Unit = {}, val aoFechar: () -> Unit = {}, val aoTocar: (Uuid) -> Unit = {},
    val aoMarcarTodos: () -> Unit = {}, val aoDispensar: () -> Unit = {})

/** Botão do cabeçalho com a contagem de não lidos. */
@Composable
fun BotaoAvisos(estado: EstadoAvisos, acoes: AcoesAvisos, modifier: Modifier = Modifier) {
    val n = estado.naoLidos
    BotaoTexto(if (n > 0) stringResource(Res.string.avisos_botao_n, n) else stringResource(Res.string.avisos_botao), acoes.aoAbrir, modifier)
}

@Composable
fun AvisosTela(estado: EstadoAvisos, acoes: AcoesAvisos, fuso: TimeZone = TimeZone.currentSystemDefault()) {
    if (!estado.aberto) return
    Dialog(onDismissRequest = acoes.aoFechar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Column(Modifier.widthIn(max = Dimens.larguraLeitura).fillMaxSize().padding(Dimens.margemCompacta), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
                    Text(stringResource(Res.string.avisos_titulo), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                    estado.mensagem?.let {
                        Text(stringResource(if (it == MensagemAvisos.SEM_CONEXAO) Res.string.avisos_msg_sem_conexao else Res.string.avisos_msg_erro),
                            color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    }
                    if (estado.avisos.isEmpty()) EstadoVazio(Modifier.weight(1f), mensagem = stringResource(Res.string.avisos_vazio))
                    else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                        items(estado.avisos, key = { it.id.toString() }) { ItemAviso(it, fuso) { acoes.aoTocar(it.id) } }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
                        BotaoContornado(stringResource(Res.string.avisos_marcar_todos), acoes.aoMarcarTodos, Modifier.weight(1f), habilitado = estado.naoLidos > 0 && !estado.trabalhando)
                        BotaoPrincipal(stringResource(Res.string.avisos_fechar), acoes.aoFechar, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemAviso(aviso: AvisoResposta, fuso: TimeZone, aoTocar: () -> Unit) {
    val naoLidoDescricao = stringResource(Res.string.avisos_nao_lido)
    CardDieta(Modifier.fillMaxWidth().clickable(onClick = aoTocar).semantics(mergeDescendants = true) { if (!aviso.lido) stateDescription = naoLidoDescricao }) {
        Text(aviso.titulo, style = MaterialTheme.typography.titleSmall, fontWeight = if (aviso.lido) FontWeight.Normal else FontWeight.Bold)
        Text(aviso.corpo, style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(Res.string.avisos_quando, formatarData(diaLocal(aviso.criadoEm, fuso)), formatarHora(aviso.criadoEm, fuso)),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun AvisosRota(viewModel: AvisosViewModel, modifier: Modifier = Modifier) {
    val estado by viewModel.estado.collectAsState()
    val acoes = AcoesAvisos(viewModel::abrir, viewModel::fechar, viewModel::tocar, viewModel::marcarTodosLidos, viewModel::limparMensagem)
    BotaoAvisos(estado, acoes, modifier)
    AvisosTela(estado, acoes)
}
