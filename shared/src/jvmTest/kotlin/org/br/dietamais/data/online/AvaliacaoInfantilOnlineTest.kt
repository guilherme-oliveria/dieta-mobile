package org.br.dietamais.data.online

import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.http.content.TextContent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.plataforma.Config
import kotlin.test.*
import kotlin.time.Instant
import kotlin.uuid.Uuid

class AvaliacaoInfantilOnlineTest {
    @Test fun webEnviaEntradasEMantemSnapshotConfirmado() = runTest {
        val dia = Instant.parse("2026-11-01T12:00:00Z"); val id = Uuid.random(); val paciente = Uuid.random()
        val entrada = DadosInfantisAvaliacaoDto(3301, "ESCORE_Z", "FUTURA", 1.234)
        val confirmada = AvaliacaoResposta(id, paciente, paciente, dia, 3.25, 0.485, 13.82, ClassificacaoImc.ABAIXO_DO_PESO,
            versao = 4, criadoEm = dia, atualizadoEm = dia, faixaEtaria = FaixaEtariaAvaliacao.CRIANCA, infantil = entrada,
            resultadosInfantis = ResultadosInfantisAvaliacaoDto(idadeMeses = 1, curvas = listOf(CurvaCrescimentoAvaliacaoDto(
                indicador = "IMC_IDADE", classificacao = "A", percentil = IntervaloCurvaDto(50.0, 85.0)))))
        var chamadas = 0
        val engine = MockEngine { req ->
            chamadas++; assertEquals(HttpMethod.Put, req.method); assertEquals("/api/v1/avaliacoes/$id", req.url.encodedPath)
            assertEquals("\"3\"", req.headers[HttpHeaders.IfMatch])
            val corpo = (req.body as TextContent).text
            assertFalse(corpo.contains("resultadosInfantis"))
            val enviado = JsonContrato.decodeFromString<SalvarAvaliacaoRequisicao>(corpo)
            assertEquals(0.485, enviado.estaturaM); assertEquals(entrada, enviado.infantil)
            respond(JsonContrato.encodeToString(confirmada), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        ApiDieta(Config("https://app.example.invalid"), "1.0", Plataforma.WEB, CofreMemoria(recursoContrato("entrar/resposta.json")), ArmazemSessaoMemoria, engine).use { api ->
            api.restaurarSessao()
            val porta = AvaliacoesOnline(api)
            assertEquals(confirmada, porta.salvar(id, 3, SalvarAvaliacaoRequisicao(dataHora = dia, pesoKg = 3.25, estaturaM = 0.485, infantil = entrada)))
            assertEquals(confirmada.resultadosInfantis, porta.observar(paciente).first().single().resultadosInfantis)
            assertEquals(1, chamadas)
        }
    }

    @Test fun previaVaiPorPostSemIdESemGravarNoCache() = runTest {
        val dia = Instant.parse("2026-11-01T12:00:00Z"); val paciente = Uuid.random()
        val resposta = PreviaAvaliacaoDto(ResultadosInfantisAvaliacaoDto(idadeMeses = 1))
        var chamadas = 0
        val engine = MockEngine { req ->
            chamadas++; assertEquals(HttpMethod.Post, req.method); assertEquals("/api/v1/pacientes/$paciente/avaliacoes/previa", req.url.encodedPath)
            val enviado = JsonContrato.decodeFromString<SalvarAvaliacaoRequisicao>((req.body as TextContent).text)
            assertNull(enviado.id); assertEquals(0.485, enviado.estaturaM)
            respond(JsonContrato.encodeToString(resposta), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        ApiDieta(Config("https://app.example.invalid"), "1.0", Plataforma.WEB, CofreMemoria(recursoContrato("entrar/resposta.json")), ArmazemSessaoMemoria, engine).use { api ->
            api.restaurarSessao()
            val porta = AvaliacoesOnline(api)
            assertEquals(resposta, porta.previa(paciente, SalvarAvaliacaoRequisicao(id = Uuid.random(), dataHora = dia, pesoKg = 3.25, estaturaM = 0.485)))
            assertEquals(1, chamadas)
            assertTrue(porta.observar(paciente).first().isEmpty())
        }
    }
}
