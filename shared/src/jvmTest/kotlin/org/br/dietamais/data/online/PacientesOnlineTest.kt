package org.br.dietamais.data.online

import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.br.dietamais.data.remoto.ApiDieta
import org.br.dietamais.data.remoto.ArmazemSessaoMemoria
import org.br.dietamais.data.remoto.CofreMemoria
import org.br.dietamais.data.remoto.JsonContrato
import org.br.dietamais.data.remoto.dto.CriarPacienteRequisicao
import org.br.dietamais.data.remoto.dto.Plataforma
import org.br.dietamais.data.remoto.recursoContrato
import org.br.dietamais.plataforma.Config
import kotlin.test.*

/** MOB-43: a Porta de pacientes da web lê tudo pela API e nunca mostra a lista de quem saiu. */
class PacientesOnlineTest {
    private val sessao = """{"accessToken":"ACCESS_FICTICIO","expiraEm":"2026-10-24T12:15:00Z","usuario":{"id":"019a0f32-5000-7000-8000-000000000001",
        "nome":"Nutri","email":"nutri@example.invalid","emailConfirmado":false,"perfis":["NUTRICIONISTA"],"perfil":"NUTRICIONISTA",
        "nutricionistaId":"019a0f32-5000-7000-8000-000000000002","pacienteId":null,"fuso":"America/Sao_Paulo"}}"""
    private val json = headersOf(HttpHeaders.ContentType, "application/json")

    private fun api(engine: MockEngine) = ApiDieta(Config("https://app.example.invalid"), "1.0", Plataforma.WEB, CofreMemoria(sessao), ArmazemSessaoMemoria, engine)

    @Test fun carregaPelaApiEEsvaziaQuandoASessaoAcaba() = runTest {
        var consultas = 0
        val engine = MockEngine { req ->
            when (req.url.encodedPath) {
                "/api/v1/pacientes" -> {
                    consultas++
                    assertEquals("0", req.url.parameters["pagina"]); assertEquals("100", req.url.parameters["tamanho"])
                    respond(recursoContrato("listarPacientes/resposta.json"), HttpStatusCode.OK, json)
                }
                "/api/v1/auth/logout" -> respond("", HttpStatusCode.NoContent)
                else -> error("Rota inesperada ${req.url.encodedPath}")
            }
        }
        api(engine).use { api ->
            api.restaurarSessao()
            val porta = PacientesOnline(api)
            assertTrue(porta.observar().first().isEmpty())
            porta.atualizar()
            assertEquals(1, consultas)
            assertEquals("Paciente de Exemplo", porta.observar().first().single().nome)
            api.sair()
            assertTrue(porta.observar().first().isEmpty(), "lista da conta anterior não pode aparecer")
        }
    }

    @Test fun criarGuardaOQueOServidorGravouENaoMostraParaOutraConta() = runTest {
        val engine = MockEngine { req ->
            assertEquals("/api/v1/pacientes", req.url.encodedPath); assertEquals(HttpMethod.Post, req.method)
            assertNotNull(req.headers["Idempotency-Key"])
            respond(recursoContrato("criarPaciente/resposta.json"), HttpStatusCode.Created, json)
        }
        api(engine).use { api ->
            api.restaurarSessao()
            val porta = PacientesOnline(api)
            val pedido = JsonContrato.decodeFromString<CriarPacienteRequisicao>(recursoContrato("criarPaciente/requisicao.json"))
            val criado = porta.criar(pedido, kotlin.uuid.Uuid.random())
            assertEquals(criado.vinculoId, porta.observar().first().single().vinculoId)
        }
    }
}
