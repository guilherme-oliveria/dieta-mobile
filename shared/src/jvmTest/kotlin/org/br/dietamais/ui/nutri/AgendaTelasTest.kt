package org.br.dietamais.ui.nutri

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.dominio.agenda.FormularioConsulta
import org.br.dietamais.ui.nutri.agenda.*
import org.br.dietamais.ui.paciente.hoje.ProximaConsultaCard
import org.br.dietamais.ui.theme.DietaTheme
import kotlin.test.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlin.uuid.Uuid

@OptIn(ExperimentalTestApi::class)
class AgendaTelasTest {
    private val fuso = TimeZone.of("America/Sao_Paulo")
    private val ana = pacienteFalso("Ana Fictícia")
    private val dia = LocalDate(2026, 10, 28)
    private val consulta = consultaFalsa(ana.id, Instant.parse("2026-10-28T17:30:00Z"), 50, TipoConsulta.ONLINE, lugar = "https://meet.exemplo.invalid/x")

    @Test fun diaMostraConsultaComPacienteHorarioETipo() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        setContent { DietaTheme { AgendaTela(EstadoAgenda(dia, consultas = listOf(consulta), pacientes = listOf(ana)), AcoesAgenda(), fuso) } }
        onNodeWithText("14:30 – 15:20").assertIsDisplayed(); onNodeWithText("Ana Fictícia").assertIsDisplayed(); onNodeWithText("Online").assertIsDisplayed()
        onNodeWithText("Remarcar").assertIsEnabled()
    }

    @Test fun semanaListaOsSeteDiasEDiaVazioTemMensagem() = runSkikoComposeUiTest(size = Size(390f, 1200f)) {
        setContent { DietaTheme { AgendaTela(EstadoAgenda(dia, ModoAgenda.SEMANA, listOf(consulta), listOf(ana)), AcoesAgenda(), fuso) } }
        onNodeWithText("26/10/2026 – 01/11/2026").assertIsDisplayed()
        onNodeWithText("qua 28/10/2026").assertExists(); onAllNodesWithText("Sem consultas").assertCountEquals(6)
    }

    @Test fun offlineDesabilitaAcoesMasMantemALeitura() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        setContent { DietaTheme { AgendaTela(EstadoAgenda(dia, consultas = listOf(consulta), pacientes = listOf(ana), semConexao = true), AcoesAgenda(), fuso) } }
        onNodeWithText("Ana Fictícia").assertIsDisplayed()
        onNodeWithText("Nova consulta").assertIsNotEnabled(); onNodeWithText("Remarcar").assertIsNotEnabled(); onNodeWithText("Cancelar consulta").assertIsNotEnabled()
    }

    @Test fun consultaCanceladaNaoOfereceAcoes() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        val cancelada = consulta.copy(status = StatusConsulta.CANCELADA)
        setContent { DietaTheme { AgendaTela(EstadoAgenda(dia, consultas = listOf(cancelada), pacientes = listOf(ana)), AcoesAgenda(), fuso) } }
        onNodeWithText("Cancelada").assertIsDisplayed(); onNodeWithText("Remarcar").assertDoesNotExist()
    }

    @Test fun botoesChamamAsAcoes() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        var remarcou: Uuid? = null; var cancelou: Uuid? = null; var nova = 0
        setContent { DietaTheme { AgendaTela(EstadoAgenda(dia, consultas = listOf(consulta), pacientes = listOf(ana)), AcoesAgenda(aoNova = { nova++ }, aoRemarcar = { remarcou = it }, aoCancelar = { cancelou = it }), fuso) } }
        onNodeWithText("Nova consulta").performClick(); onNodeWithText("Remarcar").performClick(); onNodeWithText("Cancelar consulta").performClick()
        assertEquals(1, nova); assertEquals(consulta.id, remarcou); assertEquals(consulta.id, cancelou)
    }

    @Test fun formularioNovoMostraPacientesAgendaveisEErros() = runSkikoComposeUiTest(size = Size(390f, 1200f)) {
        val encerrada = pacienteFalso("Zé Encerrado", StatusVinculo.ENCERRADO)
        val estado = EstadoAgenda(dia, pacientes = listOf(ana, encerrada), formulario = FormularioConsulta(data = "xx"),
            errosFormulario = setOf(org.br.dietamais.dominio.agenda.ErroConsulta.PACIENTE, org.br.dietamais.dominio.agenda.ErroConsulta.DATA))
        setContent { DietaTheme { FormularioConsultaTela(estado, AcoesFormularioConsulta()) } }
        onNodeWithText("Ana Fictícia").assertIsDisplayed(); onNodeWithText("Zé Encerrado").assertDoesNotExist()
        onNodeWithText("Escolha o paciente.").assertIsDisplayed(); onNodeWithText("Informe uma data válida (dd/mm/aaaa).").assertIsDisplayed()
    }

    @Test fun formularioOfflineNaoSalva() = runSkikoComposeUiTest(size = Size(390f, 1200f)) {
        val estado = EstadoAgenda(dia, pacientes = listOf(ana), formulario = FormularioConsulta(), semConexao = true)
        setContent { DietaTheme { FormularioConsultaTela(estado, AcoesFormularioConsulta()) } }
        onNodeWithText("Salvar").assertIsNotEnabled()
    }

    @Test fun avisoDeSobreposicaoAparece() = runSkikoComposeUiTest(size = Size(390f, 1400f)) {
        val estado = EstadoAgenda(dia, pacientes = listOf(ana), formulario = FormularioConsulta(), conflitosAvisados = 2)
        setContent { DietaTheme { FormularioConsultaTela(estado, AcoesFormularioConsulta()) } }
        onNodeWithText("Já há 2 consulta(s) nesse horário. Toque em Salvar de novo para agendar mesmo assim.").assertExists()
    }

    @Test fun confirmacaoDeCancelamentoCitaPacienteEHorario() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        var confirmou = 0
        setContent { DietaTheme { ConfirmarCancelamentoDialog(EstadoAgenda(dia, consultas = listOf(consulta), pacientes = listOf(ana), cancelandoId = consulta.id), { confirmou++ }, {}, fuso) } }
        onNodeWithText("Ana Fictícia, 28/10/2026 às 14:30. O paciente passa a ver a consulta como cancelada.").assertIsDisplayed()
        onAllNodesWithText("Cancelar consulta").onLast().performClick()
        assertEquals(1, confirmou)
    }

    @Test fun pacienteVeAProximaConsultaNoHojeESomeSemConsulta() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        var lista by androidx.compose.runtime.mutableStateOf(listOf(consulta))
        setContent { DietaTheme { ProximaConsultaCard(lista, agoraFalso, fuso) } }
        onNodeWithText("Próxima consulta").assertIsDisplayed(); onNodeWithText("28/10/2026 às 14:30 · Online").assertIsDisplayed()
        onNodeWithText("https://meet.exemplo.invalid/x").assertIsDisplayed()
        lista = listOf(consulta.copy(status = StatusConsulta.CANCELADA))
        onNodeWithText("Próxima consulta").assertDoesNotExist()
    }
}
