package org.br.dietamais.ui.nutri.avaliacao

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import kotlinx.datetime.LocalDate
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.PortaAvaliacoes
import org.br.dietamais.dominio.avaliacao.*
import kotlin.test.*
import kotlin.time.Instant
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class AvaliacaoInfantilViewModelTest {
    private val paciente = Uuid.random()
    private val agora = Instant.parse("2026-11-01T12:00:00Z")
    private fun avaliacao() = AvaliacaoResposta(Uuid.random(), paciente, paciente, agora, 10.0, 0.811, 15.2, ClassificacaoImc.ABAIXO_DO_PESO,
        versao = 3, criadoEm = agora, atualizadoEm = agora, faixaEtaria = FaixaEtariaAvaliacao.CRIANCA,
        infantil = DadosInfantisAvaliacaoDto(3301, "ESCORE_Z", "FUTURA", 1.2), resultadosInfantis = ResultadosInfantisAvaliacaoDto(idadeMeses = 12))
    private class Porta(inicial: List<AvaliacaoResposta> = emptyList()) : PortaAvaliacoes {
        val itens = MutableStateFlow(inicial)
        var enviada: SalvarAvaliacaoRequisicao? = null
        override fun observar(pacienteId: Uuid?) = itens
        override suspend fun atualizar(pacienteId: Uuid) = Unit
        override suspend fun criar(pacienteId: Uuid, requisicao: SalvarAvaliacaoRequisicao): AvaliacaoResposta {
            enviada = requisicao
            return AvaliacaoResposta(requisicao.id!!, pacienteId, pacienteId, requisicao.dataHora, requisicao.pesoKg, requisicao.estaturaM,
                10.0, ClassificacaoImc.OUTRO, versao = 1, criadoEm = requisicao.dataHora, atualizadoEm = requisicao.dataHora)
        }
        override suspend fun salvar(id: Uuid, versao: Long, requisicao: SalvarAvaliacaoRequisicao): AvaliacaoResposta {
            enviada = requisicao
            return itens.value.first { it.id == id }
        }
    }
    private fun TestScope.vm(p: Porta) = AvaliacoesViewModel(p, StandardTestDispatcher(testScheduler), flowOf(false), { agora })

    @Test fun criaLactenteOfflineComUnidadesDoContratoSemClassificacaoAdulta() = runTest {
        val p = Porta(); val vm = vm(p); vm.abrir(paciente); advanceUntilIdle()
        vm.definirPaciente(Sexo.FEMININO, LocalDate(2026, 9, 1)); vm.nova()
        assertTrue(vm.estado.value.formulario!!.infantilAtiva)
        vm.mudarMedida(MedidaAvaliacao.PESO, "3,25"); vm.mudarMedida(MedidaAvaliacao.ESTATURA, "48,5")
        vm.mudarInfantil(FormularioInfantil(pesoNascerKg = "3,301", modoCurva = "ESCORE_Z"))
        assertNull(vm.previaImc())
        vm.salvar(); advanceUntilIdle()
        val enviado = assertNotNull(p.enviada)
        assertEquals(0.485, enviado.estaturaM); assertEquals(3301, enviado.infantil?.pesoNascerGramas)
        assertNull(enviado.energia); assertNull(enviado.protocolo)
    }

    @Test fun edicaoPreservaMilimetrosEEscolhasDesconhecidas() = runTest {
        val a = avaliacao(); val p = Porta(listOf(a)); val vm = vm(p); vm.abrir(paciente); advanceUntilIdle()
        vm.definirPaciente(Sexo.MASCULINO, LocalDate(2025, 11, 1)); vm.editar(a.id)
        assertEquals("81,1", vm.estado.value.formulario!!.valores[MedidaAvaliacao.ESTATURA])
        assertEquals("3,301", vm.estado.value.formulario!!.infantil.pesoNascerKg)
        vm.salvar(); advanceUntilIdle()
        val r = assertNotNull(p.enviada)
        assertEquals(0.811, r.estaturaM); assertEquals("FUTURA", r.infantil?.etniaSlaughter)
        assertEquals(1.2, r.infantil?.fatorAtividade)
    }

    @Test fun alterarMedidasInvalidaSnapshotMasParecerNaoInvalida() = runTest {
        val a = avaliacao(); val vm = vm(Porta(listOf(a))); vm.abrir(paciente); advanceUntilIdle()
        vm.editar(a.id)
        assertNotNull(vm.estado.value.formulario!!.resultadosInfantis)
        vm.mudarParecer("Texto da nutri")
        assertNotNull(vm.estado.value.formulario!!.resultadosInfantis)
        vm.mudarMedida(MedidaAvaliacao.PESO, "11")
        assertNull(vm.estado.value.formulario!!.resultadosInfantis)
    }

    @Test fun entradaOpcionalInvalidaBloqueiaSalvamentoSemPerderFormulario() = runTest {
        val p = Porta(); val vm = vm(p); vm.abrir(paciente); advanceUntilIdle()
        vm.definirPaciente(Sexo.FEMININO, LocalDate(2026, 9, 1)); vm.nova()
        vm.mudarMedida(MedidaAvaliacao.PESO, "3,25"); vm.mudarMedida(MedidaAvaliacao.ESTATURA, "48,5")
        vm.mudarInfantil(FormularioInfantil(pesoNascerKg = "0")); vm.salvar(); advanceUntilIdle()
        assertNull(p.enviada); assertNotNull(vm.estado.value.formulario)
        assertEquals(ErroMedida.FORA_DO_INTERVALO, vm.estado.value.errosInfantis[CampoInfantil.PESO_NASCER])
    }

    @Test fun mudarSexoDaFichaInvalidaSnapshotDoFormularioAberto() = runTest {
        val a = avaliacao(); val vm = vm(Porta(listOf(a))); vm.abrir(paciente); advanceUntilIdle()
        vm.definirPaciente(Sexo.MASCULINO, LocalDate(2025, 11, 1)); vm.editar(a.id)
        vm.definirPaciente(Sexo.FEMININO, LocalDate(2025, 11, 1))
        assertNull(vm.estado.value.formulario!!.resultadosInfantis)
        assertEquals("81,1", vm.estado.value.formulario!!.valores[MedidaAvaliacao.ESTATURA])
    }
}
