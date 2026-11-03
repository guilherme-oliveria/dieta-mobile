package org.br.dietamais.data.remoto

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.http.content.TextContent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.br.dietamais.data.local.BancoDieta
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.PacientesRepositorio
import org.br.dietamais.plataforma.Config
import kotlin.test.*
import kotlin.uuid.Uuid

class PacientesRepositorioTest {
    private fun paciente() = JsonContrato.decodeFromString<PacienteResposta>(recursoContrato("consultarPaciente/resposta.json"))
    private fun MockRequestHandleScope.json(valor: String, status: HttpStatusCode = HttpStatusCode.OK) = respond(valor, status, headersOf(HttpHeaders.ContentType, "application/json"))

    @Test fun refreshPublicaCacheLocalELogoutApagaDadosClinicos() = runTest {
        comRepositorio(MockEngine { req ->
            if (req.url.encodedPath.endsWith("logout")) respond("", HttpStatusCode.NoContent)
            else json(recursoContrato("puxarMudancas/resposta.json"))
        }) { repo, api, banco ->
            repo.atualizar()
            assertTrue(repo.observar().first().isNotEmpty())
            api.sair()
            assertTrue(repo.observar().first().isEmpty())
            banco.sessaoQueries.salvar(usuarioId(), "NUTRICIONISTA")
            assertTrue(repo.observar().first().isEmpty())
            assertTrue(banco.registroSyncQueries.daSessao("vinculos").executeAsList().isEmpty())
            assertTrue(banco.registroSyncQueries.daSessao("nutris").executeAsList().isEmpty())
        }
    }

    @Test fun outraSessaoNuncaLeCacheAnterior() = runTest {
        comRepositorio(MockEngine { json(recursoContrato("puxarMudancas/resposta.json")) }) { repo, _, banco ->
            repo.atualizar()
            banco.sessaoQueries.salvar(Uuid.random().toString(), "NUTRICIONISTA")
            assertTrue(repo.observar().first().isEmpty())
            banco.sessaoQueries.salvar(Uuid.random().toString(), "PACIENTE")
            assertTrue(repo.observar().first().isEmpty())
        }
    }

    @Test fun paginaPosteriorComFalhaMantemCacheEPaginasConfirmadasParaRetomada() = runTest {
        var etapa = 0
        val base = paciente()
        val vinte = List(20) { base.copy(id = Uuid.random(), vinculoId = Uuid.random(), nome = "Paciente fictício $it") }
        comRepositorio(MockEngine { req ->
            when {
                etapa == 0 -> json(recursoContrato("puxarMudancas/resposta.json"))
                etapa == 2 -> json(JsonContrato.encodeToString(paginaSync(emptyList(), "24", false)))
                req.url.parameters["cursor"] == "3" -> json(JsonContrato.encodeToString(paginaSync(vinte, "23", true, 4)))
                else -> throw java.io.IOException("Rede simulada indisponível")
            }
        }) { repo, _, banco ->
            repo.atualizar()
            val antes = repo.observar().first()
            etapa = 1
            assertFailsWith<FalhaComunicacao> { repo.atualizar() }
            val confirmados = repo.observar().first()
            assertTrue(confirmados.containsAll(antes))
            assertEquals(21, confirmados.size)
            assertEquals("23", banco.cursorSyncQueries.buscar(usuarioId(), "NUTRICIONISTA").executeAsOne())
            etapa = 2; repo.atualizar()
            assertEquals(confirmados, repo.observar().first())
        }
    }

    @Test fun refreshPercorrePaginasDeSyncAteTemMaisFalso() = runTest {
        val base = paciente()
        val itens = List(21) { base.copy(id = Uuid.random(), vinculoId = Uuid.random(), nome = "Paciente fictício $it") }
        val cursores = mutableListOf<String?>()
        comRepositorio(MockEngine { req ->
            val cursor = req.url.parameters["cursor"]
            cursores += cursor
            assertEquals("/api/v1/sync", req.url.encodedPath)
            assertEquals("500", req.url.parameters["limite"])
            json(JsonContrato.encodeToString(paginaSync(if (cursor == null) itens.take(20) else itens.takeLast(1), if (cursor == null) "20" else "21", cursor == null, if (cursor == null) 1 else 21)))
        }) { repo, _, _ ->
            repo.atualizar()
            assertEquals(21, repo.observar().first().size)
            assertEquals(listOf(null, "20"), cursores)
        }
    }

