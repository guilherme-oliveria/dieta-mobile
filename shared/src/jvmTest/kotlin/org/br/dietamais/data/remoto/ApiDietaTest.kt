package org.br.dietamais.data.remoto

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.TextContent
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import org.br.dietamais.data.local.BancoDieta
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.sync.*
import org.br.dietamais.plataforma.CofreSessao
import org.br.dietamais.plataforma.Config
import kotlin.test.*
import kotlin.uuid.Uuid

internal fun recursoContrato(nome: String): String =
    ApiDietaTest::class.java.getResourceAsStream("/contrato/$nome")!!.bufferedReader(Charsets.UTF_8).use { it.readText() }

internal class CofreMemoria(var valor: String? = null) : CofreSessao {
    override suspend fun ler() = valor
    override suspend fun gravar(conteudo: String) { valor = conteudo }
    override suspend fun limpar() { valor = null }
}

class ApiDietaTest {
    @Test
    fun quedaDeRedeNoRefreshPreservaSessaoEFilaOffline() = runTest {
        verificarFalhaTransitoria { throw java.io.IOException("Simulação de rede indisponível") }
    }

    @Test
    fun timeoutNoRefreshPreservaSessaoEFilaOffline() = runTest {
        verificarFalhaTransitoria { throw java.net.SocketTimeoutException("Simulação de timeout") }
    }

    @Test
    fun servidor503NoRefreshPreservaSessaoEFilaOfflineMesmoSemProblemDetail() = runTest {
        verificarFalhaTransitoria { respond("", HttpStatusCode.ServiceUnavailable) }
    }

    @Test
    fun limite429NoRefreshPreservaSessaoEFilaOffline() = runTest {
        verificarFalhaTransitoria {
            json(recursoContrato("consultarConvite/erro-taxa-excedida.json"), HttpStatusCode.TooManyRequests)
        }
    }

    private suspend fun verificarFalhaTransitoria(resposta: suspend MockRequestHandleScope.() -> HttpResponseData) {
        comApi { banco, cofre ->
            preencherDados(banco)
            val credenciaisAntes = cofre.valor
            val sessaoAntes = banco.sessaoQueries.atual().executeAsOne()
            val filaAntes = banco.mutacaoPendenteQueries.pendentes(sessaoAntes.usuario_id, sessaoAntes.perfil).executeAsList()
            var renovacoes = 0
            novaApi(banco, cofre, MockEngine { req ->
                if (req.url.encodedPath.endsWith("refresh")) { renovacoes++; resposta() } else naoAutenticado()
            }).use { api ->
                api.restaurarSessao()
                val estadoAntes = api.sessao.value
                assertFailsWith<FalhaComunicacao> { api.obter<EuResposta>("eu") }
                assertEquals(1, renovacoes)
                assertEquals(credenciaisAntes, cofre.valor)
                assertSame(estadoAntes, api.sessao.value)
                assertEquals(sessaoAntes, banco.sessaoQueries.atual().executeAsOne())
                assertEquals(filaAntes, banco.mutacaoPendenteQueries.pendentes(sessaoAntes.usuario_id, sessaoAntes.perfil).executeAsList())
                assertEquals("99", banco.cursorSyncQueries.buscar(sessaoAntes.usuario_id, sessaoAntes.perfil).executeAsOne())
                assertEquals("{}", banco.conflitoQueries.buscar("019a0f32-5000-7000-8000-000000000200", sessaoAntes.usuario_id, sessaoAntes.perfil).executeAsOne())
            }
        }
    }

    @Test
    fun configDaPlataformaUsaPrefixoApiV1() = runTest {
        comApi { banco, cofre ->
            ApiDieta(Config("https://api.example.invalid"), "1.0", Plataforma.DESKTOP, cofre, banco, MockEngine { req ->
                assertEquals("/api/v1/eu", req.url.encodedPath)
                json(recursoContrato("consultarEu/resposta.json"))
            }).use { it.obter<EuResposta>("eu") }
        }
    }

