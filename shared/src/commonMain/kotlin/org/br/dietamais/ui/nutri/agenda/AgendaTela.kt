package org.br.dietamais.ui.nutri.agenda

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.br.dietamais.data.remoto.dto.ConsultaResposta
import org.br.dietamais.data.remoto.dto.StatusConsulta
import org.br.dietamais.dominio.agenda.*
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource
import kotlin.uuid.Uuid

data class AcoesAgenda(val aoModo: (ModoAgenda) -> Unit = {}, val aoAnterior: () -> Unit = {}, val aoProximo: () -> Unit = {}, val aoHoje: () -> Unit = {}, val aoNova: () -> Unit = {},
    val aoRemarcar: (Uuid) -> Unit = {}, val aoCancelar: (Uuid) -> Unit = {}, val aoAtualizar: () -> Unit = {})

/** Aba Agenda da nutri: dia ou semana, com criar, remarcar e cancelar (MOB-26). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AgendaTela(estado: EstadoAgenda, acoes: AcoesAgenda, fuso: TimeZone = TimeZone.currentSystemDefault()) {
    val online = !estado.semConexao && !estado.enviando
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Dimens.paddingCard), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
        Text(stringResource(Res.string.agenda_titulo), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        if (estado.semConexao) FaixaOffline()
        estado.erro?.let { Text(textoMensagemAgenda(it), color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }) }
        estado.aviso?.let { Text(textoMensagemAgenda(it), color = MaterialTheme.colorScheme.primary, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno), verticalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno), itemVerticalAlignment = Alignment.CenterVertically) {
            ModoAgenda.entries.forEach { modo ->
                FilterChip(estado.modo == modo, { acoes.aoModo(modo) }, label = { Text(stringResource(if (modo == ModoAgenda.DIA) Res.string.agenda_dia else Res.string.agenda_semana)) })
            }
            BotaoPrincipal(stringResource(Res.string.agenda_nova), acoes.aoNova, habilitado = online && estado.pacientesAgendaveis.isNotEmpty())
            BotaoTexto(stringResource(Res.string.agenda_atualizar), acoes.aoAtualizar, habilitado = !estado.atualizando && !estado.semConexao)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
            BotaoContornado(stringResource(Res.string.agenda_anterior), acoes.aoAnterior)
            Text(rotuloPeriodo(estado), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 2)
            BotaoContornado(stringResource(Res.string.agenda_proximo), acoes.aoProximo)
        }
        BotaoTexto(stringResource(Res.string.agenda_hoje), acoes.aoHoje)
        if (estado.pacientesAgendaveis.isEmpty() && estado.consultas.isEmpty()) Text(stringResource(Res.string.agenda_form_sem_pacientes), style = MaterialTheme.typography.bodyMedium)
        when (estado.modo) {
            ModoAgenda.DIA -> {
                val doDia = consultasDoDia(estado.consultas, estado.dia, fuso)
                if (doDia.isEmpty()) EstadoVazio(mensagem = stringResource(Res.string.agenda_vazio_dia))
                else doDia.forEach { CartaoConsulta(it, estado, acoes, online, fuso) }
            }
            ModoAgenda.SEMANA -> consultasDaSemana(estado.consultas, estado.dia, fuso).forEach { (dia, lista) ->
                Text(rotuloDia(dia), style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
                if (lista.isEmpty()) Text(stringResource(Res.string.agenda_vazio_semana_dia), style = MaterialTheme.typography.bodySmall)
                else lista.forEach { CartaoConsulta(it, estado, acoes, online, fuso) }
            }
        }
    }
}

@Composable
private fun rotuloDia(dia: LocalDate) = nomeDiaSemana(dia.dayOfWeek) + " " + formatarData(dia)

@Composable
private fun rotuloPeriodo(estado: EstadoAgenda): String = when (estado.modo) {
    ModoAgenda.DIA -> rotuloDia(estado.dia)
    ModoAgenda.SEMANA -> diasDaSemana(estado.dia).let { formatarData(it.first()) + " – " + formatarData(it.last()) }
}

@Composable
private fun CartaoConsulta(c: ConsultaResposta, estado: EstadoAgenda, acoes: AcoesAgenda, online: Boolean, fuso: TimeZone) {
    val cancelada = c.status == StatusConsulta.CANCELADA
    CardDieta(Modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.agenda_horario, formatarHora(c.inicio, fuso), formatarHora(c.fim, fuso)), style = MaterialTheme.typography.titleMedium)
        Text(estado.nomeDo(c.pacienteId) ?: stringResource(Res.string.agenda_paciente_desconhecido), style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
            ChipStatus(EstiloStatus.INFO, texto = rotuloTipoConsulta(c.tipo))
            if (cancelada) ChipStatus(EstiloStatus.ALERTA, texto = stringResource(Res.string.agenda_cancelada))
        }
        c.lugar?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        if (!cancelada) Row(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
            BotaoContornado(stringResource(Res.string.agenda_remarcar), { acoes.aoRemarcar(c.id) }, habilitado = online)
            BotaoTexto(stringResource(Res.string.agenda_cancelar), { acoes.aoCancelar(c.id) }, habilitado = online)
        }
    }
}
