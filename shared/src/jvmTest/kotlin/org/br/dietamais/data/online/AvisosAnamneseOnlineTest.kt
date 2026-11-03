package org.br.dietamais.data.online

import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import org.br.dietamais.data.remoto.ApiDieta
import org.br.dietamais.data.remoto.ArmazemSessaoMemoria
import org.br.dietamais.data.remoto.CofreMemoria
import org.br.dietamais.data.remoto.dto.AvisoResposta
import org.br.dietamais.data.remoto.dto.Plataforma
import org.br.dietamais.data.remoto.dto.QuestionarioResposta
import org.br.dietamais.data.remoto.dto.StatusQuestionario
import org.br.dietamais.data.repositorio.RespostasPendentes
import org.br.dietamais.plataforma.Config
import kotlin.test.*
import kotlin.uuid.Uuid

/** MOB-45: avisos e anamnese do paciente na web, só pela API. */
@OptIn(ExperimentalCoroutinesApi::class)
class AvisosAnamneseOnlineTest {
    private val sessao = """{"accessToken":"ACCESS_FICTICIO","expiraEm":"2026-10-24T12:15:00Z","usuario":{"id":"019a0f32-5000-7000-8000-000000000010",
        "nome":"Paciente","email":"paciente@example.invalid","emailConfirmado":true,"perfis":["PACIENTE"],"perfil":"PACIENTE",
        "nutricionistaId":null,"pacienteId":"019a0f32-5000-7000-8000-000000000011","fuso":"America/Sao_Paulo"}}"""
    private val json = headersOf(HttpHeaders.ContentType, "application/json")
    private val problema = headersOf(HttpHeaders.ContentType, "application/problem+json")
    private val avisoId = "019a0f32-5000-7000-8000-0000000000a1"
    private val questionarioId = "019a0f32-5000-7000-8000-0000000000b1"
    private val pergunta = "019a0f32-5000-7000-8000-0000000000c1"

    private fun api(engine: MockEngine) = ApiDieta(Config("https://app.example.invalid"), "1.0", Plataforma.WEB, CofreMemoria(sessao), ArmazemSessaoMemoria, engine)

    private fun aviso(lidoEm: String?) = """{"id":"$avisoId","tipo":"NOVA_DIETA","titulo":"Dieta nova","corpo":"Veja","rota":"/dieta","criadoEm":"2026-10-29T12:00:00Z",
        "lidoEm":${lidoEm?.let { "\"$it\"" }},"versao":1}"""

    private fun questionario(status: String = "PENDENTE", respostas: String = "{}") = """{"id":"$questionarioId","pacienteId":"019a0f32-5000-7000-8000-000000000011",
        "vinculoId":"019a0f32-5000-7000-8000-000000000012","nome":"Anamnese","blocos":[],"respostas":$respostas,"status":"$status","enviadoEm":"2026-10-29T12:00:00Z",
        "versao":1,"criadoEm":"2026-10-29T12:00:00Z","atualizadoEm":"2026-10-29T12:00:00Z"}"""

    @Test fun avisosCarregamAoObservarEMarcarLidoAtualizaATela() = runTest {
        val engine = MockEngine { req ->
            when {
                req.method == HttpMethod.Get && req.url.encodedPath == "/api/v1/eu/avisos" -> respond("""{"itens":[${aviso(null)}],"pagina":0,"tamanho":50,"total":1,"naoLidos":1}""", HttpStatusCode.OK, json)
                req.method == HttpMethod.Post && req.url.encodedPath == "/api/v1/eu/avisos/$avisoId/lido" -> respond(aviso("2026-10-29T13:00:00Z"), HttpStatusCode.OK, json)
                else -> error("Rota inesperada ${req.url.encodedPath}")
            }
        }
        api(engine).use { api ->
            api.restaurarSessao()
            val porta = AvisosOnline(api, intervalo = Long.MAX_VALUE)
            val tela = MutableStateFlow(emptyList<AvisoResposta>())
            backgroundScope.launch { porta.observar().collect { tela.value = it } }
            assertFalse(tela.first { it.isNotEmpty() }.single().lido)
            porta.marcarLido(Uuid.parse(avisoId))
            assertTrue(tela.first { l -> l.singleOrNull()?.lido == true }.single().lido)
        }
    }

    @Test fun respostasParciaisAparecemNaHoraEChegamAoServidorAntesDeConcluir() = runTest {
        val enviadas = mutableListOf<String>()
        var concluiu = false
        val engine = MockEngine { req ->
            when {
                req.method == HttpMethod.Get && req.url.encodedPath == "/api/v1/eu/questionarios" -> respond("""{"itens":[${questionario()}]}""", HttpStatusCode.OK, json)
                req.method == HttpMethod.Put && req.url.encodedPath == "/api/v1/eu/questionarios/$questionarioId/respostas" -> {
                    enviadas += (req.body as io.ktor.http.content.TextContent).text
                    respond(questionario(respostas = """{"$pergunta":"Sim"}"""), HttpStatusCode.OK, json)
                }
                req.method == HttpMethod.Post && req.url.encodedPath == "/api/v1/eu/questionarios/$questionarioId/enviar" -> {
                    assertEquals(1, enviadas.size, "as respostas vão antes de concluir"); concluiu = true
                    respond(questionario(status = "RESPONDIDO"), HttpStatusCode.OK, json)
                }
                else -> error("Rota inesperada ${req.method} ${req.url.encodedPath}")
            }
        }
        api(engine).use { api ->
            api.restaurarSessao()
            val porta = AnamneseOnline(api, backgroundScope)
            val tela = MutableStateFlow(emptyList<QuestionarioResposta>())
            backgroundScope.launch { porta.observarQuestionarios(null).collect { tela.value = it } }
            tela.first { it.isNotEmpty() }
            porta.salvarRespostas(Uuid.parse(questionarioId), mapOf(Uuid.parse(pergunta) to JsonPrimitive("Sim")))
            assertEquals(JsonPrimitive("Sim"), tela.first { it.single().respostas.isNotEmpty() }.single().respostas[Uuid.parse(pergunta)])
            val final = porta.concluir(Uuid.parse(questionarioId))
            assertEquals(StatusQuestionario.RESPONDIDO, final.status)
            assertTrue(concluiu)
            assertTrue(enviadas.single().contains("Sim"))
        }
    }

    @Test fun naoConcluiSeAsRespostasNaoChegaramAoServidor() = runTest {
        val falha = CompletableDeferred<Unit>()
        val engine = MockEngine { req ->
            when {
                req.method == HttpMethod.Get -> respond("""{"itens":[${questionario()}]}""", HttpStatusCode.OK, json)
                req.method == HttpMethod.Put -> { falha.complete(Unit); respond("""{"type":"about:blank","title":"Erro","status":500,"codigo":"ERRO_INTERNO","campos":[]}""", HttpStatusCode.InternalServerError, problema) }
                else -> error("Concluir não devia chamar ${req.url.encodedPath}")
            }
        }
        api(engine).use { api ->
            api.restaurarSessao()
            val porta = AnamneseOnline(api, backgroundScope)
            val tela = MutableStateFlow(emptyList<QuestionarioResposta>())
            backgroundScope.launch { porta.observarQuestionarios(null).collect { tela.value = it } }
            tela.first { it.isNotEmpty() }
            porta.salvarRespostas(Uuid.parse(questionarioId), mapOf(Uuid.parse(pergunta) to JsonPrimitive("Sim")))
            assertFailsWith<RespostasPendentes> { porta.concluir(Uuid.parse(questionarioId)) }
        }
    }
}
