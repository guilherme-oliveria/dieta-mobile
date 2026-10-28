package org.br.dietamais.ui.nutri.agenda

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.br.dietamais.data.remoto.ErroApi
import org.br.dietamais.data.remoto.FalhaComunicacao
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.dominio.agenda.ErroConsulta
import kotlin.test.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class AgendaViewModelTest {
    private val fuso = TimeZone.of("America/Sao_Paulo")
    private val ana = pacienteFalso("Ana"); private val bia = pacienteFalso("Bia"); private val antiga = pacienteFalso("Carla", StatusVinculo.ENCERRADO)
    private fun erro(codigo: String) = ErroApi(codigo, null, emptyList(), 400, null, null)
    private fun TestScope.vm(c: PortaConsultasFalsa, online: Boolean = true) =
        AgendaViewModel(c, PortaPacientesFalsa(listOf(ana, bia, antiga)), StandardTestDispatcher(testScheduler), flowOf(online), { agoraFalso }, { fuso })
    private fun AgendaViewModel.preencher(paciente: PacienteResposta = ana, data: String = "29/10/2026", hora: String = "14:30") {
        nova(); mudarPaciente(paciente.id); mudarData(data); mudarHora(hora); mudarDuracao("50")
    }

    @Test fun comecaNoDiaDeHojeNoFusoLocalEBuscaAJanela() = runTest {
        val c = PortaConsultasFalsa(); val vm = vm(c); vm.abrir(); advanceUntilIdle()
        assertEquals(LocalDate(2026, 10, 28), vm.estado.value.dia); assertEquals(1, c.atualizacoes); assertFalse(vm.estado.value.atualizando)
        vm.abrir(); advanceUntilIdle(); assertEquals(1, c.atualizacoes) // já carregou
    }

    @Test fun navegaPorDiaOuSemanaEVoltaParaHoje() = runTest {
        val vm = vm(PortaConsultasFalsa()); advanceUntilIdle()
        vm.proximo(); assertEquals(LocalDate(2026, 10, 29), vm.estado.value.dia)
        vm.mudarModo(ModoAgenda.SEMANA); vm.proximo(); assertEquals(LocalDate(2026, 11, 5), vm.estado.value.dia)
        vm.anterior(); vm.anterior(); assertEquals(LocalDate(2026, 10, 22), vm.estado.value.dia)
        vm.hoje(); assertEquals(LocalDate(2026, 10, 28), vm.estado.value.dia)
    }

    @Test fun navegarMuitoLongeRecarregaAJanela() = runTest {
        val c = PortaConsultasFalsa(); val vm = vm(c); vm.abrir(); advanceUntilIdle()
        vm.irPara(LocalDate(2026, 11, 10)); advanceUntilIdle(); assertEquals(1, c.atualizacoes)
        vm.irPara(LocalDate(2026, 12, 20)); advanceUntilIdle(); assertEquals(2, c.atualizacoes)
    }

    @Test fun agendaComIdEstavelEMostraNoDiaDaConsulta() = runTest {
        val c = PortaConsultasFalsa(); val vm = vm(c); advanceUntilIdle()
        vm.preencher(); vm.mudarLugar("  Sala 2 "); vm.salvar(); advanceUntilIdle()
        val req = assertNotNull(c.criada)
        assertEquals(ana.id, req.pacienteId); assertEquals(Instant.parse("2026-10-29T17:30:00Z"), req.inicio); assertEquals(50, req.duracaoMin); assertEquals("Sala 2", req.lugar)
        assertEquals(c.dados.value.single().id, req.id)
        assertNull(vm.estado.value.formulario); assertEquals(MensagemAgenda.AGENDADA, vm.estado.value.aviso); assertEquals(LocalDate(2026, 10, 29), vm.estado.value.dia)
        assertEquals("Ana", vm.estado.value.nomeDo(c.dados.value.single().pacienteId))
    }

    @Test fun naoEnviaComCamposInvalidosENaoOferecePacienteEncerrado() = runTest {
        val c = PortaConsultasFalsa(); val vm = vm(c); advanceUntilIdle()
        assertEquals(listOf("Ana", "Bia"), vm.estado.value.pacientesAgendaveis.map { it.nome })
        vm.nova(); vm.mudarHora("99:00"); vm.salvar(); advanceUntilIdle()
        assertNull(c.criada)
        assertEquals(setOf(ErroConsulta.PACIENTE, ErroConsulta.HORA), vm.estado.value.errosFormulario) // a data já vem preenchida com o dia aberto
        vm.mudarHora("10:00"); assertTrue(vm.estado.value.errosFormulario.isEmpty()) // editar limpa o erro
    }

    @Test fun horarioPassadoEBarradoNoAparelho() = runTest {
        val c = PortaConsultasFalsa(); val vm = vm(c); advanceUntilIdle()
        vm.preencher(data = "28/10/2026", hora = "08:00"); vm.salvar(); advanceUntilIdle()
        assertNull(c.criada); assertTrue(ErroConsulta.PASSADO in vm.estado.value.errosFormulario)
    }

    @Test fun sobreposicaoPedeConfirmacaoComSegundoToque() = runTest {
        val existente = consultaFalsa(bia.id, Instant.parse("2026-10-29T17:00:00Z"), 60)
        val c = PortaConsultasFalsa(listOf(existente)); val vm = vm(c); advanceUntilIdle()
        vm.preencher(); vm.salvar(); advanceUntilIdle()
        assertNull(c.criada); assertEquals(1, vm.estado.value.conflitosAvisados)
        vm.salvar(); advanceUntilIdle()
        assertNotNull(c.criada)
    }

    @Test fun mudarCampoDepoisDoAvisoExigeConfirmarDeNovo() = runTest {
        val c = PortaConsultasFalsa(listOf(consultaFalsa(bia.id, Instant.parse("2026-10-29T17:00:00Z"), 60))); val vm = vm(c); advanceUntilIdle()
        vm.preencher(); vm.salvar(); advanceUntilIdle(); assertEquals(1, vm.estado.value.conflitosAvisados)
        vm.mudarHora("14:45"); assertEquals(0, vm.estado.value.conflitosAvisados)
        vm.mudarHora("18:30"); vm.salvar(); advanceUntilIdle() // sem sobreposição, grava direto
        assertNotNull(c.criada)
    }

    @Test fun semConexaoNaoAgendaMasMostraAAgendaLocal() = runTest {
        val c = PortaConsultasFalsa(listOf(consultaFalsa(ana.id, agoraFalso + 2.hours))); c.falha = FalhaComunicacao()
        val vm = vm(c, online = false); vm.abrir(); advanceUntilIdle()
        c.falha = null
        vm.preencher(); vm.salvar(); advanceUntilIdle()
        assertNull(c.criada); assertEquals(1, vm.estado.value.consultas.size); assertTrue(vm.estado.value.semConexao)
    }

    @Test fun remarcarMandaSoOQueMudouComAVersaoQueTem() = runTest {
        val original = consultaFalsa(ana.id, Instant.parse("2026-10-29T17:30:00Z"), 50, versao = 4)
        val c = PortaConsultasFalsa(listOf(original)); val vm = vm(c); advanceUntilIdle()
        vm.remarcar(original.id); assertEquals("14:30", vm.estado.value.formulario?.hora)
        vm.mudarHora("15:00"); vm.salvar(); advanceUntilIdle()
        val (id, versao, pedido) = assertNotNull(c.alterada)
        assertEquals(original.id, id); assertEquals(4L, versao)
        assertEquals(Instant.parse("2026-10-29T18:00:00Z"), pedido.inicio); assertNull(pedido.duracaoMin); assertNull(pedido.tipo); assertNull(pedido.lugar); assertNull(pedido.status)
        assertEquals(MensagemAgenda.REMARCADA, vm.estado.value.aviso)
    }

    @Test fun remarcarSemMudarNadaNaoChamaAApi() = runTest {
        val original = consultaFalsa(ana.id, Instant.parse("2026-10-29T17:30:00Z"), 50)
        val c = PortaConsultasFalsa(listOf(original)); val vm = vm(c); advanceUntilIdle()
        vm.remarcar(original.id); vm.salvar(); advanceUntilIdle()
        assertNull(c.alterada); assertNull(vm.estado.value.formulario)
    }

    @Test fun naoRemarcaConsultaCancelada() = runTest {
        val cancelada = consultaFalsa(ana.id, agoraFalso + 3.hours, status = StatusConsulta.CANCELADA)
        val vm = vm(PortaConsultasFalsa(listOf(cancelada))); advanceUntilIdle()
        vm.remarcar(cancelada.id); assertNull(vm.estado.value.formulario)
    }

    @Test fun cancelaSoDepoisDeConfirmar() = runTest {
        val original = consultaFalsa(ana.id, agoraFalso + 3.hours, versao = 2)
        val c = PortaConsultasFalsa(listOf(original)); val vm = vm(c); advanceUntilIdle()
        vm.pedirCancelamento(original.id); assertNull(c.alterada)
        vm.desistirDoCancelamento(); assertNull(vm.estado.value.cancelandoId)
        vm.pedirCancelamento(original.id); vm.confirmarCancelamento(); advanceUntilIdle()
        val (_, versao, pedido) = assertNotNull(c.alterada)
        assertEquals(2L, versao); assertEquals(StatusConsulta.CANCELADA, pedido.status)
        assertEquals(MensagemAgenda.CANCELADA, vm.estado.value.aviso); assertNull(vm.estado.value.cancelandoId)
        assertEquals(StatusConsulta.CANCELADA, vm.estado.value.consultas.single().status)
    }

    @Test fun conflitoDeVersaoFechaOFormularioERecarrega() = runTest {
        val original = consultaFalsa(ana.id, Instant.parse("2026-10-29T17:30:00Z"))
        val c = PortaConsultasFalsa(listOf(original)); val vm = vm(c); advanceUntilIdle()
        vm.remarcar(original.id); vm.mudarHora("16:00"); c.falha = erro("CONFLITO_VERSAO"); vm.salvar(); advanceUntilIdle()
        assertEquals(MensagemAgenda.CONFLITO, vm.estado.value.erro); assertNull(vm.estado.value.formulario); assertEquals(1, c.atualizacoes)
    }

    @Test fun errosDoServidorViramMensagens() = runTest {
        val c = PortaConsultasFalsa(); val vm = vm(c); advanceUntilIdle()
        for ((codigo, mensagem) in listOf("ACESSO_NEGADO" to MensagemAgenda.VINCULO_ENCERRADO, "ASSINATURA_INATIVA" to MensagemAgenda.ASSINATURA_INATIVA,
            "FORMATO_INVALIDO" to MensagemAgenda.DATA_INVALIDA, "RECURSO_NAO_ENCONTRADO" to MensagemAgenda.NAO_ENCONTRADA)) {
            c.falha = erro(codigo); vm.preencher(); vm.salvar(); advanceUntilIdle()
            assertEquals(mensagem, vm.estado.value.erro, codigo); assertNotNull(vm.estado.value.formulario); vm.fechar()
        }
    }
}
