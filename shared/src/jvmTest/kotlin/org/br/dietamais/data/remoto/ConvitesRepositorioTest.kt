package org.br.dietamais.data.remoto

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.http.content.TextContent
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.br.dietamais.data.local.BancoDieta
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.ConvitesRepositorio
import org.br.dietamais.plataforma.Config
import kotlin.test.*
import kotlin.uuid.Uuid

class ConvitesRepositorioTest {
    private val pedido get() = JsonContrato.decodeFromString<AceitarConviteRequisicao>(recursoContrato("aceitarConvite/requisicao.json"))
    private val aceite get() = JsonContrato.decodeFromString<AceiteConviteResposta>(recursoContrato("aceitarConvite/resposta.json"))
    private fun MockRequestHandleScope.json(valor: String) = respond(valor, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))

    @Test fun consultarConviteEPublicoMesmoComSessaoAutenticada() = runTest {
        comApi(MockEngine { req ->
            assertEquals("/api/v1/convites/ABCD2345", req.url.encodedPath)
            assertNull(req.headers[HttpHeaders.Authorization])
            json(recursoContrato("consultarConvite/resposta.json"))
        }, recursoContrato("entrar/resposta.json")) { api, _, _ ->
            val repo = ConvitesRepositorio(api) { pedido.dispositivo }
            assertEquals(JsonContrato.decodeFromString<ConviteResposta>(recursoContrato("consultarConvite/resposta.json")), repo.consultar("ABCD2345"))
        }
    }

    @Test fun aceiteDeNovaContaEnviaContratoOficialEGravaSessaoNoCofre() = runTest {
        val chave = Uuid.random()
        comApi(MockEngine { req ->
            assertEquals("/api/v1/convites/ABCD2345/aceitar", req.url.encodedPath)
            assertNull(req.headers[HttpHeaders.Authorization])
            assertEquals(chave.toString(), req.headers["Idempotency-Key"])
            assertEquals(pedido, JsonContrato.decodeFromString<AceitarConviteRequisicao>((req.body as TextContent).text))
            json(recursoContrato("aceitarConvite/resposta.json"))
        }) { api, cofre, banco ->
            val resposta = api.aceitarConvite("ABCD2345", pedido, chave)
            assertEquals(aceite, resposta)
            assertEquals(aceite.sessao.usuario.id, api.sessao.value?.usuarioId)
            assertEquals(aceite.sessao, JsonContrato.decodeFromString<SessaoResposta>(cofre.ler()!!))
            assertEquals("PACIENTE", banco.sessaoQueries.atual().executeAsOne().perfil)
        }
    }

    @Test fun contaExistenteUsaBearerCadastroNuloEMantemUsuario() = runTest {
        val anterior = JsonContrato.decodeFromString<SessaoResposta>(recursoContrato("entrar/resposta.json"))
        val esperado = aceite.copy(sessao = aceite.sessao.copy(usuario = aceite.sessao.usuario.copy(id = anterior.usuario.id)))
        comApi(MockEngine { req ->
            assertNotNull(req.headers[HttpHeaders.Authorization])
            assertNull(JsonContrato.decodeFromString<AceitarConviteRequisicao>((req.body as TextContent).text).cadastro)
            json(JsonContrato.encodeToString(esperado))
        }, recursoContrato("entrar/resposta.json")) { api, _, _ ->
            val repo = ConvitesRepositorio(api) { pedido.dispositivo }
            repo.aceitar("ABCD2345", pedido.versaoTermo, null, Uuid.random())
            assertEquals(anterior.usuario.id, api.sessao.value?.usuarioId)
            assertEquals(Perfil.PACIENTE, api.sessao.value?.perfil)
        }
    }

    @Test fun respostaAtrasadaDepoisDeSairNaoRestauraSessaoDoConvite() = runTest {
        val iniciou = CompletableDeferred<Unit>(); val liberar = CompletableDeferred<Unit>()
        comApi(MockEngine { iniciou.complete(Unit); liberar.await(); json(recursoContrato("aceitarConvite/resposta.json")) }) { api, cofre, _ ->
            val tarefa = async { runCatching { api.aceitarConvite("ABCD2345", pedido, Uuid.random()) } }
            iniciou.await(); api.sair(); liberar.complete(Unit)
            assertIs<SessaoAlterada>(tarefa.await().exceptionOrNull())
            assertNull(api.sessao.value); assertNull(cofre.ler())
        }
    }

    @Test fun sessaoDeOutroPacienteNaRespostaNaoEGravada() = runTest {
        val invalida = aceite.copy(vinculo = aceite.vinculo.copy(pacienteId = Uuid.random()))
        comApi(MockEngine { json(JsonContrato.encodeToString(invalida)) }) { api, cofre, _ ->
            assertFailsWith<RespostaInvalida> { api.aceitarConvite("ABCD2345", pedido, Uuid.random()) }
            assertNull(cofre.ler()); assertNull(api.sessao.value)
        }
    }

    @Test fun termosUsoSaoPublicosNaRaizDaMesmaOrigemSemBearer() = runTest {
        comApi(MockEngine { req ->
            assertEquals("https://api.example.invalid/legal/terms-of-use", req.url.toString())
            assertNull(req.headers[HttpHeaders.Authorization])
            json("""{"versao":"1.0.0","status":"RASCUNHO","aprovado":false,"publicado":false,"cadastroHabilitado":false,"aprovadoEm":null,"publicadoEm":null,"conteudo":"Documento fictício de teste.","url":"/legal/terms-of-use"}""")
        }, recursoContrato("entrar/resposta.json")) { api, _, _ ->
            val repo = ConvitesRepositorio(api) { pedido.dispositivo }
            val termos = repo.consultarTermosUso()
            assertEquals("RASCUNHO", termos.status)
            assertFalse(termos.cadastroHabilitado)
        }
    }

    private suspend fun comApi(engine: MockEngine, sessao: String? = null, bloco: suspend (ApiDieta, CofreMemoria, BancoDieta) -> Unit) {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
            BancoDieta.Schema.create(driver)
            val banco = BancoDieta(driver); val cofre = CofreMemoria(sessao)
            ApiDieta(Config("https://api.example.invalid"), "1.0", Plataforma.DESKTOP, cofre, banco, engine).use { api ->
                api.restaurarSessao(); bloco(api, cofre, banco)
            }
        }
    }
}
