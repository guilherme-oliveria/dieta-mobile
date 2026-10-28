package org.br.dietamais.ui.paciente.evolucao

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import kotlinx.datetime.LocalDate
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.*
import org.br.dietamais.dominio.avaliacao.*
import kotlin.test.*
import kotlin.time.Instant
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class EvolucaoViewModelTest {
    private val paciente = Uuid.random(); private val agora = Instant.parse("2026-10-28T12:00:00Z")
    private fun av(data: String, peso: Double, compartilhada: Boolean = true) = AvaliacaoResposta(Uuid.random(), paciente, Uuid.random(), Instant.parse(data), peso, 1.7,
        imcDe(peso, 1.7), classificarImc(imcDe(peso, 1.7)), compartilhada = compartilhada, versao = 1, criadoEm = Instant.parse(data), atualizadoEm = Instant.parse(data))
    private class Evolucao(inicial: DadosEvolucao) : PortaEvolucao { val dados = MutableStateFlow(inicial); var consultado: Uuid? = Uuid.NIL
        override fun observar(pacienteId: Uuid?) = dados.also { consultado = pacienteId } }
    private class Rotina : PortaRotina {
        var peso: Double? = null; var falhar = false
        override fun observar() = MutableStateFlow(DadosRotina())
        override fun registrarCheckin(dietaId: Uuid, refeicaoId: Uuid, data: LocalDate, status: StatusCheckin, trocas: List<TrocaCheckin>, agora: Instant) = Unit
        override fun registrarAgua(ml: Int, agora: Instant) = Unit
        override fun registrarPeso(pesoKg: Double, agora: Instant) { if (falhar) error("x"); peso = pesoKg }
    }
    private fun TestScope.vm(e: Evolucao, r: Rotina? = Rotina()) = EvolucaoViewModel(e, r, StandardTestDispatcher(testScheduler), { agora })

    @Test fun pacienteVeSoCompartilhadasEEscolhePesoPrimeiro() = runTest {
        val e = Evolucao(DadosEvolucao(listOf(av("2026-10-01T00:00:00Z", 70.0), av("2026-09-01T00:00:00Z", 99.0, compartilhada = false))))
        val vm = vm(e); vm.abrir(null); advanceUntilIdle()
        assertTrue(vm.estado.value.carregado); assertEquals(SerieEvolucao.PESO, vm.estado.value.serie); assertEquals(1, vm.estado.value.pontos.size)
        assertTrue(vm.estado.value.podeRegistrarPeso); assertNull(e.consultado)
    }

    @Test fun nutriVeTodasMasNaoRegistraPeso() = runTest {
        val e = Evolucao(DadosEvolucao(listOf(av("2026-10-01T00:00:00Z", 70.0), av("2026-09-01T00:00:00Z", 72.0, compartilhada = false))))
        val vm = vm(e); vm.abrir(paciente); advanceUntilIdle()
        assertEquals(2, vm.estado.value.pontos.size); assertFalse(vm.estado.value.podeRegistrarPeso); assertEquals(paciente, e.consultado)
        vm.abrirRegistroPeso(); assertFalse(vm.estado.value.registrandoPeso)
    }

    @Test fun registraPesoEmCasaPelaFilaComValidacao() = runTest {
        val r = Rotina(); val vm = vm(Evolucao(DadosEvolucao()), r); vm.abrir(null); advanceUntilIdle()
        vm.abrirRegistroPeso(); vm.mudarPeso("19,9"); vm.registrarPeso()
        assertEquals(MensagemEvolucao.PESO_INVALIDO, vm.estado.value.erro); assertNull(r.peso)
        vm.mudarPeso("70,555"); vm.registrarPeso(); assertEquals(MensagemEvolucao.PESO_INVALIDO, vm.estado.value.erro)
        vm.mudarPeso("70,5"); vm.registrarPeso()
        assertEquals(70.5, r.peso); assertFalse(vm.estado.value.registrandoPeso); assertEquals(MensagemEvolucao.PESO_SALVO, vm.estado.value.aviso)
    }

    @Test fun novoPesoAparecePeloBancoELimpaSerieSelecionadaQueSumiu() = runTest {
        val e = Evolucao(DadosEvolucao(listOf(av("2026-10-01T00:00:00Z", 70.0)))); val vm = vm(e); vm.abrir(null); advanceUntilIdle()
        e.dados.value = DadosEvolucao(listOf(av("2026-10-01T00:00:00Z", 70.0)), listOf(PesoResposta(Uuid.random(), paciente, 69.0, Instant.parse("2026-10-20T00:00:00Z"))))
        advanceUntilIdle(); vm.escolherSerie(SerieEvolucao.PESO_EM_CASA); assertEquals(69.0, vm.estado.value.pontos.single().valor)
        vm.escolherSerie(SerieEvolucao.COXA); assertEquals(SerieEvolucao.PESO_EM_CASA, vm.estado.value.serie)
        e.dados.value = DadosEvolucao(); advanceUntilIdle(); assertNull(vm.estado.value.serie)
    }
}
