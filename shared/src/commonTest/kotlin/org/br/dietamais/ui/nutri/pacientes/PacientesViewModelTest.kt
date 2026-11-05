package org.br.dietamais.ui.nutri.pacientes

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.PortaPacientes
import org.br.dietamais.plataforma.Compartilhamento
import org.br.dietamais.plataforma.ResultadoCompartilhamento
import kotlin.test.*
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import org.br.dietamais.dominio.pacientes.FiltroAtencao
import org.br.dietamais.dominio.pacientes.ResumoPaciente

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PacientesViewModelTest {
    private fun paciente(nome: String = "Pessoa fictícia", status: StatusVinculo = StatusVinculo.CONVIDADO) = PacienteResposta(
        Uuid.random(), Uuid.random(), nome, status = status, versao = 0,
        criadoEm = Instant.parse("2026-01-01T00:00:00Z"), atualizadoEm = Instant.parse("2026-01-01T00:00:00Z"))
    private val compartilhar = Compartilhamento { ResultadoCompartilhamento.COPIADO }

    @Test fun buscaEStatusFiltramCacheSemNovaRequisicao() = runTest {
        val porta = PortaTeste(listOf(paciente("Pessoa Alpha", StatusVinculo.ATIVO), paciente("Pessoa Beta")))
        val vm = PacientesViewModel(porta, compartilhar, StandardTestDispatcher(testScheduler))
        runCurrent(); vm.mudarBusca("BETA"); runCurrent()
        assertEquals(listOf("Pessoa Beta"), vm.estado.value.lista.map { it.nome })
        vm.mudarFiltro(StatusVinculo.ATIVO)
        assertTrue(vm.estado.value.lista.isEmpty())
        vm.mudarBusca("")
        assertEquals(listOf("Pessoa Alpha"), vm.estado.value.lista.map { it.nome })
        assertEquals(1, porta.atualizacoes)
    }

    private val agora = Instant.parse("2026-11-05T15:00:00Z")
    private fun vmResumos(porta: PortaTeste, escopo: kotlinx.coroutines.test.TestScope) =
        PacientesViewModel(porta, compartilhar, StandardTestDispatcher(escopo.testScheduler), agora = { agora }, fuso = { kotlinx.datetime.TimeZone.UTC })

    @Test fun resumosAlimentamContagemEFiltrosDeAtencao() = runTest {
        val hoje = paciente("Pessoa Hoje", StatusVinculo.ATIVO); val sem = paciente("Pessoa Sem", StatusVinculo.ATIVO); val ok = paciente("Pessoa Ok", StatusVinculo.ATIVO)
        val porta = PortaTeste(listOf(hoje, sem, ok)).apply { resumosDados = mapOf(
            hoje.vinculoId to ResumoPaciente(agora + 2.hours, agora - 5.days),
            sem.vinculoId to ResumoPaciente(null, agora - 5.days),
            ok.vinculoId to ResumoPaciente(agora + 4.days, agora - 5.days)) }
        val vm = vmResumos(porta, this); runCurrent()
        assertEquals(3, vm.estado.value.resumos?.size)
        assertEquals(1, vm.estado.value.precisamAtencao)
        vm.mudarFiltroAtencao(FiltroAtencao.RETORNO_HOJE)
        assertEquals(listOf("Pessoa Hoje"), vm.estado.value.lista.map { it.nome })
        vm.mudarFiltroAtencao(FiltroAtencao.SEM_RETORNO)
        assertEquals(listOf("Pessoa Sem"), vm.estado.value.lista.map { it.nome })
        // tocar de novo no mesmo chip desliga o filtro
        vm.mudarFiltroAtencao(FiltroAtencao.SEM_RETORNO)
        assertNull(vm.estado.value.filtroAtencao); assertEquals(3, vm.estado.value.lista.size)
        // atenção combina com a busca por nome
        vm.mudarFiltroAtencao(FiltroAtencao.AVALIACAO_VENCIDA); vm.mudarBusca("ok")
        assertTrue(vm.estado.value.lista.isEmpty())
    }

    @Test fun semResumosNaoHaChipsNemContagem() = runTest {
        val porta = PortaTeste(listOf(paciente("Pessoa Alpha", StatusVinculo.ATIVO)))
        val vm = vmResumos(porta, this); runCurrent()
        assertNull(vm.estado.value.resumos); assertEquals(0, vm.estado.value.precisamAtencao)
        vm.mudarFiltroAtencao(FiltroAtencao.SEM_RETORNO)
        assertTrue(vm.estado.value.lista.isEmpty(), "sem dados nenhum paciente se encaixa no filtro")
    }

    @Test fun falhaDoResumoNaoDerrubaAListaEResumoVoltaNaProximaAtualizacao() = runTest {
        val p = paciente("Pessoa Alpha", StatusVinculo.ATIVO)
        val porta = PortaTeste(listOf(p)); val vm = vmResumos(porta, this); runCurrent()
        assertNull(vm.estado.value.resumos); assertEquals(1, vm.estado.value.lista.size); assertNull(vm.estado.value.erro)
        porta.resumosDados = mapOf(p.vinculoId to ResumoPaciente(null, null)); vm.atualizar(); runCurrent()
        assertEquals(1, vm.estado.value.precisamAtencao)
    }

    @Test fun redeIndisponivelMantemCacheEPermiteCadastrarNaFila() = runTest {
        val porta = PortaTeste(listOf(paciente())).apply { falhaAtualizar = FalhaComunicacao() }
        val vm = PacientesViewModel(porta, compartilhar, StandardTestDispatcher(testScheduler))
        runCurrent()
        assertTrue(vm.estado.value.semConexao)
        assertEquals(1, vm.estado.value.lista.size)
        vm.abrirCriacao()
        assertNotNull(vm.estado.value.formulario)
    }

    @Test fun formularioInvalidoNaoChegaAoServidor() = runTest {
        val porta = PortaTeste()
        val vm = PacientesViewModel(porta, compartilhar, StandardTestDispatcher(testScheduler))
        runCurrent(); vm.abrirCriacao(); vm.mudarCampo(CampoPaciente.EMAIL, "invalido")
        vm.mudarCampo(CampoPaciente.NASCIMENTO, "31/02/2000"); vm.criar(); runCurrent()
        assertTrue(porta.criacoes.isEmpty())
        assertEquals(MensagemPacientes.OBRIGATORIO, vm.estado.value.errosCampos["nome"])
        assertEquals(MensagemPacientes.DATA_INVALIDA, vm.estado.value.errosCampos["dataNascimento"])
    }

    @Test fun retentativaDeCadastroPreservaIdEChaveSemDuplicarEnvio() = runTest {
        val porta = PortaTeste().apply { falhaCriar = FalhaComunicacao() }
        val vm = PacientesViewModel(porta, compartilhar, StandardTestDispatcher(testScheduler))
        runCurrent(); vm.abrirCriacao(); vm.mudarCampo(CampoPaciente.NOME, "Pessoa fictícia")
        vm.criar(); vm.criar(); runCurrent()
        assertEquals(1, porta.criacoes.size)
        vm.atualizar(); runCurrent()
        vm.criar(); runCurrent()
        assertEquals(porta.criacoes[0], porta.criacoes[1])
        assertEquals(porta.chaves[0], porta.chaves[1])
    }

    @Test fun cadastroAmpliadoVaiNaRequisicaoSoComOQueFoiPreenchidoECepInvalidoBarra() = runTest {
        val porta = PortaTeste()
        val vm = PacientesViewModel(porta, compartilhar, StandardTestDispatcher(testScheduler))
        runCurrent(); vm.abrirCriacao(); vm.mudarCampo(CampoPaciente.NOME, "Pessoa fictícia")
        vm.mudarCadastroAmpliado { it.copy(profissao = "Professora", uf = "sp", cep = "123") }; vm.criar(); runCurrent()
        assertTrue(porta.criacoes.isEmpty()); assertEquals(MensagemPacientes.CAMPO_INVALIDO, vm.estado.value.errosCampos["cep"]); assertNull(vm.estado.value.errosCampos["uf"])
        vm.mudarCadastroAmpliado { it.copy(cep = "01310-100") }; vm.criar(); runCurrent()
        val enviado = porta.criacoes.single().cadastroAmpliado
        assertEquals("Professora", enviado?.profissao); assertEquals("SP", enviado?.endereco?.uf); assertEquals("01310100", enviado?.endereco?.cep); assertNull(enviado?.convenio)
    }

    @Test fun semCadastroAmpliadoARequisicaoNaoLevaOCampo() = runTest {
        val porta = PortaTeste()
        val vm = PacientesViewModel(porta, compartilhar, StandardTestDispatcher(testScheduler))
        runCurrent(); vm.abrirCriacao(); vm.mudarCampo(CampoPaciente.NOME, "Pessoa fictícia"); vm.criar(); runCurrent()
        assertNull(porta.criacoes.single().cadastroAmpliado)
    }

    @Test fun limiteAbreAvisoSemFabricarPaciente() = runTest {
        val porta = PortaTeste().apply { falhaCriar = ErroApi("LIMITE_PACIENTES_ATINGIDO", "RN-ASS-03", emptyList(), 422, null, null) }
        val vm = PacientesViewModel(porta, compartilhar, StandardTestDispatcher(testScheduler))
        runCurrent(); vm.abrirCriacao(); vm.mudarCampo(CampoPaciente.NOME, "Pessoa fictícia"); vm.criar(); runCurrent()
        assertTrue(vm.estado.value.limiteAtingido)
        assertTrue(vm.estado.value.lista.isEmpty())
    }

    @Test fun conviteNaoCompartilhaAutomaticamenteEEncerradoNaoPodeConvidar() = runTest {
        var compartilhamentos = 0
        val convidado = paciente()
        val encerrado = paciente("Ficha encerrada fictícia", StatusVinculo.ENCERRADO)
        val porta = PortaTeste(listOf(convidado, encerrado))
        val vm = PacientesViewModel(porta, Compartilhamento { compartilhamentos++; ResultadoCompartilhamento.COPIADO }, StandardTestDispatcher(testScheduler))
        runCurrent(); vm.selecionar(convidado.vinculoId); vm.gerarConvite(); runCurrent()
        assertNotNull(vm.estado.value.convite)
        assertEquals(0, compartilhamentos)
        vm.compartilharConvite("Convite fictício sem dado clínico"); runCurrent()
        assertEquals(1, compartilhamentos)
        vm.selecionar(encerrado.vinculoId); vm.gerarConvite(); runCurrent()
        assertEquals(1, porta.convites)
    }

    @Test fun cancelarConviteEmVooNaoAnexaCodigoNaOutraFicha() = runTest {
        val primeiro = paciente(); val segundo = paciente()
        val liberar = CompletableDeferred<Unit>()
        val porta = PortaTeste(listOf(primeiro, segundo)).apply { esperaConvite = liberar }
        val vm = PacientesViewModel(porta, compartilhar, StandardTestDispatcher(testScheduler))
        runCurrent(); vm.selecionar(primeiro.vinculoId); vm.gerarConvite(); runCurrent()
        vm.selecionar(segundo.vinculoId); liberar.complete(Unit); runCurrent()
        assertNull(vm.estado.value.convite)
        assertEquals(segundo, vm.estado.value.selecionado)
    }

    @Test fun conviteJaGeradoPodeSerCompartilhadoQuandoRedeCai() = runTest {
        val convidado = paciente()
        val porta = PortaTeste(listOf(convidado))
        var compartilhamentos = 0
        val vm = PacientesViewModel(porta, Compartilhamento { compartilhamentos++; ResultadoCompartilhamento.COPIADO }, StandardTestDispatcher(testScheduler))
        runCurrent(); vm.selecionar(convidado.vinculoId); vm.gerarConvite(); runCurrent()
        porta.falhaAtualizar = FalhaComunicacao()
        vm.atualizar(); runCurrent()
        assertTrue(vm.estado.value.semConexao)
        vm.compartilharConvite("Convite fictício"); runCurrent()
        assertEquals(1, compartilhamentos)
        assertEquals(MensagemPacientes.CONVITE_COPIADO, vm.estado.value.aviso)
    }

    @Test fun consentimentoNaoConfiguradoApresentaIndisponibilidadeSemInventarConvite() = runTest {
        val convidado = paciente()
        val porta = PortaTeste(listOf(convidado)).apply {
            falhaConvidar = ErroApi("CONSENTIMENTO_OBRIGATORIO", null, emptyList(), 422, null, null)
        }
        val vm = PacientesViewModel(porta, compartilhar, StandardTestDispatcher(testScheduler))
        runCurrent(); vm.selecionar(convidado.vinculoId); vm.gerarConvite(); runCurrent()
        assertEquals(MensagemPacientes.CONSENTIMENTO_INDISPONIVEL, vm.estado.value.erro)
        assertNull(vm.estado.value.convite)
    }

    @Test fun perdaDeConexaoNaoImpedeCadastroOffline() = runTest {
        val online = MutableStateFlow(true)
        val vm = PacientesViewModel(PortaTeste(listOf(paciente())), compartilhar, StandardTestDispatcher(testScheduler), conectividade = online)
        runCurrent(); online.value = false; runCurrent()
        vm.abrirCriacao(); assertNotNull(vm.estado.value.formulario)
        assertTrue(vm.estado.value.semConexao)
    }

    private inner class PortaTeste(inicial: List<PacienteResposta> = emptyList()) : PortaPacientes {
        val dados = MutableStateFlow(inicial)
        var atualizacoes = 0; var convites = 0
        var falhaAtualizar: Exception? = null; var falhaCriar: Exception? = null; var falhaConvidar: Exception? = null
        var esperaConvite: CompletableDeferred<Unit>? = null
        val criacoes = mutableListOf<CriarPacienteRequisicao>(); val chaves = mutableListOf<Uuid>()
        override fun observar() = dados
        override suspend fun assinatura(): AssinaturaResumoResposta? = null
        var resumosDados: Map<Uuid, org.br.dietamais.dominio.pacientes.ResumoPaciente>? = null
        override suspend fun resumos() = resumosDados
        override suspend fun atualizar() { atualizacoes++; falhaAtualizar?.let { throw it } }
        override suspend fun criar(requisicao: CriarPacienteRequisicao, chave: Uuid): PacienteResposta {
            criacoes += requisicao; chaves += chave; falhaCriar?.let { throw it }
            return paciente(requisicao.nome).copy(id = requisicao.id).also { dados.value += it }
        }
        override suspend fun convidar(id: Uuid, chave: Uuid): ConviteCriadoResposta {
            convites++; esperaConvite?.await(); falhaConvidar?.let { throw it }
            return ConviteCriadoResposta("ABCD2345", "https://example.invalid/convite/ABCD2345", Instant.parse("2026-01-08T00:00:00Z"))
        }
    }

    private class FixadosFalso : org.br.dietamais.data.repositorio.PortaFixados {
        val dados = MutableStateFlow(org.br.dietamais.dominio.pacientes.FixadosRecentes())
        override fun observar() = dados
        override fun alternarFixado(vinculo: Uuid) { dados.value = dados.value.alternarFixado(vinculo) }
        override fun registrarAbertura(vinculo: Uuid) { dados.value = dados.value.registrarAbertura(vinculo) }
    }
    @Test fun fixadosRecentesEResumoDeAtivos() = runTest {
        val ana = paciente("Pessoa Ana", StatusVinculo.ATIVO); val bia = paciente("Pessoa Bia", StatusVinculo.ATIVO); val cid = paciente("Pessoa Cid")
        val fixados = FixadosFalso()
        val vm = PacientesViewModel(PortaTeste(listOf(ana, bia, cid)), compartilhar, StandardTestDispatcher(testScheduler), fixados = fixados); runCurrent()
        assertEquals(2, vm.estado.value.ativos)
        vm.alternarFixado(bia.vinculoId); vm.selecionar(ana.vinculoId); runCurrent()
        assertEquals(listOf("Pessoa Bia"), vm.estado.value.atalhos.fixados.map { it.nome })
        assertEquals(listOf("Pessoa Ana"), vm.estado.value.atalhos.recentes.map { it.nome })
        assertEquals(setOf(bia.vinculoId), vm.estado.value.fixadosIds)
        vm.alternarFixado(bia.vinculoId); runCurrent()
        assertTrue(vm.estado.value.atalhos.fixados.isEmpty())
    }
}
