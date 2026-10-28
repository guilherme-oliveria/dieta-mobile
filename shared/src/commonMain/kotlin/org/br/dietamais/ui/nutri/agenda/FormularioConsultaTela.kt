package org.br.dietamais.ui.nutri.agenda

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.datetime.TimeZone
import org.br.dietamais.data.remoto.dto.TipoConsulta
import org.br.dietamais.dominio.agenda.*
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource
import kotlin.uuid.Uuid

data class AcoesFormularioConsulta(val aoPaciente: (Uuid) -> Unit = {}, val aoData: (String) -> Unit = {}, val aoHora: (String) -> Unit = {}, val aoDuracao: (String) -> Unit = {},
    val aoTipo: (TipoConsulta) -> Unit = {}, val aoLugar: (String) -> Unit = {}, val aoSalvar: () -> Unit = {}, val aoFechar: () -> Unit = {})

private val tiposEscolhiveis = listOf(TipoConsulta.PRIMEIRA, TipoConsulta.RETORNO, TipoConsulta.ONLINE)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FormularioConsultaTela(estado: EstadoAgenda, acoes: AcoesFormularioConsulta) {
    val form = estado.formulario ?: return
    val habilitado = !estado.enviando && !estado.semConexao
    val erros = estado.errosFormulario
    Dialog(onDismissRequest = acoes.aoFechar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Column(Modifier.widthIn(max = Dimens.larguraLeitura).fillMaxWidth().verticalScroll(rememberScrollState()).padding(Dimens.margemCompacta),
                    verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
                    Text(stringResource(if (form.id == null) Res.string.agenda_form_titulo_nova else Res.string.agenda_form_titulo_remarcar),
                        style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
                    if (estado.semConexao) FaixaOffline()
                    estado.erro?.let { Text(textoMensagemAgenda(it), color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }) }
                    if (form.id == null) {
                        Text(stringResource(Res.string.agenda_form_paciente), style = MaterialTheme.typography.labelLarge)
                        estado.pacientesAgendaveis.forEach { p ->
                            Row(Modifier.fillMaxWidth().heightIn(min = Dimens.alvoToque).selectable(form.pacienteId == p.id, enabled = habilitado, role = Role.RadioButton) { acoes.aoPaciente(p.id) },
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                                RadioButton(form.pacienteId == p.id, onClick = null, enabled = habilitado)
                                Text(p.nome, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                        if (ErroConsulta.PACIENTE in erros) Text(textoErroConsulta(ErroConsulta.PACIENTE), color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                    } else Text(estado.nomeDo(form.pacienteId ?: Uuid.NIL) ?: stringResource(Res.string.agenda_paciente_desconhecido), style = MaterialTheme.typography.titleMedium)
                    CampoTexto(stringResource(Res.string.agenda_form_data), form.data, acoes.aoData, erro = erros.firstOrNull { it == ErroConsulta.DATA }?.let { textoErroConsulta(it) }, habilitado = habilitado,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
                    CampoHora(stringResource(Res.string.agenda_form_hora), form.hora, acoes.aoHora,
                        erro = (erros.firstOrNull { it == ErroConsulta.HORA } ?: erros.firstOrNull { it == ErroConsulta.PASSADO })?.let { textoErroConsulta(it) }, habilitado = habilitado)
                    CampoNumero(stringResource(Res.string.agenda_form_duracao), form.duracao, acoes.aoDuracao, erro = erros.firstOrNull { it == ErroConsulta.DURACAO }?.let { textoErroConsulta(it) }, habilitado = habilitado)
                    Text(stringResource(Res.string.agenda_form_tipo), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                        tiposEscolhiveis.forEach { tipo -> FilterChip(form.tipo == tipo, { acoes.aoTipo(tipo) }, label = { Text(rotuloTipoConsulta(tipo)) }, enabled = habilitado) }
                    }
                    CampoTexto(stringResource(Res.string.agenda_form_lugar), form.lugar, acoes.aoLugar, habilitado = habilitado)
                    if (estado.conflitosAvisados > 0) Text(stringResource(Res.string.agenda_conflito_aviso, estado.conflitosAvisados), color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                        BotaoPrincipal(stringResource(Res.string.agenda_form_salvar), acoes.aoSalvar, habilitado = habilitado, carregando = estado.enviando)
                        BotaoTexto(stringResource(Res.string.agenda_form_voltar), acoes.aoFechar, habilitado = !estado.enviando)
                    }
                }
            }
        }
    }
}

@Composable
fun ConfirmarCancelamentoDialog(estado: EstadoAgenda, aoConfirmar: () -> Unit, aoDesistir: () -> Unit, fuso: TimeZone = TimeZone.currentSystemDefault()) {
    val consulta = estado.consultas.firstOrNull { it.id == estado.cancelandoId } ?: return
    AlertDialog(onDismissRequest = aoDesistir,
        title = { Text(stringResource(Res.string.agenda_cancelar_titulo)) },
        text = {
            Text(stringResource(Res.string.agenda_cancelar_texto, estado.nomeDo(consulta.pacienteId) ?: stringResource(Res.string.agenda_paciente_desconhecido),
                formatarData(diaLocal(consulta.inicio, fuso)), formatarHora(consulta.inicio, fuso)))
        },
        confirmButton = { BotaoPrincipal(stringResource(Res.string.agenda_cancelar_confirmar), aoConfirmar, habilitado = !estado.semConexao, carregando = estado.enviando) },
        dismissButton = { BotaoTexto(stringResource(Res.string.agenda_cancelar_desistir), aoDesistir, habilitado = !estado.enviando) })
}

@Composable
fun AgendaRota(viewModel: AgendaViewModel) {
    val estado by viewModel.estado.collectAsState()
    LaunchedEffect(Unit) { viewModel.abrir() }
    AgendaTela(estado, AcoesAgenda(viewModel::mudarModo, viewModel::anterior, viewModel::proximo, viewModel::hoje, { viewModel.nova() }, viewModel::remarcar, viewModel::pedirCancelamento, viewModel::recarregar))
    FormularioConsultaTela(estado, AcoesFormularioConsulta(viewModel::mudarPaciente, viewModel::mudarData, viewModel::mudarHora, viewModel::mudarDuracao, viewModel::mudarTipo,
        viewModel::mudarLugar, viewModel::salvar, viewModel::fechar))
    ConfirmarCancelamentoDialog(estado, viewModel::confirmarCancelamento, viewModel::desistirDoCancelamento)
}
