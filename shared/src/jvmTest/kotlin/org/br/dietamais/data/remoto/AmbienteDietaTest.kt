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
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class AmbienteDietaTest {
    @Test fun uiEExecucaoEmSegundoPlanoCompartilhamSessaoEPullEmVoo() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY); BancoDieta.Schema.create(driver)
        val local = BancoLocal(driver); val rede = RedeTeste()
        val liberar = CompletableDeferred<Unit>(); var pedidos = 0; var leituras = 0
        val cofre = object : CofreSessao {
            private var valor: String? = recursoContrato("entrar/resposta.json")
            override suspend fun ler(): String? { leituras++; return valor }
            override suspend fun gravar(conteudo: String) { valor = conteudo }
            override suspend fun limpar() { valor = null }
        }
        val config = Config("https://api.example.invalid")
        val api = ApiDieta(config, "1.0", Plataforma.DESKTOP, cofre, local.banco, MockEngine {
            pedidos++; liberar.await()
            respond(recursoContrato("puxarMudancas/resposta.json"), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        })
        val ambiente = AmbienteDieta(config, local, api, rede, StandardTestDispatcher(testScheduler))
        try {
            ambiente.primeiroPlano(true); runCurrent()
            val segundoPlano = async { ambiente.sincronizarAgora() }; runCurrent()
            assertEquals(1, pedidos)
            liberar.complete(Unit); runCurrent()
            assertEquals(ResultadoLeituraSync.CONCLUIDO, segundoPlano.await())
            assertEquals(1, pedidos); assertEquals(1, leituras)
            assertTrue(local.banco.pacienteCacheQueries.daSessao().executeAsList().isNotEmpty())
        } finally { ambiente.encerrar() }
        assertTrue(rede.fechada)
    }

    private class RedeTeste : ObservadorConectividade {
        override val estado = MutableStateFlow(EstadoConexao.DISPONIVEL)
        var fechada = false
        override fun close() { fechada = true }
    }
}
