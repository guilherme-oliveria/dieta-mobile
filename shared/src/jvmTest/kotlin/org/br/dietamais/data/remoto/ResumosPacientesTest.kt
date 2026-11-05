package org.br.dietamais.data.remoto

import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.carregarResumosPacientes
import org.br.dietamais.plataforma.Config
import kotlin.test.*
import kotlin.time.Instant
import kotlin.uuid.Uuid

/** BE-66: leitura do resumo (próxima consulta e última avaliação) de `GET /pacientes`, página a página. */
class ResumosPacientesTest {
    private val sessao = """{"accessToken":"ACCESS_FICTICIO","expiraEm":"2026-10-24T12:15:00Z","usuario":{"id":"019a0f32-5000-7000-8000-000000000001",
        "nome":"Nutri","email":"nutri@example.invalid","emailConfirmado":false,"perfis":["NUTRICIONISTA"],"perfil":"NUTRICIONISTA",
        "nutricionistaId":"019a0f32-5000-7000-8000-000000000002","pacienteId":null,"fuso":"America/Sao_Paulo"}}"""
    private val json = headersOf(HttpHeaders.ContentType, "application/json")
    private val t = Instant.parse("2026-11-05T12:00:00Z")
    private fun api(engine: MockEngine) = ApiDieta(Config("https://app.example.invalid"), "1.0", Plataforma.WEB, CofreMemoria(sessao), ArmazemSessaoMemoria, engine)
    private fun ficha(proxima: Instant? = null, avaliacao: Instant? = null) = PacienteResposta(Uuid.random(), Uuid.random(), "Fictício", status = StatusVinculo.ATIVO, versao = 1,
        criadoEm = t, atualizadoEm = t, proximaConsulta = proxima,
        ultimaAvaliacao = avaliacao?.let { UltimaAvaliacaoResumoDto(Uuid.random(), it, 70.0, 22.9, "NORMAL") })
    private fun pagina(itens: List<PacienteResposta>, numero: Int, total: Long) = JsonContrato.encodeToString(PaginaPacientesResposta(itens, numero, 100, total))

    @Test fun lePaginaAPaginaEGuardaSoOResumo() = runTest {
        val primeira = List(100) { ficha() }; val ultima = ficha(proxima = t, avaliacao = t)
        val paginas = mutableListOf<String>()
        val engine = MockEngine { req ->
            paginas += req.url.parameters["pagina"].orEmpty()
            respond(if (req.url.parameters["pagina"] == "0") pagina(primeira, 0, 101) else pagina(listOf(ultima), 1, 101), HttpStatusCode.OK, json)
        }
        api(engine).use { api ->
            api.restaurarSessao()
            val r = carregarResumosPacientes(api)!!
            assertEquals<List<String>>(listOf("0", "1"), paginas); assertEquals(101, r.size)
            assertEquals(t, r[ultima.vinculoId]?.proximaConsulta); assertEquals(t, r[ultima.vinculoId]?.ultimaAvaliacaoEm)
            assertNull(r[primeira.first().vinculoId]?.proximaConsulta)
        }
    }

    @Test fun paginaVaziaComTotalMaiorQueZeroDevolveNulo() = runTest {
        val engine = MockEngine { respond(pagina(emptyList(), 0, 5), HttpStatusCode.OK, json) }
        api(engine).use { api -> api.restaurarSessao(); assertNull(carregarResumosPacientes(api)) }
    }

    @Test fun semSessaoDeNutriNaoPedeNada() = runTest {
        var pedidos = 0
        val engine = MockEngine { pedidos++; respond(pagina(emptyList(), 0, 0), HttpStatusCode.OK, json) }
        api(engine).use { api -> assertNull(carregarResumosPacientes(api)); assertEquals(0, pedidos) }
    }

    @Test fun falhaDoServidorDevolveNuloSemLancar() = runTest {
        val engine = MockEngine { respond("", HttpStatusCode.InternalServerError) }
        api(engine).use { api -> api.restaurarSessao(); assertNull(carregarResumosPacientes(api)) }
    }

    @Test fun sessaoTrocadaNoMeioDescartaOResultado() = runTest {
        lateinit var cliente: ApiDieta
        val engine = MockEngine { req ->
            if (req.url.encodedPath == "/api/v1/auth/logout") respond("", HttpStatusCode.NoContent)
            else { cliente.sair(); respond(pagina(listOf(ficha()), 0, 1), HttpStatusCode.OK, json) }
        }
        cliente = api(engine)
        cliente.use { a -> a.restaurarSessao(); assertNull(carregarResumosPacientes(a)) }
    }
}
