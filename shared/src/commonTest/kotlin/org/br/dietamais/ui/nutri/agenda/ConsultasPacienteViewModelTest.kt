package org.br.dietamais.ui.nutri.agenda

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.br.dietamais.ui.paciente.hoje.ConsultasPacienteViewModel
import kotlin.test.*
import kotlin.time.Duration.Companion.hours
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class ConsultasPacienteViewModelTest {
    @Test fun expoeAsConsultasDoBancoLocalEAcompanhaMudancas() = runTest {
        val porta = PortaConsultasFalsa(); val vm = ConsultasPacienteViewModel(porta, StandardTestDispatcher(testScheduler)); advanceUntilIdle()
        assertTrue(vm.consultas.value.isEmpty())
        val c = consultaFalsa(Uuid.random(), agoraFalso + 2.hours); porta.dados.value = listOf(c); advanceUntilIdle()
        assertEquals(listOf(c), vm.consultas.value)
    }
}