    @Test
    fun enviaHeadersERenovaUmaVezParaRequisicoesConcorrentes() = runTest {
        var renovacoes = 0
        val headers = mutableListOf<Headers>()
        comApi { banco, cofre ->
            val engine = MockEngine { req ->
                headers += req.headers
                when (req.url.encodedPath) {
                    "/api/v1/auth/refresh" -> {
                        renovacoes++
                        delay(50)
                        assertNull(req.headers[HttpHeaders.Authorization])
                        assertEquals("REFRESH_TOKEN_FICTICIO_SEM_VALIDADE", Json.parseToJsonElement((req.body as TextContent).text).jsonObject["refreshToken"]!!.jsonPrimitive.content)
                        json(recursoContrato("renovarSessao/resposta.json").replace("TOKEN_FICTICIO", "TOKEN_NOVO_FICTICIO"))
                    }
                    "/api/v1/eu" -> if (req.headers[HttpHeaders.Authorization]!!.contains("NOVO")) json(recursoContrato("consultarEu/resposta.json")) else naoAutenticado()
                    else -> error("Rota inesperada")
                }
            }
            val api = novaApi(banco, cofre, engine)
            api.use {
                coroutineScope { List(8) { async { api.obter<EuResposta>("eu") } }.awaitAll() }
                assertEquals(1, renovacoes)
                assertTrue(cofre.valor!!.contains("NOVO"))
                assertEquals(Perfil.NUTRICIONISTA, api.sessao.value?.perfil)
                assertTrue(headers.all { it["X-App-Versao"] == "1.0" && it["X-Plataforma"] == "DESKTOP" })
            }
        }
    }

    @Test
    fun rejeicaoDoRefreshLimpaSessaoETodasTabelasENaoFazLoop() = runTest {
        var renovacoes = 0
        comApi { banco, cofre ->
            preencherDados(banco)
            novaApi(banco, cofre, MockEngine { req ->
                if (req.url.encodedPath.endsWith("refresh")) {
                    renovacoes++
                    json(recursoContrato("renovarSessao/erro-token-invalido.json"), HttpStatusCode.Unauthorized)
                } else naoAutenticado()
            }).use { api ->
                val erro = assertFailsWith<ErroApi> { api.obter<EuResposta>("eu") }
                assertEquals("NAO_AUTENTICADO", erro.codigo)
                assertEquals(1, renovacoes)
                assertNull(cofre.valor)
                assertNull(api.sessao.value)
                assertBancoLimpo(banco)
            }
        }
    }

    @Test
    fun logoutPublicoEnviaRefreshNoJsonSemBearer() = runTest {
        comApi { banco, cofre ->
            novaApi(banco, cofre, MockEngine { req ->
                assertEquals("/api/v1/auth/logout", req.url.encodedPath)
                assertNull(req.headers[HttpHeaders.Authorization])
                val corpo = Json.parseToJsonElement((req.body as TextContent).text).jsonObject
                assertEquals(setOf("refreshToken"), corpo.keys)
                respond("", HttpStatusCode.NoContent)
            }).use { api -> api.sair(); assertNull(cofre.valor); assertBancoLimpo(banco) }
        }
    }

    @Test
    fun logoutOfflineApagaCredenciaisEDadosENaoReenviaBearerAntigo() = runTest {
        comApi { banco, cofre ->
            preencherDados(banco)
            var sair = true
            novaApi(banco, cofre, MockEngine { req ->
                if (sair) { sair = false; throw java.io.IOException("Simulação sem rede") }
                assertNull(req.headers[HttpHeaders.Authorization])
                naoAutenticado()
            }).use { api ->
                assertFailsWith<FalhaComunicacao> { api.sair() }
                assertNull(cofre.valor)
                assertBancoLimpo(banco)
                assertFailsWith<ErroApi> { api.obter<EuResposta>("eu") }
            }
        }
    }

    @Test
    fun postPreservaChaveDeIdempotenciaNaRetentativaApos401() = runTest {
        val chaves = mutableListOf<String?>()
        comApi { banco, cofre ->
            novaApi(banco, cofre, MockEngine { req ->
                if (req.url.encodedPath.endsWith("refresh")) json(recursoContrato("renovarSessao/resposta.json").replace("TOKEN_FICTICIO", "TOKEN_NOVO_FICTICIO"))
                else {
                    chaves += req.headers["Idempotency-Key"]
                    if (chaves.size == 1) naoAutenticado() else json(recursoContrato("criarPaciente/resposta.json"), HttpStatusCode.Created)
                }
            }).use { api ->
                val chave = Uuid.parse("019a0f32-5000-7000-8000-000000000200")
                val corpo = Json.parseToJsonElement(recursoContrato("criarPaciente/requisicao.json"))
                api.enviar<PacienteResposta>("pacientes", corpo, chave)
                assertEquals<List<String?>>(listOf(chave.toString(), chave.toString()), chaves)
            }
        }
    }

    @Test
    fun erroUsaCodigoRegraECamposSemIncluirCorpoNaExcecao() = runTest {
        comApi { banco, cofre ->
            novaApi(banco, cofre, MockEngine { json(recursoContrato("cadastrarNutri/erro-obrigatorio.json"), HttpStatusCode.BadRequest) }).use { api ->
                val erro = assertFailsWith<ErroApi> { api.obter<EuResposta>("eu") }
                assertEquals("OBRIGATORIO", erro.codigo)
                assertTrue(erro.campos.isNotEmpty())
                assertFalse(erro.toString().contains("mensagem"))
            }
        }
    }

