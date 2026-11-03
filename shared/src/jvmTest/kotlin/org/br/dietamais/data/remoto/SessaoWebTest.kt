package org.br.dietamais.data.remoto

import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.plataforma.Config
import kotlin.test.*

/** MOB-42: na web o refresh vive só no cookie; a resposta não o traz e as chamadas de refresh e logout vão sem corpo. */
class SessaoWebTest {
    private val usuario = """{"id":"019a0f32-5000-7000-8000-000000000001","nome":"Nutri","email":"nutri@example.invalid","emailConfirmado":false,
        "perfis":["NUTRICIONISTA"],"perfil":"NUTRICIONISTA","nutricionistaId":"019a0f32-5000-7000-8000-000000000002","pacienteId":null,"fuso":"America/Sao_Paulo"}"""
    private fun sessao(access: String) = """{"accessToken":"$access","expiraEm":"2026-10-24T12:15:00Z","usuario":$usuario}"""
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    private val semSessao = """{"type":"about:blank","title":"Não autenticado","status":401,"codigo":"TOKEN_INVALIDO","campos":[]}"""

    private fun api(cofre: CofreMemoria, engine: MockEngine) =
        ApiDieta(Config("https://app.example.invalid"), "1.0", Plataforma.WEB, cofre, ArmazemSessaoMemoria, engine)

    @Test fun loginSemRefreshNoCorpoMantemSessaoEmMemoria() = runTest {
        val cofre = CofreMemoria()
        val engine = MockEngine { req ->
            assertEquals("WEB", req.headers["X-Plataforma"])
            respond(sessao("ACCESS_FICTICIO"), HttpStatusCode.OK, jsonHeaders)
        }
        api(cofre, engine).use { api ->
            api.entrar(LoginRequisicao("nutri@example.invalid", "senha-ficticia", DispositivoRequisicao(kotlin.uuid.Uuid.random(), Plataforma.WEB, "Navegador")))
            assertEquals(Perfil.NUTRICIONISTA, api.sessao.value?.perfil)
        }
    }

    @Test fun renovaPeloCookieSemCorpoQuandoOAccessExpira() = runTest {
        val cofre = CofreMemoria(sessao("ACCESS_VELHO").replace("\"accessToken\"", "\"accessToken\""))
        var renovacoes = 0
        val engine = MockEngine { req ->
            when (req.url.encodedPath) {
                "/api/v1/auth/refresh" -> {
                    renovacoes++
                    assertTrue(req.body is io.ktor.http.content.OutgoingContent.NoContent, "refresh da web não leva corpo")
                    assertNull(req.headers[HttpHeaders.Authorization])
                    respond(sessao("ACCESS_NOVO"), HttpStatusCode.OK, jsonHeaders)
                }
                "/api/v1/eu" -> if (req.headers[HttpHeaders.Authorization] == "Bearer ACCESS_NOVO") respond("{}", HttpStatusCode.OK, jsonHeaders)
                    else respond(semSessao, HttpStatusCode.Unauthorized, headersOf(HttpHeaders.ContentType, "application/problem+json"))
                else -> error("Rota inesperada")
            }
        }
        api(cofre, engine).use { api ->
            api.restaurarSessao()
            api.obter<kotlinx.serialization.json.JsonObject>("eu")
            assertEquals(1, renovacoes)
        }
    }

    @Test fun recarregarAPaginaFazRefreshSilenciosoEFalhaSemCookieVoltaParaEntrar() = runTest {
        var cookieValido = true
        fun engine() = MockEngine { req ->
            assertEquals("/api/v1/auth/refresh", req.url.encodedPath)
            if (cookieValido) respond(sessao("ACCESS_NOVO"), HttpStatusCode.OK, jsonHeaders)
            else respond(semSessao, HttpStatusCode.Unauthorized, headersOf(HttpHeaders.ContentType, "application/problem+json"))
        }
        api(CofreMemoria(), engine()).use { api ->
            api.restaurarSessao()
            assertEquals(Perfil.NUTRICIONISTA, api.sessao.value?.perfil)
        }
        cookieValido = false
        api(CofreMemoria(), engine()).use { api ->
            api.restaurarSessao()
            assertNull(api.sessao.value)
        }
    }

    @Test fun logoutVaiSemCorpoELimpaASessao() = runTest {
        val cofre = CofreMemoria(sessao("ACCESS_FICTICIO"))
        var chamado = false
        val engine = MockEngine { req ->
            assertEquals("/api/v1/auth/logout", req.url.encodedPath)
            assertTrue(req.body is io.ktor.http.content.OutgoingContent.NoContent)
            chamado = true
            respond("", HttpStatusCode.NoContent)
        }
        api(cofre, engine).use { api ->
            api.restaurarSessao()
            api.sair()
            assertTrue(chamado); assertNull(api.sessao.value); assertNull(cofre.valor)
        }
    }
}
