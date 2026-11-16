package org.br.dietamais.ui.nutri.avaliacao

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.br.dietamais.data.remoto.ErroApi
import org.br.dietamais.data.remoto.ErroCampoResposta
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.PortaAvaliacoes
import org.br.dietamais.dominio.avaliacao.*
import kotlin.test.*
import kotlin.time.Instant
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class AvaliacoesViewModelTest {
    private val paciente = Uuid.random()
    private val agora = Instant.parse("2026-10-28T12:00:00Z")
    private fun item(dias: Long, peso: Double, compartilhada: Boolean = true, criadoDias: Long = dias): AvaliacaoResposta {
        val quando = agora - kotlin.time.Duration.parse("${dias * 24}h"); val criada = agora - kotlin.time.Duration.parse("${criadoDias * 24}h")
        return AvaliacaoResposta(Uuid.random(), paciente, Uuid.random(), quando, peso, 1.75, imcDe(peso, 1.75), classificarImc(imcDe(peso, 1.75)),
            compartilhada = compartilhada, versao = 1, criadoEm = criada, atualizadoEm = criada)
    }
    private class Porta(inicial: List<AvaliacaoResposta> = emptyList()) : PortaAvaliacoes {
        val lista = MutableStateFlow(inicial); var falha: Exception? = null
        var criada: SalvarAvaliacaoRequisicao? = null; var salva: Pair<Long, SalvarAvaliacaoRequisicao>? = null; var atualizacoes = 0
        override fun observar(pacienteId: Uuid?) = lista
        override suspend fun atualizar(pacienteId: Uuid) { atualizacoes++; falha?.let { throw it } }
        override suspend fun criar(pacienteId: Uuid, requisicao: SalvarAvaliacaoRequisicao): AvaliacaoResposta {
            falha?.let { throw it }; criada = requisicao
            return AvaliacaoResposta(requisicao.id!!, pacienteId, Uuid.random(), requisicao.dataHora, requisicao.pesoKg, requisicao.estaturaM, 22.86, ClassificacaoImc.NORMAL,
                versao = 1, criadoEm = requisicao.dataHora, atualizadoEm = requisicao.dataHora).also { lista.value = lista.value + it }
        }
        override suspend fun salvar(id: Uuid, versao: Long, requisicao: SalvarAvaliacaoRequisicao): AvaliacaoResposta {
            falha?.let { throw it }; salva = versao to requisicao
            return lista.value.first { it.id == id }.copy(pesoKg = requisicao.pesoKg, versao = versao + 1)
        }
    }
    private fun TestScope.vm(p: Porta, online: Boolean = true) = AvaliacoesViewModel(p, StandardTestDispatcher(testScheduler), flowOf(online), { agora })
    private fun AvaliacoesViewModel.preencher() { mudarMedida(MedidaAvaliacao.PESO, "70,5"); mudarMedida(MedidaAvaliacao.ESTATURA, "1,75") }
    private fun AvaliacoesViewModel.preencherComCintura() { preencher(); mudarMedida(MedidaAvaliacao.CINTURA, "80") }

    @Test fun abreCarregaHistoricoDoBancoEAtualizaDoServidor() = runTest {
        val p = Porta(listOf(item(1, 70.0)));val vm = vm(p); vm.abrir(paciente); advanceUntilIdle()
        assertEquals(1, vm.estado.value.itens.size); assertEquals(1, p.atualizacoes); assertFalse(vm.estado.value.atualizando)
    }

    @Test fun criaComIdEstavelParaReenvioIdempotente() = runTest {
        val p = Porta(); val vm = vm(p); vm.abrir(paciente); advanceUntilIdle()
        vm.nova(); vm.preencher(); vm.mudarParecer("  Ótimo  "); vm.salvar(); advanceUntilIdle()
        val enviado = assertNotNull(p.criada)
        assertEquals(vm.estado.value.itens.single().id, enviado.id); assertEquals(70.5, enviado.pesoKg); assertEquals("Ótimo", enviado.parecer)
        assertNull(vm.estado.value.formulario); assertEquals(MensagemAvaliacao.SALVO, vm.estado.value.aviso)
    }

    @Test fun naoEnviaComMedidaInvalida() = runTest {
        val p = Porta(); val vm = vm(p); vm.abrir(paciente); advanceUntilIdle()
        vm.nova(); vm.mudarMedida(MedidaAvaliacao.PESO, "19"); vm.salvar(); advanceUntilIdle()
        assertNull(p.criada); assertEquals(ErroMedida.FORA_DO_INTERVALO, vm.estado.value.errosMedidas[MedidaAvaliacao.PESO]); assertEquals(ErroMedida.OBRIGATORIA, vm.estado.value.errosMedidas[MedidaAvaliacao.ESTATURA])
    }

    @Test fun previaDoImcSoApareceComMedidasValidas() = runTest {
        val vm = vm(Porta()); vm.abrir(paciente); advanceUntilIdle(); vm.nova()
        assertNull(vm.previaImc()); vm.preencher()
        val previa = assertNotNull(vm.previaImc()); assertEquals(23.02, previa.first); assertEquals(ClassificacaoImc.NORMAL, previa.second)
    }

    @Test fun edicaoDepoisDe30DiasExigeJustificativa() = runTest {
        val antiga = item(40, 80.0); val p = Porta(listOf(antiga)); val vm = vm(p); vm.abrir(paciente); advanceUntilIdle()
        vm.editar(antiga.id); assertTrue(vm.estado.value.formulario!!.exigeJustificativa)
        vm.mudarMedida(MedidaAvaliacao.PESO, "79"); vm.salvar(); advanceUntilIdle()
        assertTrue(vm.estado.value.justificativaFaltando); assertNull(p.salva)
        vm.mudarJustificativa(" Erro de digitação "); vm.salvar(); advanceUntilIdle()
        assertEquals(1L, p.salva?.first); assertEquals("Erro de digitação", p.salva?.second?.justificativa); assertNull(vm.estado.value.formulario)
    }

    @Test fun edicaoRecenteNaoPedeJustificativa() = runTest {
        val recente = item(5, 80.0); val p = Porta(listOf(recente)); val vm = vm(p); vm.abrir(paciente); advanceUntilIdle()
        vm.editar(recente.id); assertFalse(vm.estado.value.formulario!!.exigeJustificativa)
        vm.salvar(); advanceUntilIdle(); assertNull(p.salva?.second?.justificativa)
    }

    @Test fun servidorPedeJustificativaMesmoSemRelogioCerto() = runTest {
        val a = item(5, 80.0); val p = Porta(listOf(a)); val vm = vm(p); vm.abrir(paciente); advanceUntilIdle()
        vm.editar(a.id); p.falha = ErroApi("JUSTIFICATIVA_OBRIGATORIA", "RN-AVA-04", emptyList(), 422, null, null); vm.salvar(); advanceUntilIdle()
        assertTrue(vm.estado.value.formulario!!.exigeJustificativa); assertTrue(vm.estado.value.justificativaFaltando)
    }

    @Test fun erroDeFaixaDoServidorMarcaOCampo() = runTest {
        val p = Porta(); val vm = vm(p); vm.abrir(paciente); advanceUntilIdle()
        vm.nova(); vm.preencher(); p.falha = ErroApi("VALOR_FORA_DO_INTERVALO", "RN-AVA-03", listOf(ErroCampoResposta("perimetros.cintura", "VALOR_FORA_DO_INTERVALO", "x")), 422, null, null)
        vm.salvar(); advanceUntilIdle()
        assertEquals(ErroMedida.FORA_DO_INTERVALO, vm.estado.value.errosMedidas[MedidaAvaliacao.CINTURA]); assertNotNull(vm.estado.value.formulario)
    }

    @Test fun conflitoFechaOFormularioERecarrega() = runTest {
        val a = item(5, 80.0); val p = Porta(listOf(a)); val vm = vm(p); vm.abrir(paciente); advanceUntilIdle()
        vm.editar(a.id); p.falha = ErroApi("CONFLITO_VERSAO", "RN-SYN-02", emptyList(), 409, null, null); vm.salvar(); advanceUntilIdle()
        assertEquals(MensagemAvaliacao.CONFLITO, vm.estado.value.erro); assertNull(vm.estado.value.formulario); assertEquals(2, p.atualizacoes)
    }

    @Test fun semConexaoNaoEnviaESelecaoComparaDuasDatas() = runTest {
        val a = item(10, 80.0); val b = item(2, 78.0); val c = item(1, 77.0)
        val p = Porta(listOf(a, b, c)).also { it.falha = org.br.dietamais.data.remoto.FalhaComunicacao() }
        val vm = vm(p, online = false); vm.abrir(paciente); advanceUntilIdle()
        vm.nova(); vm.preencher(); vm.salvar(); advanceUntilIdle(); assertTrue(vm.estado.value.semConexao)
        vm.alternarSelecao(a.id); vm.alternarSelecao(b.id); vm.alternarSelecao(c.id) // a terceira troca a mais antiga da seleção
        assertEquals(listOf(b.id, c.id), vm.estado.value.selecionadas)
        vm.comparar(); assertTrue(vm.estado.value.comparando); assertEquals(b.id to c.id, vm.estado.value.comparacao!!.let { it.first.id to it.second.id })
    }

    @Test fun pacienteVeSoCompartilhadasSemEditar() = runTest {
        val p = Porta(listOf(item(3, 70.0), item(2, 71.0, compartilhada = false))); val vm = vm(p); vm.abrirComoPaciente(); advanceUntilIdle()
        assertTrue(vm.estado.value.somenteLeitura); assertEquals(1, vm.estado.value.itens.size); assertEquals(0, p.atualizacoes)
    }

    @Test fun passoUmNaoAvancaSemPesoEEstaturaEOResumoVemPreenchido() = runTest {
        val p = Porta(); val vm = vm(p); vm.abrir(paciente); advanceUntilIdle()
        vm.definirPaciente(Sexo.MASCULINO, kotlinx.datetime.LocalDate(1997, 4, 28))
        vm.nova(); vm.proximoPasso()
        assertEquals(PassoAvaliacao.MEDIDAS, vm.estado.value.formulario?.passo); assertEquals(ErroMedida.OBRIGATORIA, vm.estado.value.errosMedidas[MedidaAvaliacao.PESO])
        vm.preencherComCintura(); vm.proximoPasso()
        assertEquals(PassoAvaliacao.COMPOSICAO, vm.estado.value.formulario?.passo); assertTrue(vm.estado.value.errosMedidas.isEmpty())
        vm.proximoPasso() // composição nunca bloqueia
        assertEquals(PassoAvaliacao.NECESSIDADES, vm.estado.value.formulario?.passo)
        vm.proximoPasso() // necessidades também não
        val form = assertNotNull(vm.estado.value.formulario)
        assertEquals(PassoAvaliacao.RESUMO, form.passo); assertTrue(form.parecer.startsWith("O avaliado apresenta"), form.parecer)
        vm.mudarParecer("Meu texto"); vm.passoAnterior(); vm.proximoPasso()
        assertEquals("Meu texto", vm.estado.value.formulario?.parecer) // não sobrescreve o que a nutri escreveu
    }

    @Test fun salvaProtocoloComplementaresECompartilhamentoPorSecao() = runTest {
        val p = Porta(); val vm = vm(p); vm.abrir(paciente); advanceUntilIdle()
        vm.nova(); vm.preencher(); vm.mudarMedida(MedidaAvaliacao.PUNHO, "17,5")
        vm.mudarProtocoloDobras(org.br.dietamais.dominio.calculos.ProtocoloDobras.TRES); vm.mudarProtocoloMassa(org.br.dietamais.dominio.calculos.ProtocoloMassa.MUSCULAR)
        vm.mudarCompartilhamento(composicao = false)
        vm.salvar(); advanceUntilIdle()
        val enviado = assertNotNull(p.criada)
        assertEquals("TRES", enviado.protocolo?.dobras); assertEquals("MUSCULAR", enviado.protocolo?.massa)
        assertEquals(17.5, enviado.complementares?.punhoCm); assertEquals(false, enviado.compartilhamento?.composicao); assertNull(enviado.compartilhamento?.medidas)
    }

    @Test fun salvaAsEscolhasDeEnergiaEReabreComElas() = runTest {
        val p = Porta(); val vm = vm(p); vm.abrir(paciente); advanceUntilIdle()
        vm.nova(); vm.preencher()
        vm.mudarEnergia(org.br.dietamais.dominio.avaliacao.EscolhasEnergia(nivel = org.br.dietamais.dominio.calculos.NivelAtividade.MODERADO,
            carboidrato = org.br.dietamais.dominio.calculos.ProtocoloCarboidrato.FAIXA_ESPORTIVA, objetivo = org.br.dietamais.dominio.calculos.ObjetivoPeso.PERDA, kgPorMes = "2"))
        vm.salvar(); advanceUntilIdle()
        val enviado = assertNotNull(p.criada)
        assertEquals("FAO_2004", enviado.energia?.metodo); assertEquals("FAIXA_ESPORTIVA", enviado.energia?.carboidrato)
        assertEquals("MODERADO", enviado.complementares?.nivelAtividade); assertEquals("PERDA", enviado.complementares?.objetivo); assertEquals(2.0, enviado.complementares?.objetivoKgPorMes)
        val gravada = item(9, 70.0).copy(energia = enviado.energia, complementares = enviado.complementares)
        val vm2 = vm(Porta(listOf(gravada))); vm2.abrir(paciente); advanceUntilIdle(); vm2.editar(gravada.id)
        val e = assertNotNull(vm2.estado.value.formulario).energia
        assertEquals(org.br.dietamais.dominio.calculos.ProtocoloCarboidrato.FAIXA_ESPORTIVA, e.carboidrato); assertEquals("2", e.kgPorMes)
    }

    @Test fun avaliacaoSemEscolhaDeEnergiaNaoEnviaOBloco() = runTest {
        val p = Porta(); val vm = vm(p); vm.abrir(paciente); advanceUntilIdle()
        vm.nova(); vm.preencher(); vm.salvar(); advanceUntilIdle()
        assertNull(assertNotNull(p.criada).energia)
    }

    @Test fun avaliacaoNovaUsaOsProtocolosPadraoDaNutriSeAindaNaoMexeu() = runTest {
        val vm = AvaliacoesViewModel(Porta(), StandardTestDispatcher(testScheduler), flowOf(true), { agora },
            protocolosPadrao = { org.br.dietamais.dominio.calculos.ProtocoloDobras.TRES to org.br.dietamais.dominio.calculos.ProtocoloMassa.MUSCULAR })
        vm.abrir(paciente); advanceUntilIdle()
        vm.nova(); advanceUntilIdle()
        assertEquals(org.br.dietamais.dominio.calculos.ProtocoloDobras.TRES, vm.estado.value.formulario?.protocoloDobras)
        assertEquals(org.br.dietamais.dominio.calculos.ProtocoloMassa.MUSCULAR, vm.estado.value.formulario?.protocoloMassa)
        val semPadrao = vm(Porta()); semPadrao.abrir(paciente); advanceUntilIdle(); semPadrao.nova(); advanceUntilIdle()
        assertEquals(org.br.dietamais.dominio.calculos.ProtocoloDobras.SETE, semPadrao.estado.value.formulario?.protocoloDobras)
    }

    @Test fun repetirMedidasCopiaDaUltimaSemTocarNaComposicao() = runTest {
        val ultima = item(7, 80.0).copy(perimetros = PerimetrosAvaliacao(cintura = 90.0), complementares = MedidasComplementaresDto(punhoCm = 17.0),
            composicao = ComposicaoAvaliacao(percentualGordura = 20.0))
        val vm = vm(Porta(listOf(ultima))); vm.abrir(paciente); advanceUntilIdle()
        vm.nova(); vm.repetirMedidasDaUltima()
        val v = assertNotNull(vm.estado.value.formulario).valores
        assertEquals("80", v[MedidaAvaliacao.PESO]); assertEquals("90", v[MedidaAvaliacao.CINTURA]); assertEquals("17", v[MedidaAvaliacao.PUNHO])
        assertNull(v[MedidaAvaliacao.PERCENTUAL_GORDURA])
    }
}