    @Test
    fun cancelarRequisicaoDuranteRefreshNaoPerdeTokenRotacionado() = runTest {
        comApi { banco, cofre ->
            val iniciou = CompletableDeferred<Unit>()
            val liberar = CompletableDeferred<Unit>()
            novaApi(banco, cofre, MockEngine { req ->
                when {
                    req.url.encodedPath.endsWith("refresh") -> {
                        iniciou.complete(Unit)
                        liberar.await()
                        json(recursoContrato("renovarSessao/resposta.json").replace("TOKEN_FICTICIO", "TOKEN_NOVO_FICTICIO"))
                    }
                    req.headers[HttpHeaders.Authorization]?.contains("NOVO") == true -> json(recursoContrato("consultarEu/resposta.json"))
                    else -> naoAutenticado()
                }
            }).use { api ->
                val requisicao = launch { api.obter<EuResposta>("eu") }
                iniciou.await()
                requisicao.cancel()
                liberar.complete(Unit)
                requisicao.join()
                assertTrue(cofre.valor!!.contains("NOVO"))
                assertNotNull(api.obter<EuResposta>("eu"))
            }
        }
    }

    @Test
    fun perfilFuturoNaoAtivaSessaoNemMantemDadosDaAnterior() = runTest {
        comApi { banco, cofre ->
            preencherDados(banco)
            novaApi(banco, cofre, MockEngine { json(recursoContrato("entrar/resposta.json").replace("NUTRICIONISTA", "PERFIL_FUTURO")) }).use { api ->
                assertFailsWith<RespostaInvalida> { api.entrar(JsonContrato.decodeFromString(recursoContrato("entrar/requisicao.json"))) }
                assertNull(cofre.valor)
                assertNull(api.sessao.value)
                assertBancoLimpo(banco)
            }
        }
    }

    @Test
    fun respostaBemSucedidaAntigaNaoEntregaDadosDepoisDoLogout() = runTest {
        comApi { banco, cofre ->
            val iniciou = CompletableDeferred<Unit>()
            val liberar = CompletableDeferred<Unit>()
            novaApi(banco, cofre, MockEngine { req ->
                if (req.url.encodedPath.endsWith("logout")) respond("", HttpStatusCode.NoContent)
                else { iniciou.complete(Unit); liberar.await(); json(recursoContrato("consultarEu/resposta.json")) }
            }).use { api ->
                val antiga = async { runCatching { api.obter<EuResposta>("eu") } }
                iniciou.await()
                api.sair()
                liberar.complete(Unit)
                assertTrue(antiga.await().isFailure)
                assertNull(cofre.valor)
            }
        }
    }

    @Test
    fun loginEmVooNaoRestauraSessaoDepoisDeLogout() = runTest {
        comApi { banco, cofre ->
            val iniciou = CompletableDeferred<Unit>()
            val liberar = CompletableDeferred<Unit>()
            novaApi(banco, cofre, MockEngine { req ->
                if (req.url.encodedPath.endsWith("logout")) respond("", HttpStatusCode.NoContent)
                else { iniciou.complete(Unit); liberar.await(); json(recursoContrato("entrar/resposta.json")) }
            }).use { api ->
                val login = async { runCatching { api.entrar(JsonContrato.decodeFromString(recursoContrato("entrar/requisicao.json"))) } }
                iniciou.await()
                api.sair()
                liberar.complete(Unit)
                assertTrue(login.await().isFailure)
                assertNull(cofre.valor)
                assertNull(api.sessao.value)
            }
        }
    }

    @Test
    fun escolhaDePerfilNaoPersisteCredenciaisNemAtivaSessao() = runTest {
        comApi { banco, cofre ->
            novaApi(banco, cofre, MockEngine { req ->
                assertNull(req.headers[HttpHeaders.Authorization])
                json(recursoContrato("entrar/resposta-escolher-perfil.json"))
            }).use { api ->
                val resposta = api.entrar(JsonContrato.decodeFromString(recursoContrato("entrar/requisicao-escolher-perfil.json")))
                assertEquals(2, resposta.usuario.perfis.size)
                assertNull(cofre.valor)
                assertNull(api.sessao.value)
                assertBancoLimpo(banco)
            }
        }
    }

