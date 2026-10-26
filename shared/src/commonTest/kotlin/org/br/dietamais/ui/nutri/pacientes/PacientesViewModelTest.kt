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

    @Test fun redeIndisponivelMantemCacheEImpedeNovaEscrita() = runTest {
        val porta = PortaTeste(listOf(paciente())).apply { falhaAtualizar = FalhaComunicacao() }
        val vm = PacientesViewModel(porta, compartilhar, StandardTestDispatcher(testScheduler))
        runCurrent()
        assertTrue(vm.estado.value.semConexao)
        assertEquals(1, vm.estado.value.lista.size)
        vm.abrirCriacao()
        assertNull(vm.estado.value.formulario)
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

    private inner class PortaTeste(inicial: List<PacienteResposta> = emptyList()) : PortaPacientes {
        val dados = MutableStateFlow(inicial)
        var atualizacoes = 0; var convites = 0
        var falhaAtualizar: Exception? = null; var falhaCriar: Exception? = null; var falhaConvidar: Exception? = null
        var esperaConvite: CompletableDeferred<Unit>? = null
        val criacoes = mutableListOf<CriarPacienteRequisicao>(); val chaves = mutableListOf<Uuid>()
        override fun observar() = dados
        override suspend fun assinatura(): AssinaturaResumoResposta? = null
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
}
