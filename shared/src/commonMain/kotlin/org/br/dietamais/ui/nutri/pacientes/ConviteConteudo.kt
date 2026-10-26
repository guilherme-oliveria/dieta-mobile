package org.br.dietamais.ui.nutri.pacientes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

@Composable
fun ConviteConteudo(estado: EstadoPacientes, aoCompartilhar: (String) -> Unit, aoFechar: () -> Unit) {
    val convite = estado.convite ?: return
    val data = convite.expiraEm.toLocalDateTime(TimeZone.currentSystemDefault())
    val validade = "${data.day.toString().padStart(2, '0')}/${(data.month.ordinal + 1).toString().padStart(2, '0')}/${data.year} ${data.hour.toString().padStart(2, '0')}:${data.minute.toString().padStart(2, '0')}"
    val texto = stringResource(Res.string.pacientes_texto_convite, convite.link, convite.codigo, validade)
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(Dimens.margemCompacta), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
        Text(stringResource(Res.string.pacientes_convite_titulo), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(Res.string.pacientes_convite_descricao), style = MaterialTheme.typography.bodyMedium)
        SelectionContainer { Text(stringResource(Res.string.pacientes_codigo, convite.codigo), style = MaterialTheme.typography.titleLarge) }
        SelectionContainer { Text(stringResource(Res.string.pacientes_link, convite.link), style = MaterialTheme.typography.bodyMedium) }
        Text(stringResource(Res.string.pacientes_validade, validade), style = MaterialTheme.typography.bodySmall)
        estado.erro?.let { MensagemPaciente(it, alerta = true) }
        estado.aviso?.let { MensagemPaciente(it) }
        BotaoPrincipal(stringResource(Res.string.pacientes_compartilhar), { aoCompartilhar(texto) }, Modifier.fillMaxWidth(), carregando = estado.envioEmAndamento)
        BotaoTexto(stringResource(Res.string.auth_fechar), aoFechar, Modifier.fillMaxWidth())
    }
}