    @Test
    fun segundo401EncerraSessaoSemOutroRefresh() = runTest {
        comApi { banco, cofre ->
            var refresh = 0
            var consultas = 0
            novaApi(banco, cofre, MockEngine { req ->
                if (req.url.encodedPath.endsWith("refresh")) { refresh++; json(recursoContrato("renovarSessao/resposta.json")) }
                else { consultas++; naoAutenticado() }
            }).use { api ->
                assertFailsWith<ErroApi> { api.obter<EuResposta>("eu") }
                assertEquals(1, refresh)
                assertEquals(2, consultas)
                assertNull(cofre.valor)
                assertBancoLimpo(banco)
            }
        }
    }

    @Test
    fun resposta401AntigaNaoRenovaNemApagaUmaNovaSessao() = runTest {
        comApi { banco, cofre ->
            val iniciou = CompletableDeferred<Unit>()
            val liberar = CompletableDeferred<Unit>()
            var renovacoes = 0
            novaApi(banco, cofre, MockEngine { req ->
                when {
                    req.url.encodedPath.endsWith("login") -> json(recursoContrato("entrar/resposta.json").replace("TOKEN_FICTICIO", "TOKEN_NOVO_FICTICIO"))
                    req.url.encodedPath.endsWith("refresh") -> { renovacoes++; naoAutenticado() }
                    else -> { iniciou.complete(Unit); liberar.await(); naoAutenticado() }
                }
            }).use { api ->
                val antiga = async { assertFailsWith<ErroApi> { api.obter<EuResposta>("eu") } }
                iniciou.await()
                api.entrar(JsonContrato.decodeFromString(recursoContrato("entrar/requisicao.json")))
                liberar.complete(Unit)
                antiga.await()
                assertEquals(0, renovacoes)
                assertNotNull(api.sessao.value)
                assertTrue(cofre.valor!!.contains("NOVO"))
            }
        }
    }

    @Test
    fun rotasAbsolutasOuForaDaApiSaoRecusadasAntesDaRede() = runTest {
        comApi { banco, cofre ->
            novaApi(banco, cofre, MockEngine { error("Não deve chamar rede") }).use { api ->
                listOf("https://outro.example.invalid/eu", "//outro/eu", "../eu", "/eu").forEach {
                    assertFailsWith<IllegalArgumentException> { api.obter<EuResposta>(it) }
                }
            }
        }
    }

    private fun novaApi(banco: BancoDieta, cofre: CofreSessao, engine: MockEngine) =
        ApiDieta(Config("https://api.example.invalid/api/v1"), "1.0", Plataforma.DESKTOP, cofre, banco, engine)

    private suspend fun <T> comApi(bloco: suspend (BancoDieta, CofreMemoria) -> T): T {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        return try {
            BancoDieta.Schema.create(driver)
            val banco = BancoDieta(driver)
            banco.sessaoQueries.salvar("019a0f32-5000-7000-8000-000000000001", "NUTRICIONISTA")
            bloco(banco, CofreMemoria(recursoContrato("entrar/resposta.json")))
        } finally { driver.close() }
    }

    private fun preencherDados(banco: BancoDieta) {
        FilaMutacoes(banco).gravarEEnfileirar(NovaMutacao("pacientes", OperacaoMutacao.CRIAR,
            Uuid.parse("019a0f32-5000-7000-8000-000000000003"), JsonObject(emptyMap()))) {}
        banco.cursorSyncQueries.salvar("019a0f32-5000-7000-8000-000000000001", "NUTRICIONISTA", "99")
        banco.conflitoQueries.salvar("019a0f32-5000-7000-8000-000000000200", "019a0f32-5000-7000-8000-000000000001", "NUTRICIONISTA", "{}")
    }

    private fun assertBancoLimpo(banco: BancoDieta) {
        assertNull(banco.sessaoQueries.atual().executeAsOneOrNull())
        assertTrue(banco.mutacaoPendenteQueries.pendentes("019a0f32-5000-7000-8000-000000000001", "NUTRICIONISTA").executeAsList().isEmpty())
        assertNull(banco.cursorSyncQueries.buscar("019a0f32-5000-7000-8000-000000000001", "NUTRICIONISTA").executeAsOneOrNull())
        assertNull(banco.conflitoQueries.buscar("019a0f32-5000-7000-8000-000000000200", "019a0f32-5000-7000-8000-000000000001", "NUTRICIONISTA").executeAsOneOrNull())
    }

    private fun MockRequestHandleScope.json(texto: String, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(texto, status, headersOf(HttpHeaders.ContentType, "application/json"))
    private fun MockRequestHandleScope.naoAutenticado() = respond(recursoContrato("consultarEu/erro-nao-autenticado.json"),
        HttpStatusCode.Unauthorized, headersOf(HttpHeaders.ContentType to listOf("application/problem+json"), HttpHeaders.WWWAuthenticate to listOf("Bearer")))
}
