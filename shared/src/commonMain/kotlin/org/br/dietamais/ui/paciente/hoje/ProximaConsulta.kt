package org.br.dietamais.ui.paciente.hoje

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import org.br.dietamais.data.remoto.dto.ConsultaResposta
import org.br.dietamais.data.repositorio.PortaConsultas
import org.br.dietamais.dominio.agenda.diaLocal
import org.br.dietamais.dominio.agenda.formatarData
import org.br.dietamais.dominio.agenda.formatarHora
import org.br.dietamais.dominio.agenda.proximaConsulta
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.CardDieta
import org.br.dietamais.ui.nutri.agenda.rotuloTipoConsulta
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.Instant

/** Consultas do paciente, lidas do banco local (o sync as traz); a escolha da próxima fica por conta de quem desenha. */
class ConsultasPacienteViewModel(consultas: PortaConsultas, dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate) : ViewModel() {
    private val atual = MutableStateFlow<List<ConsultaResposta>>(emptyList())
    val consultas = atual.asStateFlow()

    init {
        viewModelScope.launch(dispatcher) { consultas.observar().catch { }.collect { atual.value = it } }
    }
}

/** Cartão da próxima consulta no Hoje; some quando não há consulta marcada. */
@Composable
fun ProximaConsultaCard(consultas: List<ConsultaResposta>, agora: Instant = Clock.System.now(), fuso: TimeZone = TimeZone.currentSystemDefault(), modifier: Modifier = Modifier) {
    val proxima = proximaConsulta(consultas, agora) ?: return
    CardDieta(modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.agenda_prox_titulo), style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
        Text(stringResource(Res.string.agenda_prox_quando, formatarData(diaLocal(proxima.inicio, fuso)), formatarHora(proxima.inicio, fuso), rotuloTipoConsulta(proxima.tipo)),
            style = MaterialTheme.typography.bodyLarge)
        proxima.lugar?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
fun ProximaConsultaRota(viewModel: ConsultasPacienteViewModel, modifier: Modifier = Modifier) {
    val lista by viewModel.consultas.collectAsState()
    ProximaConsultaCard(lista, modifier = modifier.padding(horizontal = Dimens.paddingCard, vertical = Dimens.espacoPequeno))
}
