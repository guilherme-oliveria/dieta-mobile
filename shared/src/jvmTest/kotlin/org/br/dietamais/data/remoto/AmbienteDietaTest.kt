package org.br.dietamais.data.remoto

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.br.dietamais.app.AmbienteDieta
import org.br.dietamais.data.local.BancoDieta
import org.br.dietamais.data.local.BancoLocal
import org.br.dietamais.data.remoto.dto.Plataforma
import org.br.dietamais.data.sync.ResultadoLeituraSync
import org.br.dietamais.plataforma.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class AmbienteDietaTest {
    @Test fun uiEExecucaoEmSegundoPlanoCompartilhamSessaoEPullEmVoo() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY); BancoDieta.Schema.create(driver)
        val local = BancoLocal(driver); val rede = RedeTeste()
        val liberar = CompletableDeferred<Unit>(); val pedidos = AtomicInteger(); val leituras = AtomicInteger()
        val cofre = object : CofreSessao {
            private var valor: String? = recursoContrato("entrar/resposta.json")
            override suspend fun ler(): String? { leituras.incrementAndGet(); return valor }
            override suspend fun gravar(conteudo: String) { valor = conteudo }
            override suspend fun limpar() { valor = null }
        }
        val config = Config("https://api.example.invalid")
        val api = ApiDieta(config, "1.0", Plataforma.DESKTOP, cofre, local.banco, MockEngine {
            pedidos.incrementAndGet(); liberar.await()
            respond(recursoContrato("puxarMudancas/resposta.json"), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        })
        val ambiente = AmbienteDieta(config, local, api, rede, StandardTestDispatcher(testScheduler))
        try {
            ambiente.primeiroPlano(true); runCurrent()
            val segundoPlano = async { ambiente.sincronizarAgora() }; runCurrent()
            esperarPedido(pedidos)
            assertEquals(1, pedidos.get())
            liberar.complete(Unit); runCurrent()
            val resultado = esperarResultado(segundoPlano)
            assertEquals(ResultadoLeituraSync.CONCLUIDO, resultado, "resultado do sync")
            assertEquals(1, pedidos.get(), "pedidos à API"); assertEquals(1, leituras.get(), "leituras do cofre")
            assertTrue(local.banco.pacienteCacheQueries.daSessao().executeAsList().isNotEmpty())
        } finally { ambiente.encerrar() }
        assertTrue(rede.fechada)
    }

    /** O MockEngine responde fora do scheduler do teste: espera (em tempo real) o pedido chegar em vez de supor que o runCurrent() já o executou. */
    private suspend fun esperarPedido(pedidos: AtomicInteger) = withContext(Dispatchers.Default) {
        withTimeout(10_000) { while (pedidos.get() < 1) delay(10) }
    }

    /** Completa em tempo real: o pull termina fora do scheduler do teste, então só o runCurrent() não basta no CI lento. */
    private suspend fun <T> TestScope.esperarResultado(tarefa: Deferred<T>): T {
        val limite = System.currentTimeMillis() + 10_000
        while (!tarefa.isCompleted && System.currentTimeMillis() < limite) { testScheduler.runCurrent(); withContext(Dispatchers.Default) { delay(10) } }
        return tarefa.await()
    }

    private class RedeTeste : ObservadorConectividade {
        override val estado = MutableStateFlow(EstadoConexao.DISPONIVEL)
        var fechada = false
        override fun close() { fechada = true }
    }
}
