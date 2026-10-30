package org.br.dietamais.data.remoto

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.http.content.TextContent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import org.br.dietamais.data.local.BancoDieta
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.AutenticacaoRepositorio
import org.br.dietamais.plataforma.Config
import org.br.dietamais.plataforma.IdentidadeDispositivo
import kotlin.test.*

class AutenticacaoRepositorioTest {

    private val usuarioJson = """{"id":"019a0f32-5000-7000-8000-000000000001","nome":"Nutri","email":"nutri@example.invalid","emailConfirmado":true,
        "perfis":["NUTRICIONISTA"],"perfil":"NUTRICIONISTA","nutricionistaId":"019a0f32-5000-7000-8000-000000000002","pacienteId":null,"fuso":"America/Sao_Paulo"}"""
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    private val problema = headersOf(HttpHeaders.ContentType, "application/problem+json")
    private fun restaurandoNaWeb(engine: MockEngine, bloco: suspend (AutenticacaoRepositorio) -> Unit) = runTest {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
            BancoDieta.Schema.create(driver).value
            ApiDieta(Config("https://app.example.invalid"), "1.0", Plataforma.WEB, CofreMemoria(), ArmazemSessaoMemoria, engine).use { api ->
                bloco(AutenticacaoRepositorio(api, IdentidadeDispositivo()))
            }
        }
    }

    @Test fun restaurarNaWebRenovaPeloCookieEDevolveOUsuario() {
        var chamadas = 0
        restaurandoNaWeb(MockEngine { req ->
            assertEquals("/api/v1/auth/refresh", req.url.encodedPath)
            chamadas++
            respond("""{"accessToken":"ACCESS_FICTICIO","expiraEm":"2026-10-24T12:15:00Z","usuario":$usuarioJson}""", HttpStatusCode.OK, jsonHeaders)
        }) { repositorio ->
            assertEquals(Perfil.NUTRICIONISTA, repositorio.restaurar()?.perfil)
            assertEquals(1, chamadas)
        }
    }

    @Test fun restaurarSemCookieValidoDevolveNuloEDeixaIrParaEntrar() {
        restaurandoNaWeb(MockEngine { respond("""{"type":"about:blank","title":"Não autenticado","status":401,"codigo":"TOKEN_INVALIDO","campos":[]}""", HttpStatusCode.Unauthorized, problema) }) { repositorio ->
            assertNull(repositorio.restaurar())
        }
    }

    @Test fun restaurarSemRedeOuComApiForaDoArNaoViraLogout() {
        restaurandoNaWeb(MockEngine { throw java.io.IOException("sem rede") }) { repositorio ->
            assertFailsWith<FalhaComunicacao> { repositorio.restaurar() }
        }
        restaurandoNaWeb(MockEngine { respond("""{"type":"about:blank","title":"Indisponível","status":503,"codigo":"INDISPONIVEL","campos":[]}""", HttpStatusCode.ServiceUnavailable, problema) }) { repositorio ->
            assertFailsWith<FalhaComunicacao> { repositorio.restaurar() }
        }
    }
    @Test fun recuperacaoERedefinicaoAceitamRespostaVaziaSemBearer() = runTest {
        val caminhos = mutableListOf<String>()
        comApi(MockEngine { requisicao ->
            caminhos += requisicao.url.encodedPath
            assertNull(requisicao.headers[HttpHeaders.Authorization])
            val json = Json.parseToJsonElement((requisicao.body as TextContent).text).jsonObject
            when {
                requisicao.url.encodedPath.endsWith("esqueci-senha") -> { assertEquals("teste@example.invalid", json["email"]!!.jsonPrimitive.content); respond("", HttpStatusCode.Accepted) }
                else -> { assertEquals("TOKEN_FICTICIO", json["token"]!!.jsonPrimitive.content); respond("", HttpStatusCode.NoContent) }
            }
        }) { repositorio ->
            repositorio.recuperar("teste@example.invalid")
            repositorio.redefinir("TOKEN_FICTICIO", "SENHA_FICTICIA_FORTE")
        }
        assertEquals(listOf("/api/v1/auth/esqueci-senha", "/api/v1/auth/redefinir-senha"), caminhos)
    }

    @Test fun cadastroUsaContratoOficialSemLoginAutomatico() = runTest {
        comApi(MockEngine { requisicao ->
            assertEquals("/api/v1/auth/cadastro-nutri", requisicao.url.encodedPath)
            assertNull(requisicao.headers[HttpHeaders.Authorization])
            assertNotNull(requisicao.headers["Idempotency-Key"])
            val corpo = JsonContrato.decodeFromString<CadastroNutriRequisicao>((requisicao.body as TextContent).text)
            assertEquals(JsonContrato.decodeFromString<CadastroNutriRequisicao>(recursoContrato("cadastrarNutri/requisicao.json")), corpo)
            respond(recursoContrato("cadastrarNutri/resposta.json"), HttpStatusCode.Created, headersOf(HttpHeaders.ContentType, "application/json"))
        }) { repositorio ->
            repositorio.cadastrar(JsonContrato.decodeFromString(recursoContrato("cadastrarNutri/requisicao.json")))
            assertNull(repositorio.sessao.value)
        }
    }

    private suspend fun comApi(engine: MockEngine, bloco: suspend (AutenticacaoRepositorio) -> Unit) {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            BancoDieta.Schema.create(driver)
            ApiDieta(Config("https://api.example.invalid"), "1.0", Plataforma.DESKTOP, CofreMemoria(), BancoDieta(driver), engine).use { api ->
                bloco(AutenticacaoRepositorio(api, IdentidadeDispositivo()))
            }
        } finally { driver.close() }
    }
}
