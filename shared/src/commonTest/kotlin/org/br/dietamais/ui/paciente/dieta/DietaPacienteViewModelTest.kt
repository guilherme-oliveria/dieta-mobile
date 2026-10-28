package org.br.dietamais.ui.paciente.dieta

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlinx.datetime.LocalTime
import org.br.dietamais.data.remoto.ErroApi
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.DadosDietaPaciente
import org.br.dietamais.data.repositorio.PortaDietaPaciente
import kotlin.test.*
import kotlin.time.Instant
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class DietaPacienteViewModelTest {
    private val zero = TotaisNutricionaisResposta(0.0, 0.0, 0.0, 0.0)
    private fun snap(nome: String, publicado: String) = DietaSnapshotResposta(Uuid.random(), Uuid.random(), Uuid.random(), Uuid.random(),
        NutricionistaResumoResposta(Uuid.random(), nome, "CRN"), "Dieta $nome",null,
        listOf(RefeicaoResposta(Uuid.random(), "Café", LocalTime(8, 0), 0, emptyList(), zero), RefeicaoResposta(Uuid.random(), "Almoço", LocalTime(12, 0), 1, emptyList(), zero)),
        ListaSubstituicaoResposta(Uuid.random(), 0, emptyList(), emptyList(), emptyList()), zero, Instant.parse(publicado))

    private class Porta(val dados: MutableStateFlow<DadosDietaPaciente>) : PortaDietaPaciente {
        var falha: Exception? = null; var chamadas = 0
        override fun observar() = dados
        override suspend fun selecionar(dietaId: Uuid, versao: Long): DietaEmUsoResposta { chamadas++; falha?.let { throw it }; return DietaEmUsoResposta(dietaId, versao + 1) }
    }

    @Test fun semDietaMostraEstadoVazio() = runTest {
        val vm = DietaPacienteViewModel(Porta(MutableStateFlow(DadosDietaPaciente())), { LocalTime(9, 0) }, StandardTestDispatcher(testScheduler), tique = flowOf(Unit))
        advanceUntilIdle()
        assertTrue(vm.estado.value.carregado); assertNull(vm.estado.value.dieta)
    }

    @Test fun destacaAProximaERespeitaAEscolha() = runTest {
        val a = snap("Ana", "2026-01-01T10:00:00Z"); val b = snap("Bia", "2026-02-01T10:00:00Z")
        val porta = Porta(MutableStateFlow(DadosDietaPaciente(listOf(a, b))))
        val vm = DietaPacienteViewModel(porta, { LocalTime(9, 0) }, StandardTestDispatcher(testScheduler), tique = flowOf(Unit))
        advanceUntilIdle()
        assertEquals(b.dietaId, vm.estado.value.dieta?.dietaId); assertEquals("Almoço", vm.estado.value.proxima?.nome); assertEquals(2, vm.estado.value.alternativas.size)
        vm.escolher(a.dietaId); advanceUntilIdle()
        assertEquals(a.dietaId, vm.estado.value.dieta?.dietaId); assertEquals(1, vm.estado.value.versaoSelecao)
    }

    @Test fun escolherSemRedeNaoChamaAApi() = runTest {
        val a = snap("Ana", "2026-01-01T10:00:00Z"); val b = snap("Bia", "2026-02-01T10:00:00Z")
        val porta = Porta(MutableStateFlow(DadosDietaPaciente(listOf(a, b))))
        val vm = DietaPacienteViewModel(porta, { LocalTime(9, 0) }, StandardTestDispatcher(testScheduler), flowOf(false), flowOf(Unit))
        advanceUntilIdle()
        vm.escolher(a.dietaId); advanceUntilIdle()
        assertEquals(0, porta.chamadas); assertEquals(MensagemDietaPaciente.SEM_CONEXAO, vm.estado.value.erro)
    }

    @Test fun conflitoDeVersaoViraMensagem() = runTest {
        val a = snap("Ana", "2026-01-01T10:00:00Z"); val b = snap("Bia", "2026-02-01T10:00:00Z")
        val porta = Porta(MutableStateFlow(DadosDietaPaciente(listOf(a, b)))).apply { falha = ErroApi("CONFLITO_VERSAO", null, emptyList(), 409, null, null) }
        val vm = DietaPacienteViewModel(porta, { LocalTime(9, 0) }, StandardTestDispatcher(testScheduler), tique = flowOf(Unit))
        advanceUntilIdle()
        vm.escolher(a.dietaId); advanceUntilIdle()
        assertEquals(MensagemDietaPaciente.CONFLITO, vm.estado.value.erro); assertFalse(vm.estado.value.enviando)
    }
}
