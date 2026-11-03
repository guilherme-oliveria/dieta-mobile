package org.br.dietamais.data.sync

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.br.dietamais.data.remoto.FalhaComunicacao
import org.br.dietamais.data.remoto.dto.*
import kotlin.test.*
import kotlin.uuid.Uuid

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SincronizadorTest {
    private val contexto = ContextoLeituraSync(Uuid.random(), Perfil.PACIENTE, "sessao-ficticia-1")
    private val vazias = MudancasFase1Resposta(emptyList(), emptyList(), emptyList())

    @Test fun percorrePaginasEPreservaCursorOpacoSemConverterBigint() = runTest {
        val fonte = FonteTeste(mutableListOf(SyncResposta("184467440737095516160000", true, vazias), SyncResposta("184467440737095516160001", false, vazias)))
        val banco = ArmazemTeste(); val sync = Sincronizador(fonte, banco) { contexto }
        assertEquals(ResultadoLeituraSync.CONCLUIDO, sync.puxar())
        assertEquals(listOf(null, "184467440737095516160000"), fonte.cursores)
        assertEquals("184467440737095516160001", banco.cursorAtual)
        assertEquals(2, banco.paginas.size)
    }

    @Test fun falhaPosteriorMantemPaginaConfirmadaERetomaDoSeuCursor() = runTest {
        val fonte = FonteTeste(mutableListOf(SyncResposta("3", true, vazias))).apply { falha = FalhaComunicacao() }
        val banco = ArmazemTeste(); val sync = Sincronizador(fonte, banco) { contexto }
        assertEquals(ResultadoLeituraSync.SEM_REDE, sync.puxar())
        assertEquals("3", banco.cursorAtual)
        fonte.respostas += SyncResposta("4", false, vazias); fonte.falha = null
        assertEquals(ResultadoLeituraSync.CONCLUIDO, sync.puxar())
        assertEquals(listOf(null, "3", "3"), fonte.cursores)
    }

    @Test fun falhaAoAplicarPaginaNaoAvancaSeuCursor() = runTest {
        val fonte = FonteTeste(mutableListOf(SyncResposta("4", false, vazias)))
        val banco = ArmazemTeste().apply { cursorAtual = "3"; falharAplicacao = true }
        assertEquals(ResultadoLeituraSync.FALHOU, Sincronizador(fonte, banco) { contexto }.puxar())
        assertEquals("3", banco.cursorAtual)
        assertTrue(banco.paginas.isEmpty())
    }

    @Test fun outroPullNaoAbreSegundaRequisicaoEnquantoPrimeiroEstaEmVoo() = runTest {
        val liberar = CompletableDeferred<Unit>()
        val fonte = FonteTeste(mutableListOf(SyncResposta("3", false, vazias))).apply { espera = liberar }
        val sync = Sincronizador(fonte, ArmazemTeste()) { contexto }
        val primeira = async { sync.puxar() }; runCurrent()
        assertEquals(ResultadoLeituraSync.EM_ANDAMENTO, sync.puxar())
        assertEquals(1, fonte.cursores.size)
        liberar.complete(Unit); assertEquals(ResultadoLeituraSync.CONCLUIDO, primeira.await())
    }

    @Test fun reentradaDaMesmaContaInvalidaRespostaDeOperacaoAnterior() = runTest {
        val liberar = CompletableDeferred<Unit>()
        val fonte = FonteTeste(mutableListOf(SyncResposta("3", false, vazias))).apply { espera = liberar }
        val banco = ArmazemTeste(); var atual: ContextoLeituraSync? = contexto
        val sync = Sincronizador(fonte, banco) { atual }
        val pedido = async { sync.puxar() }; runCurrent()
        atual = contexto.copy(operacao = "sessao-ficticia-2")
        liberar.complete(Unit)
        assertEquals(ResultadoLeituraSync.SESSAO_ALTERADA, pedido.await())
        assertNull(banco.cursorAtual); assertTrue(banco.paginas.isEmpty())
    }

    @Test fun cursorSemProgressoNaoGeraLoopNemAplicaPaginaInvalida() = runTest {
        val fonte = FonteTeste(mutableListOf(SyncResposta("3", true, vazias)))
        val banco = ArmazemTeste().apply { cursorAtual = "3" }
        assertEquals(ResultadoLeituraSync.FALHOU, Sincronizador(fonte, banco) { contexto }.puxar())
        assertTrue(banco.paginas.isEmpty())
        assertEquals(1, fonte.cursores.size)
    }

    @Test fun semSessaoNaoConsultaFonte() = runTest {
        val fonte = FonteTeste(mutableListOf())
        assertEquals(ResultadoLeituraSync.SEM_SESSAO, Sincronizador(fonte, ArmazemTeste()) { null }.puxar())
        assertTrue(fonte.cursores.isEmpty())
    }

    @Test fun cancelarPullNaoAplicaRespostaNemPrendeMutex() = runTest {
        val fonte = FonteTeste(mutableListOf(SyncResposta("3", false, vazias))).apply { espera = CompletableDeferred() }
        val banco = ArmazemTeste(); val sync = Sincronizador(fonte, banco) { contexto }
        val pedido = async { sync.puxar() }; runCurrent(); pedido.cancelAndJoin()
        assertNull(banco.cursorAtual)
        fonte.espera = null
        assertEquals(ResultadoLeituraSync.CONCLUIDO, sync.puxar())
    }

    private class FonteTeste(val respostas: MutableList<SyncResposta>) : FonteLeituraSync {
        val cursores = mutableListOf<String?>()
        var falha: Exception? = null; var espera: CompletableDeferred<Unit>? = null
        override suspend fun puxar(cursor: String?): SyncResposta {
            cursores += cursor; espera?.await()
            return if (respostas.isNotEmpty()) respostas.removeAt(0) else throw (falha ?: IllegalStateException("Resposta fictícia ausente"))
        }
    }
    private class ArmazemTeste : ArmazemLeituraSync {
        var cursorAtual: String? = null; var falharAplicacao = false
        val paginas = mutableListOf<SyncResposta>()
        override fun cursor(contexto: ContextoLeituraSync) = cursorAtual
        override fun aplicar(contexto: ContextoLeituraSync, pagina: SyncResposta) {
            if (falharAplicacao) error("Falha local simulada antes da transação")
            paginas += pagina; cursorAtual = pagina.cursor
        }
    }
}
