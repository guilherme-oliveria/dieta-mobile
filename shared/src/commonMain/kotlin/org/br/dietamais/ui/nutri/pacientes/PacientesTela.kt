package org.br.dietamais.ui.nutri.pacientes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.theme.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun PacientesTela(estado: EstadoPacientes, aoBusca: (String) -> Unit, aoFiltro: (StatusVinculo?) -> Unit,
    aoNovo: () -> Unit, aoAtualizar: () -> Unit, aoSelecionar: (PacienteResposta) -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(Modifier.fillMaxSize().testTag("lista-pacientes"), contentPadding = PaddingValues(Dimens.margemCompacta), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
                    Text(stringResource(Res.string.aba_pacientes), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
                    estado.assinatura?.let { Text(stringResource(Res.string.pacientes_uso_plano, it.pacientesOcupados, it.limitePacientes), style = MaterialTheme.typography.bodySmall) }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                        BotaoPrincipal(stringResource(Res.string.pacientes_novo), aoNovo, habilitado = !estado.atualizando && !estado.envioEmAndamento)
                        BotaoTexto(stringResource(Res.string.pacientes_atualizar), aoAtualizar, carregando = estado.atualizando)
                    }
                    CampoTexto(stringResource(Res.string.pacientes_busca), estado.busca, aoBusca)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                        listOf(null, StatusVinculo.CONVIDADO, StatusVinculo.ATIVO, StatusVinculo.ENCERRADO).forEach { filtro ->
                            FilterChip(selected = estado.filtro == filtro, onClick = { aoFiltro(filtro) }, modifier = Modifier.heightIn(min = Dimens.alvoToque),
                                label = { Text(stringResource(when (filtro) {
                                    null -> Res.string.pacientes_todos
                                    StatusVinculo.CONVIDADO -> Res.string.pacientes_convidados
                                    StatusVinculo.ATIVO -> Res.string.pacientes_ativos
                                    else -> Res.string.pacientes_encerrados
                                })) })
                        }
                    }
                    if (estado.semConexao) FaixaOffline()
                    estado.erro?.let { MensagemPaciente(it, alerta = true) }
                    estado.aviso?.let { MensagemPaciente(it) }
                }
            }
            when {
                estado.atualizando && estado.lista.isEmpty() -> item { EstadoCarregando() }
                estado.lista.isEmpty() && estado.erro != null -> item { EstadoErro(aoAtualizar, mensagem = textoMensagemPaciente(estado.erro)) }
                estado.lista.isEmpty() -> item { EstadoVazio(mensagem = stringResource(Res.string.pacientes_lista_vazia)) }
                else -> {
                    items(estado.lista, key = { it.vinculoId.toString() }) { paciente ->
                        Surface(shape = FormasDieta.card, color = MaterialTheme.colorScheme.surface, border = BorderStroke(Dimens.borda, MaterialTheme.colorScheme.outlineVariant)) {
                            Row(Modifier.fillMaxWidth().selectable(estado.selecionado?.vinculoId == paciente.vinculoId, role = Role.Button,
                                onClick = { aoSelecionar(paciente) }).padding(Dimens.paddingCampo).semantics(mergeDescendants = true) {},
                                horizontalArrangement = Arrangement.spacedBy(Dimens.espacoMedio), verticalAlignment = Alignment.CenterVertically) {
                                AvatarPaciente(paciente.nome)
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMinimo)) {
                                    Text(paciente.nome, style = MaterialTheme.typography.bodyLarge)
                                    StatusPaciente(paciente.status)
                                }
                            }
                        }
                    }
                }
            }
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
