package org.br.dietamais.data.sync

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import org.br.dietamais.data.local.BancoDieta
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.AnamneseRepositorio
import org.br.dietamais.plataforma.Config
import kotlin.test.*
import kotlin.time.Instant
import kotlin.uuid.Uuid

class AnamneseEstruturadaOfflineTest {
    @Test fun respostaEFilaSaoAtomicasEResultadoAntigoNaoSobreviveAEdicao() = runTest {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
            BancoDieta.Schema.create(driver)
            val banco = BancoDieta(driver); val usuario = Uuid.random().toString()
            banco.sessaoQueries.salvar(usuario, "PACIENTE")
            val agora = Instant.parse("2026-11-02T00:00:00Z"); val pergunta = PerguntaQuestionario(Uuid.random(), "Pessoas", TipoPergunta.NUMERO, codigo = "SAL_PESSOAS")
            val q = QuestionarioResposta(Uuid.random(), Uuid.random(), Uuid.random(), nome = "Teste", blocos = listOf(BlocoQuestionario("Hábitos", listOf(pergunta))),
                respostas = mapOf(pergunta.id!! to JsonPrimitive(3)), status = StatusQuestionario.PENDENTE, enviadoEm = agora, versao = 1, criadoEm = agora, atualizadoEm = agora,
                derivados = AnamneseDerivada(salGramasPorPessoaDia = 7))
            banco.registroSyncQueries.salvar(usuario, "PACIENTE", "questionarios", q.id.toString(), 1, 0, JsonContrato.encodeToString(q))
            ApiDieta(Config("https://app.example.invalid"), "1.0", Plataforma.ANDROID, CofreMemoria(), ArmazemSessaoMemoria, MockEngine { error("Sem rede neste teste") }).use { api ->
                var pedidos = 0
                val repo = AnamneseRepositorio(api, banco, aoEnfileirar = { pedidos++ })
                assertEquals(7, repo.observarQuestionarios(null).first().single().derivados?.salGramasPorPessoaDia)
                repo.salvarRespostas(q.id, mapOf(pergunta.id!! to JsonPrimitive(4)))
                val lido = AnamneseRepositorio(api, banco).observarQuestionarios(null).first().single()
                assertNull(lido.derivados)
                assertEquals("SAL_PESSOAS", lido.blocos.single().perguntas.single().codigo)
                assertEquals(JsonPrimitive(4), lido.respostas[pergunta.id])
                val fila = FilaMutacoes(banco).pendentes().single()
                assertEquals("questionarios", fila.entidade)
                assertEquals(q.id, fila.id)
                assertFalse(fila.dados.containsKey("derivados"))
                assertEquals(1, pedidos)
            }
        }
    }
}
