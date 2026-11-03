package org.br.dietamais.ui.pendencias

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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.br.dietamais.data.repositorio.EstadoPendencia
import org.br.dietamais.data.repositorio.Pendencia
import org.br.dietamais.data.sync.OperacaoMutacao
import org.br.dietamais.dominio.pendencias.diferencas
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource
import kotlin.uuid.Uuid

data class AcoesPendencias(val aoAbrir: () -> Unit = {}, val aoFechar: () -> Unit = {}, val aoSelecionar: (Uuid) -> Unit = {}, val aoVoltar: () -> Unit = {},
    val aoManterMinha: (Uuid) -> Unit = {}, val aoUsarServidor: (Uuid) -> Unit = {}, val aoDescartar: (Uuid) -> Unit = {})

/** Contador do que ainda não foi aplicado no servidor; some quando a fila esvazia. */
@Composable
fun BotaoPendencias(estado: EstadoPendencias, acoes: AcoesPendencias, modifier: Modifier = Modifier) {
    if (estado.total > 0) BotaoTexto(stringResource(Res.string.pendencias_botao_n, estado.total), acoes.aoAbrir, modifier)
}

@Composable
fun PendenciasTela(estado: EstadoPendencias, acoes: AcoesPendencias) {
    if (!estado.aberto) return
    Dialog(onDismissRequest = acoes.aoFechar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Column(Modifier.widthIn(max = Dimens.larguraLeitura).fillMaxSize().padding(Dimens.margemCompacta), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
                    Text(stringResource(Res.string.pendencias_titulo), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                    val detalhe = estado.detalhe
                    if (detalhe == null) Lista(estado, acoes, Modifier.weight(1f)) else Detalhe(detalhe, acoes, Modifier.weight(1f))
                    BotaoPrincipal(stringResource(Res.string.pendencias_fechar), acoes.aoFechar, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun Lista(estado: EstadoPendencias, acoes: AcoesPendencias, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
        Text(stringResource(Res.string.pendencias_explicacao), style = MaterialTheme.typography.bodyMedium)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
            items(estado.pendencias, key = { it.idMutacao.toString() }) { p ->
                CardDieta(Modifier.fillMaxWidth().clickable { acoes.aoSelecionar(p.idMutacao) }) {
                    Text(rotulo(p), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(stringResource(textoEstado(p.estado)), style = MaterialTheme.typography.bodySmall,
                        color = if (p.estado == EstadoPendencia.AGUARDANDO) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun Detalhe(p: Pendencia, acoes: AcoesPendencias, modifier: Modifier) {
    val campos = diferencas(p.minha, p.servidor)
    val vazio = stringResource(Res.string.pendencias_vazio_valor)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
        Text(rotulo(p), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(textoEstado(p.estado)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        p.codigo?.let { Text(stringResource(Res.string.pendencias_motivo, motivo(it)), style = MaterialTheme.typography.bodyMedium) }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
            items(campos, key = { it.nome }) { c ->
                CardDieta(Modifier.fillMaxWidth()) {
                    Text(c.nome, style = MaterialTheme.typography.labelLarge)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(Res.string.pendencias_minha), style = MaterialTheme.typography.labelSmall)
                            Text(c.minha ?: vazio, style = MaterialTheme.typography.bodyMedium)
                        }
                        if (p.servidor != null) Column(Modifier.weight(1f)) {
                            Text(stringResource(Res.string.pendencias_servidor), style = MaterialTheme.typography.labelSmall)
                            Text(c.servidor ?: vazio, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
        if (p.estado == EstadoPendencia.CONFLITO) {
            BotaoPrincipal(stringResource(Res.string.pendencias_manter_minha), { acoes.aoManterMinha(p.idMutacao) }, Modifier.fillMaxWidth())
            BotaoContornado(stringResource(Res.string.pendencias_usar_servidor), { acoes.aoUsarServidor(p.idMutacao) }, Modifier.fillMaxWidth())
        }
        BotaoContornado(stringResource(Res.string.pendencias_descartar), { acoes.aoDescartar(p.idMutacao) }, Modifier.fillMaxWidth())
        BotaoTexto(stringResource(Res.string.pendencias_voltar), acoes.aoVoltar, Modifier.fillMaxWidth())
    }
}

@Composable
private fun motivo(codigo: String): String = when (codigo) {
    "LIMITE_PACIENTES_ATINGIDO" -> stringResource(Res.string.pendencias_codigo_LIMITE_PACIENTES_ATINGIDO)
    "ASSINATURA_INATIVA" -> stringResource(Res.string.pendencias_codigo_ASSINATURA_INATIVA)
    "VINCULO_JA_EXISTE" -> stringResource(Res.string.pendencias_codigo_VINCULO_JA_EXISTE)
    "JUSTIFICATIVA_OBRIGATORIA" -> stringResource(Res.string.pendencias_codigo_JUSTIFICATIVA_OBRIGATORIA)
    "RECURSO_NAO_ENCONTRADO" -> stringResource(Res.string.pendencias_codigo_RECURSO_NAO_ENCONTRADO)
    else -> stringResource(Res.string.pendencias_codigo_outro, codigo)
}

private fun textoEstado(e: EstadoPendencia) = when (e) {
    EstadoPendencia.AGUARDANDO -> Res.string.pendencias_estado_aguardando
    EstadoPendencia.CONFLITO -> Res.string.pendencias_estado_conflito
    EstadoPendencia.REJEITADA -> Res.string.pendencias_estado_rejeitada
}

@Composable
private fun rotulo(p: Pendencia): String {
    val entidade = stringResource(when (p.entidade) {
        "pacientes" -> Res.string.pendencias_entidade_pacientes
        "alimentos" -> Res.string.pendencias_entidade_alimentos
        "listas" -> Res.string.pendencias_entidade_listas
        "dietas" -> Res.string.pendencias_entidade_dietas
        "avaliacoes" -> Res.string.pendencias_entidade_avaliacoes
        "modelos" -> Res.string.pendencias_entidade_modelos
        else -> Res.string.pendencias_entidade_consultas
    })
    val operacao = stringResource(if (p.operacao == OperacaoMutacao.CRIAR) Res.string.pendencias_operacao_criar else Res.string.pendencias_operacao_atualizar)
    return "$entidade · $operacao"
}

@Composable
fun PendenciasRota(viewModel: PendenciasViewModel, modifier: Modifier = Modifier) {
    val estado by viewModel.estado.collectAsState()
    val acoes = AcoesPendencias(viewModel::abrir, viewModel::fechar, viewModel::selecionar, viewModel::voltar, viewModel::manterMinha, viewModel::usarServidor, viewModel::descartar)
    BotaoPendencias(estado, acoes, modifier)
    PendenciasTela(estado, acoes)
}
