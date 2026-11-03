package org.br.dietamais.data.sync

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.http.content.TextContent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import org.br.dietamais.data.local.BancoDieta
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.PendenciasRepositorio
import org.br.dietamais.plataforma.Config
import kotlin.test.*
import kotlin.uuid.Uuid

class ManterMinhaIdempotenciaTest {
    private val sessao = JsonContrato.decodeFromString<SessaoResposta>(recursoContrato("entrar/resposta.json"))
    private val usuario = sessao.usuario.id.toString()
    private val perfil = Perfil.NUTRICIONISTA.name
    private val alimento = JsonContrato.decodeFromString<AlimentoResposta>(recursoContrato("fase-2/criarAlimento/resposta.json"))
    private val dados = JsonContrato.decodeFromString<AtualizarAlimentoRequisicao>(recursoContrato("fase-2/atualizarAlimento/requisicao.json"))
    private val contexto = ContextoLeituraSync(sessao.usuario.id, Perfil.NUTRICIONISTA, "epoca-teste")

    @Test fun novaIdentidadeAplicaSemInvalidarCacheDoConflitoAntigoOuPerderPendentes() = runTest {
        val cache = mutableMapOf<Uuid, ResultadoMutacaoResposta>()
        val versoes = mutableMapOf(alimento.id to 9L)
        var aplicacoes = 0
        val engine = MockEngine { req ->
            assertEquals(HttpMethod.Post, req.method); assertEquals("/api/v1/sync", req.url.encodedPath)
            assertNotNull(req.headers[HttpHeaders.Authorization])
            val lote = JsonContrato.decodeFromString<EnviarMutacoesRequisicao>((req.body as TextContent).text)
            val resultados = lote.mutacoes.map { m -> cache.getOrPut(m.idMutacao) {
                val versao = versoes.getValue(m.id)
                if (m.versaoBase != versao) ResultadoMutacaoResposta(m.idMutacao, StatusMutacao.CONFLITO,
                    codigo = "CONFLITO_VERSAO", atual = JsonContrato.encodeToJsonElement(alimento.copy(id = m.id, versao = versao)))
                else {
                    assertEquals(JsonContrato.encodeToJsonElement(dados), m.dados)
                    aplicacoes++; versoes[m.id] = versao + 1
                    ResultadoMutacaoResposta(m.idMutacao, StatusMutacao.APLICADA, versao + 1)
                }
            } }
            respond(JsonContrato.encodeToString(EnviarMutacoesResposta(resultados)), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
            BancoDieta.Schema.create(driver); val banco = BancoDieta(driver)
            ApiDieta(Config("https://api.example.invalid"), "1.0", Plataforma.DESKTOP, CofreMemoria(recursoContrato("entrar/resposta.json")), banco, engine).use { api ->
                api.restaurarSessao(); semearEdicao(banco)
                val fila = FilaMutacoes(banco); val antiga = fila.pendentes().single()
                val fonte = FonteEnvioHttp(api); val enviador = EnviadorFila(fonte, banco, { contexto })
                assertEquals(ResultadoEnvio.CONCLUIDO, enviador.enviar())
                assertEquals(StatusMutacao.CONFLITO, cache.getValue(antiga.idMutacao).status)
                // A mesma chave continua devolvendo o resultado guardado, mesmo mudando o payload/base.
                assertEquals(StatusMutacao.CONFLITO, fonte.enviar(listOf(antiga.copy(versaoBase = 9))).single().status)
                val outroId = novoIdLocal(); versoes[outroId] = 1
                fila.gravarEEnfileirar(NovaMutacao("alimentos", OperacaoMutacao.ATUALIZAR, outroId, JsonContrato.encodeToJsonElement(dados).jsonObject, 1)) { }
                val outra = fila.pendentes().single()
                val estrangeira = novoIdLocal()
                banco.mutacaoPendenteQueries.enfileirar(estrangeira.toString(), "outra-conta", perfil, "alimentos", "ATUALIZAR", novoIdLocal().toString(), 1, "{}")
                var pedidos = 0; val repo = PendenciasRepositorio(banco) { pedidos++ }
                repo.manterMinha(antiga.idMutacao)
                val nova = fila.pendentes().single { it.id == alimento.id }
                assertNotEquals(antiga.idMutacao, nova.idMutacao)
                assertEquals(antiga.copy(idMutacao = nova.idMutacao, versaoBase = 9, tentativas = 0), nova)
                assertEquals(outra, fila.pendentes().single { it.id == outroId })
                assertNull(banco.conflitoQueries.buscar(antiga.idMutacao.toString(), usuario, perfil).executeAsOneOrNull())
                assertEquals(10L, registro(banco, "alimentos")!!.getValue("versao").jsonPrimitive.long)
                assertNotNull(registro(banco, ANTERIOR + "alimentos"))
                repo.manterMinha(antiga.idMutacao); assertEquals(1, pedidos); assertEquals(2, fila.pendentes().size)
                assertEquals(ResultadoEnvio.CONCLUIDO, enviador.enviar()); assertEquals(2, aplicacoes)
                assertTrue(fila.pendentes().isEmpty()); assertNull(registro(banco, ANTERIOR + "alimentos"))
                assertEquals(StatusMutacao.CONFLITO, fonte.enviar(listOf(antiga.copy(versaoBase = 9))).single().status)
                assertEquals(StatusMutacao.APLICADA, fonte.enviar(listOf(nova)).single().status); assertEquals(2, aplicacoes)
                assertEquals(1, banco.mutacaoPendenteQueries.pendentes("outra-conta", perfil).executeAsList().size)
            }
        }
    }

    @Test fun falhaNoMeioDaResolucaoReverteIdentidadeBaseConflitoEProjecao() = runTest {
        comBanco { banco, driver ->
            val antiga = semearConflito(banco, JsonObject(mapOf("versao" to JsonPrimitive(9))))
            val filaAntes = banco.mutacaoPendenteQueries.doEscopo(usuario, perfil).executeAsList()
            val conflitoAntes = banco.conflitoQueries.doEscopo(usuario, perfil).executeAsList()
            val localAntes = registro(banco, "alimentos")
            driver.execute(null, "CREATE TRIGGER impedir_resolucao BEFORE DELETE ON conflito BEGIN SELECT RAISE(ABORT, 'falha controlada'); END", 0)
            var pedidos = 0
            assertFails { PendenciasRepositorio(banco) { pedidos++ }.manterMinha(antiga.idMutacao) }
            assertEquals(filaAntes, banco.mutacaoPendenteQueries.doEscopo(usuario, perfil).executeAsList())
            assertEquals(conflitoAntes, banco.conflitoQueries.doEscopo(usuario, perfil).executeAsList())
            assertEquals(localAntes, registro(banco, "alimentos")); assertNotNull(registro(banco, ANTERIOR + "alimentos")); assertEquals(0, pedidos)
        }
    }

    @Test fun versaoAusenteInvalidaOuSessaoAlheiaNaoReabreConflito() = runTest {
        listOf(JsonObject(emptyMap()), buildJsonObject { put("versao", JsonObject(emptyMap())) },
            buildJsonObject { put("versao", JsonPrimitive(-1)) }, buildJsonObject { put("versao", JsonPrimitive(Long.MAX_VALUE)) }).forEach { atual ->
            comBanco { banco, _ ->
                val antiga = semearConflito(banco, atual)
                val antes = banco.mutacaoPendenteQueries.doEscopo(usuario, perfil).executeAsList(); var pedidos = 0
                PendenciasRepositorio(banco) { pedidos++ }.manterMinha(antiga.idMutacao)
                assertEquals(antes, banco.mutacaoPendenteQueries.doEscopo(usuario, perfil).executeAsList()); assertEquals(0, pedidos)
            }
        }
        comBanco { banco, _ ->
            val antiga = semearConflito(banco, buildJsonObject { put("versao", JsonPrimitive(9)) })
            banco.sessaoQueries.salvar("outra-conta", perfil)
            PendenciasRepositorio(banco).manterMinha(antiga.idMutacao)
            assertEquals("CONFLITO", banco.mutacaoPendenteQueries.buscar(antiga.idMutacao.toString(), usuario, perfil).executeAsOne().status)
        }
    }

    private fun semearEdicao(banco: BancoDieta) {
        banco.registroSyncQueries.salvar(usuario, perfil, "alimentos", alimento.id.toString(), 7, 0, JsonContrato.encodeToString(alimento))
        EscritaOffline(banco).atualizar("alimentos", alimento.id, alimento.versao, JsonContrato.encodeToJsonElement(dados).jsonObject,
            JsonContrato.encodeToString(alimento.copy(ativo = false, versao = alimento.versao + 1)))
    }
    private fun semearConflito(banco: BancoDieta, atual: JsonObject): MutacaoPendente {
        semearEdicao(banco); val mutacao = FilaMutacoes(banco).pendentes().single()
        banco.mutacaoPendenteQueries.marcar("CONFLITO", mutacao.idMutacao.toString(), usuario, perfil)
        banco.conflitoQueries.salvar(mutacao.idMutacao.toString(), usuario, perfil, atual.toString()); return mutacao
    }
    private fun registro(banco: BancoDieta, colecao: String) = banco.registroSyncQueries.dadosDe(usuario, perfil, colecao, alimento.id.toString())
        .executeAsOneOrNull()?.dados_json?.let { Json.parseToJsonElement(it).jsonObject }
    private fun comBanco(bloco: (BancoDieta, JdbcSqliteDriver) -> Unit) {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
            BancoDieta.Schema.create(driver); val banco = BancoDieta(driver); banco.sessaoQueries.salvar(usuario, perfil); bloco(banco, driver)
        }
    }
}
