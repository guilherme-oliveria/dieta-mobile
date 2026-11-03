package org.br.dietamais.ui.notificacoes

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.br.dietamais.data.remoto.FalhaComunicacao
import org.br.dietamais.data.remoto.dto.AvisoResposta
import org.br.dietamais.data.remoto.dto.TipoAviso
import org.br.dietamais.data.repositorio.PortaAvisos
import kotlin.test.*
import kotlin.time.Instant
import kotlin.uuid.Uuid

class PortaAvisosFalsa : PortaAvisos {
    val dados = MutableStateFlow<List<AvisoResposta>>(emptyList())
    val lidos = mutableListOf<Uuid>()
    var todos = 0
    var falha: Exception? = null
    override fun observar(): Flow<List<AvisoResposta>> = dados
    override suspend fun marcarLido(id: Uuid) { falha?.let { throw it }; lidos += id }
    override suspend fun marcarTodosLidos() { falha?.let { throw it }; todos++ }
}

fun avisoFalso(rota: String? = "/dieta", lido: Boolean = false, quando: String = "2026-10-28T12:00:00Z", tipo: TipoAviso = TipoAviso.NOVA_DIETA) =
    AvisoResposta(Uuid.random(), tipo, "Nova dieta", "Sua nutricionista publicou uma nova dieta.", rota, Instant.parse(quando),
        if (lido) Instant.parse(quando) else null, 1)

@OptIn(ExperimentalCoroutinesApi::class)
class AvisosViewModelTest {
    @Test fun contaNaoLidosEAcompanhaMudancasDoBanco() = runTest {
        val porta = PortaAvisosFalsa(); val vm = AvisosViewModel(porta, dispatcher = StandardTestDispatcher(testScheduler)); advanceUntilIdle()
        assertEquals(0, vm.estado.value.naoLidos)
        porta.dados.value = listOf(avisoFalso(), avisoFalso(lido = true)); advanceUntilIdle()
        assertEquals(1, vm.estado.value.naoLidos); assertEquals(2, vm.estado.value.avisos.size)
    }

    @Test fun tocarNavegaPelaRotaMarcaLidoESincroniza() = runTest {
        val porta = PortaAvisosFalsa(); val aviso = avisoFalso("/agenda"); porta.dados.value = listOf(aviso)
        val rotas = mutableListOf<String>(); var syncs = 0
        val vm = AvisosViewModel(porta, { syncs++ }, rotas::add, StandardTestDispatcher(testScheduler)); advanceUntilIdle()
        vm.abrir(); assertTrue(vm.estado.value.aberto)
        vm.tocar(aviso.id); advanceUntilIdle()
        assertEquals(listOf("/agenda"), rotas); assertEquals(listOf(aviso.id), porta.lidos); assertEquals(1, syncs)
        assertFalse(vm.estado.value.aberto); assertFalse(vm.estado.value.trabalhando)
    }

    @Test fun avisoJaLidoSoNavega() = runTest {
        val porta = PortaAvisosFalsa(); val aviso = avisoFalso(lido = true); porta.dados.value = listOf(aviso)
        val rotas = mutableListOf<String>()
        val vm = AvisosViewModel(porta, aoNavegar = rotas::add, dispatcher = StandardTestDispatcher(testScheduler)); advanceUntilIdle()
        vm.tocar(aviso.id); advanceUntilIdle()
        assertEquals(listOf("/dieta"), rotas); assertTrue(porta.lidos.isEmpty())
    }

    @Test fun semConexaoMantemANavegacaoEMostraMensagem() = runTest {
        val porta = PortaAvisosFalsa().apply { falha = FalhaComunicacao() }; val aviso = avisoFalso(); porta.dados.value = listOf(aviso)
        val rotas = mutableListOf<String>()
        val vm = AvisosViewModel(porta, aoNavegar = rotas::add, dispatcher = StandardTestDispatcher(testScheduler)); advanceUntilIdle()
        vm.tocar(aviso.id); advanceUntilIdle()
        assertEquals(listOf("/dieta"), rotas); assertEquals(MensagemAvisos.SEM_CONEXAO, vm.estado.value.mensagem)
        vm.limparMensagem(); assertNull(vm.estado.value.mensagem)
    }

    @Test fun marcarTodosSoComNaoLidosEFalhaGenericaViraErro() = runTest {
        val porta = PortaAvisosFalsa(); porta.dados.value = listOf(avisoFalso(lido = true))
        val vm = AvisosViewModel(porta, dispatcher = StandardTestDispatcher(testScheduler)); advanceUntilIdle()
        vm.marcarTodosLidos(); advanceUntilIdle(); assertEquals(0, porta.todos)
        porta.dados.value = listOf(avisoFalso()); advanceUntilIdle()
        porta.falha = IllegalStateException("x"); vm.marcarTodosLidos(); advanceUntilIdle()
        assertEquals(MensagemAvisos.ERRO, vm.estado.value.mensagem)
        porta.falha = null; vm.marcarTodosLidos(); advanceUntilIdle(); assertEquals(1, porta.todos)
    }
}
