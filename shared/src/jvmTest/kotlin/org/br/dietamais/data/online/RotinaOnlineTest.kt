package org.br.dietamais.data.online

import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.br.dietamais.data.remoto.ApiDieta
import org.br.dietamais.data.remoto.ArmazemSessaoMemoria
import org.br.dietamais.data.remoto.CofreMemoria
import org.br.dietamais.data.remoto.dto.Plataforma
import org.br.dietamais.data.repositorio.DadosRotina
import org.br.dietamais.plataforma.Config
import kotlin.test.*
import kotlin.time.Instant

/** MOB-44: o "Hoje" do paciente na web lê `GET /eu/rotina` e grava por POST, com o registro aparecendo na hora. */
@OptIn(ExperimentalCoroutinesApi::class)
class RotinaOnlineTest {
    private val sessao = """{"accessToken":"ACCESS_FICTICIO","expiraEm":"2026-10-24T12:15:00Z","usuario":{"id":"019a0f32-5000-7000-8000-000000000010",
        "nome":"Paciente","email":"paciente@example.invalid","emailConfirmado":true,"perfis":["PACIENTE"],"perfil":"PACIENTE",
        "nutricionistaId":null,"pacienteId":"019a0f32-5000-7000-8000-000000000011","fuso":"America/Sao_Paulo"}}"""
    private val json = headersOf(HttpHeaders.ContentType, "application/json")
    private val problema = """{"type":"about:blank","title":"Recusado","status":400,"codigo":"FORMATO_INVALIDO","campos":[]}"""
    private val agora = Instant.parse("2026-10-29T15:00:00Z")
    private val vazio = """{"checkins":[],"agua":[],"pesos":[]}"""

    private fun api(engine: MockEngine) = ApiDieta(Config("https://app.example.invalid"), "1.0", Plataforma.WEB, CofreMemoria(sessao), ArmazemSessaoMemoria, engine)

    private fun TestScope.telaDe(porta: RotinaOnline): MutableStateFlow<DadosRotina> {
        val tela = MutableStateFlow(DadosRotina())
        backgroundScope.launch { porta.observar().collect { tela.value = it } }
        return tela
    }

    @Test fun leOPeriodoEMostraAAguaNaHoraMantendoOQueOServidorGravou() = runTest {
        val servidor = CompletableDeferred<Unit>()
        val postFeito = CompletableDeferred<Unit>()
        var posts = 0
        val engine = MockEngine { req ->
            when {
                req.method == HttpMethod.Get && req.url.encodedPath == "/api/v1/eu/rotina" -> {
                    assertEquals("2026-10-29", req.url.parameters["ate"]); assertEquals("2026-08-01", req.url.parameters["de"])
                    respond("""{"checkins":[],"agua":[{"id":"019a0f32-5000-7000-8000-000000000020","pacienteId":"019a0f32-5000-7000-8000-000000000011","ml":200,"registradoEm":"2026-10-29T12:00:00Z"}],"pesos":[]}""", HttpStatusCode.OK, json)
                }
                req.method == HttpMethod.Post && req.url.encodedPath == "/api/v1/eu/agua" -> {
                    servidor.await()
                    posts++; assertNotNull(req.headers["Idempotency-Key"]); postFeito.complete(Unit)
                    val id = Regex("\"id\":\"([^\"]+)\"").find((req.body as io.ktor.http.content.TextContent).text)!!.groupValues[1]
                    respond("""{"id":"$id","pacienteId":"019a0f32-5000-7000-8000-000000000011","ml":250,"registradoEm":"2026-10-29T15:00:00Z"}""", HttpStatusCode.Created, json)
                }
                else -> error("Rota inesperada ${req.url.encodedPath}")
            }
        }
        api(engine).use { api ->
            api.restaurarSessao()
            val porta = RotinaOnline(api, backgroundScope, fuso = { kotlinx.datetime.TimeZone.UTC }, agora = { agora })
            val tela = telaDe(porta)
            assertEquals(200, tela.first { it.agua.isNotEmpty() }.agua.single().ml)
            porta.registrarAgua(250, agora)
            assertEquals(setOf(200, 250), tela.first { it.agua.size == 2 }.agua.map { it.ml }.toSet(), "o registro aparece antes da resposta do servidor")
            assertEquals(0, posts)
            servidor.complete(Unit)
            postFeito.await()
            assertEquals(1, posts)
            assertEquals(2, tela.value.agua.size)
        }
    }

    @Test fun seOServidorRecusarORegistroSome() = runTest {
        val servidor = CompletableDeferred<Unit>()
        val engine = MockEngine { req ->
            if (req.method == HttpMethod.Get) respond(vazio, HttpStatusCode.OK, json)
            else { servidor.await(); respond(problema, HttpStatusCode.BadRequest, headersOf(HttpHeaders.ContentType, "application/problem+json")) }
        }
        api(engine).use { api ->
            api.restaurarSessao()
            val porta = RotinaOnline(api, backgroundScope, fuso = { kotlinx.datetime.TimeZone.UTC }, agora = { agora })
            val tela = telaDe(porta)
            tela.first { true }
            porta.registrarPeso(70.5, agora)
            assertEquals(1, tela.first { it.pesos.size == 1 }.pesos.size)
            servidor.complete(Unit)
            assertTrue(tela.first { it.pesos.isEmpty() }.pesos.isEmpty(), "registro recusado não fica na tela")
        }
    }
}
