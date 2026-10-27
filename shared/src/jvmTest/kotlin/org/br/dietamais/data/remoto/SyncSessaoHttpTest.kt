package org.br.dietamais.data.remoto

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.br.dietamais.data.local.BancoDieta
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.sync.FonteLeituraHttp
import org.br.dietamais.plataforma.CofreSessao
import org.br.dietamais.plataforma.Config
import kotlin.test.*

class SyncSessaoHttpTest {
    private fun MockRequestHandleScope.json(valor: String) = respond(valor, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))

    @Test fun fonteUsaSyncAutenticadoComCursorOpacoELimiteGlobal() = runTest {
        comApi(CofreMemoria(recursoContrato("entrar/resposta.json")), MockEngine { req ->
            assertEquals("/api/v1/sync", req.url.encodedPath)
            assertEquals("3", req.url.parameters["cursor"])
            assertEquals("500", req.url.parameters["limite"])
            assertNotNull(req.headers[HttpHeaders.Authorization])
            json(recursoContrato("puxarMudancas/resposta.json"))
        }) { api ->
            val resposta = FonteLeituraHttp(api).puxar("3")
            assertEquals(JsonContrato.decodeFromString<SyncResposta>(recursoContrato("puxarMudancas/resposta.json")), resposta)
        }
    }

    @Test fun reentradaDaMesmaContaInvalidaEpocaAntesDeGravarCofre() = runTest {
        val iniciou = CompletableDeferred<Unit>(); val liberar = CompletableDeferred<Unit>()
        val cofre = object : CofreSessao {
            private var texto: String? = recursoContrato("entrar/resposta.json")
            override suspend fun ler() = texto
            override suspend fun gravar(conteudo: String) { iniciou.complete(Unit); liberar.await(); texto = conteudo }
            override suspend fun limpar() { texto = null }
        }
        comApi(cofre, MockEngine { json(recursoContrato("entrar/resposta.json")) }) { api ->
            val antes = assertNotNull(api.operacaoSessao.value)
            val entrar = async { api.entrar(JsonContrato.decodeFromString(recursoContrato("entrar/requisicao.json"))) }
            iniciou.await()
            assertNull(api.operacaoSessao.value)
            liberar.complete(Unit); entrar.await()
            val depois = assertNotNull(api.operacaoSessao.value)
            assertEquals(antes.contexto, depois.contexto)
            assertNotEquals(antes.id, depois.id)
        }
    }

    @Test fun rotacaoDeRefreshPreservaEpocaDaOperacaoDeLeitura() = runTest {
        var syncs = 0
        comApi(CofreMemoria(recursoContrato("entrar/resposta.json")), MockEngine { req ->
            if (req.url.encodedPath.endsWith("refresh")) json(recursoContrato("renovarSessao/resposta.json").replace("TOKEN_FICTICIO", "TOKEN_NOVO_FICTICIO"))
            else if (syncs++ == 0) respond(recursoContrato("consultarEu/erro-nao-autenticado.json"), HttpStatusCode.Unauthorized, headersOf(HttpHeaders.ContentType, "application/problem+json"))
            else json(recursoContrato("puxarMudancas/resposta.json"))
        }) { api ->
            val antes = api.operacaoSessao.value
            FonteLeituraHttp(api).puxar(null)
            assertEquals(antes, api.operacaoSessao.value)
            assertEquals(2, syncs)
        }
    }

    private suspend fun comApi(cofre: CofreSessao, engine: MockEngine, bloco: suspend (ApiDieta) -> Unit) {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
            BancoDieta.Schema.create(driver)
            ApiDieta(Config("https://api.example.invalid"), "1.0", Plataforma.DESKTOP, cofre, BancoDieta(driver), engine).use { api ->
                api.restaurarSessao(); bloco(api)
            }
        }
    }
}
