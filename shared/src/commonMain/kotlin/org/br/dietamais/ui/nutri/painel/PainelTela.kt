package org.br.dietamais.ui.nutri.painel

import androidx.compose.foundation.clickable
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
import kotlinx.datetime.TimeZone
import org.br.dietamais.app.SolicitacoesNavegacao
import org.br.dietamais.data.remoto.dto.ConsultaResposta
import org.br.dietamais.data.remoto.dto.StatusAssinatura
import org.br.dietamais.dominio.agenda.formatarHora
import org.br.dietamais.dominio.painel.ItemPainel
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.nutri.agenda.rotuloTipoConsulta
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource
import kotlin.uuid.Uuid

data class AcoesPainel(val aoAtualizar: () -> Unit = {}, val aoAbrirPaciente: (Uuid) -> Unit = {}, val aoAbrirAgenda: () -> Unit = {})

@Composable
fun PainelTela(estado: EstadoPainel, acoes: AcoesPainel, fuso: TimeZone = TimeZone.currentSystemDefault(), modifier: Modifier = Modifier) {
    val r = estado.resumo
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Dimens.margemCompacta), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(Res.string.painel_titulo), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
            BotaoTexto(stringResource(Res.string.painel_atualizar), acoes.aoAtualizar, carregando = estado.atualizando)
        }
        if (estado.semConexao) Text(stringResource(Res.string.painel_offline), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        CardDieta(Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.painel_consultas_hoje), style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
            if (r.consultasHoje.isEmpty()) Text(stringResource(Res.string.painel_sem_consultas), style = MaterialTheme.typography.bodyMedium)
            r.consultasHoje.forEach { LinhaConsulta(it, estado.nomeDo(it), fuso) { acoes.aoAbrirPaciente(it.pacienteId) } }
            BotaoTexto(stringResource(Res.string.painel_abrir_agenda), acoes.aoAbrirAgenda)
        }
        SecaoPacientes(stringResource(Res.string.painel_sem_checkin), stringResource(Res.string.painel_sem_checkin_vazio), r.semCheckin, acoes.aoAbrirPaciente)
        SecaoPacientes(stringResource(Res.string.painel_respondidos), stringResource(Res.string.painel_respondidos_vazio), r.respondidos, acoes.aoAbrirPaciente)
        r.assinatura?.let { a ->
            CardDieta(Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.painel_plano), style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
                Text(stringResource(Res.string.painel_plano_uso, a.pacientesOcupados, a.limitePacientes), style = MaterialTheme.typography.bodyLarge)
                LinearProgressIndicator(progress = { if (a.limitePacientes <= 0) 0f else (a.pacientesOcupados.toFloat() / a.limitePacientes).coerceIn(0f, 1f) }, Modifier.fillMaxWidth())
                if (a.status == StatusAssinatura.EM_ATRASO || a.status == StatusAssinatura.EXPIRADA || a.status == StatusAssinatura.CANCELADA)
                    Text(stringResource(Res.string.painel_plano_atencao), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun LinhaConsulta(c: ConsultaResposta, nome: String?, fuso: TimeZone, aoTocar: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = aoTocar).padding(vertical = Dimens.espacoPequeno)) {
        Text("${formatarHora(c.inicio, fuso)} – ${formatarHora(c.fim, fuso)}", style = MaterialTheme.typography.bodyLarge)
        Text("${nome ?: stringResource(Res.string.painel_paciente)} · ${rotuloTipoConsulta(c.tipo)}", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SecaoPacientes(titulo: String, vazio: String, itens: List<ItemPainel>, aoAbrir: (Uuid) -> Unit) {
    CardDieta(Modifier.fillMaxWidth()) {
        Text(titulo, style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
        if (itens.isEmpty()) Text(vazio, style = MaterialTheme.typography.bodyMedium)
        itens.forEach { Text(it.nome ?: stringResource(Res.string.painel_paciente), style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth().clickable { aoAbrir(it.fichaId) }.padding(vertical = Dimens.espacoPequeno).semantics { role = Role.Button }) }
    }
}

@Composable
fun PainelRota(viewModel: PainelViewModel) {
    val estado by viewModel.estado.collectAsState()
    LaunchedEffect(viewModel) { viewModel.atualizar() }
    PainelTela(estado, AcoesPainel(viewModel::atualizar, { SolicitacoesNavegacao.pedir("/pacientes/$it") }, { SolicitacoesNavegacao.pedir("/agenda") }))
}