    @Test fun respostaAtrasadaAposLogoutNaoRecriaCache() = runTest {
        val iniciou = CompletableDeferred<Unit>(); val liberar = CompletableDeferred<Unit>()
        comRepositorio(MockEngine { req ->
            if (req.url.encodedPath.endsWith("logout")) respond("", HttpStatusCode.NoContent)
            else { iniciou.complete(Unit); liberar.await(); json(recursoContrato("puxarMudancas/resposta.json")) }
        }) { repo, api, banco ->
            val pedido = async { runCatching { repo.atualizar() } }
            iniciou.await(); api.sair(); liberar.complete(Unit)
            assertTrue(pedido.await().isFailure)
            banco.sessaoQueries.salvar(usuarioId(), "NUTRICIONISTA")
            assertTrue(repo.observar().first().isEmpty())
        }
    }

    @Test fun criarUsaIdEChaveDuraveisESalvaApenasRespostaDoServidor() = runTest {
        val requisicao = JsonContrato.decodeFromString<CriarPacienteRequisicao>(recursoContrato("criarPaciente/requisicao.json"))
        val chave = Uuid.random()
        comRepositorio(MockEngine { req ->
            assertEquals("/api/v1/pacientes", req.url.encodedPath)
            assertNotNull(req.headers[HttpHeaders.Authorization])
            assertEquals(chave.toString(), req.headers["Idempotency-Key"])
            assertEquals(requisicao, JsonContrato.decodeFromString<CriarPacienteRequisicao>((req.body as TextContent).text))
            json(recursoContrato("criarPaciente/resposta.json"), HttpStatusCode.Created)
        }) { repo, _, _ ->
            val salvo = repo.criar(requisicao, chave)
            assertEquals(salvo, repo.observar().first().single())
        }
    }

    @Test fun conviteEnviaPostSemCorpoESemDadoClinicoArtificial() = runTest {
        val id = paciente().id; val chave = Uuid.random()
        comRepositorio(MockEngine { req ->
            assertEquals("/api/v1/pacientes/$id/convite", req.url.encodedPath)
            assertFalse(req.body is TextContent)
            assertEquals(chave.toString(), req.headers["Idempotency-Key"])
            json(recursoContrato("convidarPaciente/resposta.json"), HttpStatusCode.Created)
        }) { repo, _, _ ->
            val convite = repo.convidar(id, chave)
            assertEquals(JsonContrato.decodeFromString<ConviteCriadoResposta>(recursoContrato("convidarPaciente/resposta.json")), convite)
        }
    }

    @Test fun conviteAceitaEsquemaPadraoPublicadoPelaBe07() = runTest {
        val oficial = JsonContrato.decodeFromString<ConviteCriadoResposta>(recursoContrato("convidarPaciente/resposta.json"))
        val esperado = oficial.copy(link = "dietamais://convites/${oficial.codigo}")
        comRepositorio(MockEngine { json(JsonContrato.encodeToString(esperado), HttpStatusCode.Created) }) { repo, _, _ ->
            assertEquals(esperado, repo.convidar(paciente().id, Uuid.random()))
        }
    }

    @Test fun conviteRecusaEsquemaExecutavelMesmoComCodigoValido() = runTest {
        val oficial = JsonContrato.decodeFromString<ConviteCriadoResposta>(recursoContrato("convidarPaciente/resposta.json"))
        comRepositorio(MockEngine { json(JsonContrato.encodeToString(oficial.copy(link = "javascript:alert(1)"))) }) { repo, _, _ ->
            assertFailsWith<RespostaInvalida> { repo.convidar(paciente().id, Uuid.random()) }
        }
    }

    private suspend fun comRepositorio(engine: MockEngine, bloco: suspend (PacientesRepositorio, ApiDieta, BancoDieta) -> Unit) {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            BancoDieta.Schema.create(driver)
            val banco = BancoDieta(driver)
            ApiDieta(Config("https://api.example.invalid"), "1.0", Plataforma.DESKTOP,
                CofreMemoria(recursoContrato("entrar/resposta.json")), banco, engine).use { api ->
                api.restaurarSessao()
                bloco(PacientesRepositorio(api, banco), api, banco)
            }
        } finally { driver.close() }
    }
    private fun usuarioId() = JsonContrato.decodeFromString<SessaoResposta>(recursoContrato("entrar/resposta.json")).usuario.id.toString()
    private fun paginaSync(pacientes: List<PacienteResposta>, cursor: String, mais: Boolean, inicio: Long = 1) = SyncResposta(cursor, mais,
        MudancasFase1Resposta(pacientes.mapIndexed { indice, paciente -> MudancaSyncResposta(paciente.id, indice.toLong() + inicio, false, paciente) }, emptyList(), emptyList()))
}
