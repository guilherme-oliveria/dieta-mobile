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
import org.br.dietamais.data.repositorio.AlimentosRepositorio
import org.br.dietamais.plataforma.Config
import kotlin.test.*
import kotlin.uuid.Uuid

class AlimentosRepositorioTest {
    private fun alimento() = JsonContrato.decodeFromString<AlimentoResposta>(recursoContrato("fase-2/criarAlimento/resposta.json"))
    private fun MockRequestHandleScope.json(texto: String, status: HttpStatusCode = HttpStatusCode.OK) = respond(texto, status, headersOf(HttpHeaders.ContentType, "application/json"))

    @Test fun contratoOficialTipaGruposPaginaCriacaoEAtualizacaoSemInventarMacros() {
        val grupos = JsonContrato.decodeFromString<GruposAlimentosResposta>(recursoContrato("fase-2/listarGruposAlimentos/resposta.json"))
        val pagina = JsonContrato.decodeFromString<PaginaAlimentosResposta>(recursoContrato("fase-2/listarAlimentos/resposta.json"))
        assertEquals(grupos.itens.single().id, pagina.itens.single().grupoId)
        assertEquals(150.0, grupos.itens.single().kcalPorPorcao)
        assertEquals(50.0, JsonContrato.decodeFromString<CriarAlimentoRequisicao>(recursoContrato("fase-2/criarAlimento/requisicao.json")).gramasPorPorcao)
        assertFalse(JsonContrato.decodeFromString<AtualizarAlimentoRequisicao>(recursoContrato("fase-2/atualizarAlimento/requisicao.json")).ativo)
    }

    @Test fun refreshLeGruposEListaAtivosInativosPublicandoApenasBancoLocal() = runTest {
        val pedidos = mutableListOf<String>()
        comRepositorio(MockEngine { req ->
            assertNotNull(req.headers[HttpHeaders.Authorization])
            pedidos += req.url.encodedPath + "?" + req.url.parameters.toString()
            when {
                req.url.encodedPath.endsWith("grupos-alimentos") -> json(recursoContrato("fase-2/listarGruposAlimentos/resposta.json"))
                else -> {
                    assertEquals("0", req.url.parameters["pagina"]); assertEquals("20", req.url.parameters["tamanho"])
                    json(if (req.url.parameters["ativo"] == "true") recursoContrato("fase-2/listarAlimentos/resposta.json")
                        else JsonContrato.encodeToString(PaginaAlimentosResposta(emptyList(), 0, 20, 0)))
                }
            }
        }) { repo, _, _ ->
            repo.atualizar()
            assertEquals(listOf(alimento()), repo.observar().first().alimentos)
            assertEquals(1, repo.observar().first().grupos.size)
            assertEquals(3, pedidos.size)
        }
    }

    @Test fun paginaComFalhaOuStub501NaoApagaCacheValido() = runTest {
        var modo = 0
        comRepositorio(MockEngine { req ->
            when {
                modo == 2 -> json(recursoContrato("fase-2/nao-implementado.json"), HttpStatusCode.NotImplemented)
                req.url.encodedPath.endsWith("grupos-alimentos") -> json(recursoContrato("fase-2/listarGruposAlimentos/resposta.json"))
                modo == 1 && req.url.parameters["pagina"] == "1" -> throw java.io.IOException("Falha simulada")
                req.url.parameters["ativo"] == "false" -> json(JsonContrato.encodeToString(PaginaAlimentosResposta(emptyList(), 0, 20, 0)))
                else -> json(JsonContrato.encodeToString(PaginaAlimentosResposta(listOf(alimento()), 0, 20, if (modo == 1) 21 else 1)))
            }
        }) { repo, _, _ ->
            repo.atualizar(); val salvo = repo.observar().first()
            modo = 1; assertFailsWith<FalhaComunicacao> { repo.atualizar() }; assertEquals(salvo, repo.observar().first())
            modo = 2; assertEquals("NAO_IMPLEMENTADO", assertFailsWith<ErroApi> { repo.atualizar() }.codigo)
            assertEquals(salvo, repo.observar().first())
        }
    }

    @Test fun patchUsaVersaoClinicaIfMatchESalvaRespostaAutoritativa() = runTest {
        val requisicao = JsonContrato.decodeFromString<AtualizarAlimentoRequisicao>(recursoContrato("fase-2/atualizarAlimento/requisicao.json"))
        val resposta = alimento().copy(ativo = false, versao = 2)
        comRepositorio(MockEngine { req ->
            assertEquals(HttpMethod.Patch, req.method)
            assertEquals("/api/v1/alimentos/${resposta.id}", req.url.encodedPath)
            assertEquals("\"1\"", req.headers[HttpHeaders.IfMatch])
            assertEquals(requisicao, JsonContrato.decodeFromString<AtualizarAlimentoRequisicao>((req.body as TextContent).text))
            json(JsonContrato.encodeToString(resposta))
        }) { repo, _, _ ->
            assertEquals(resposta, repo.alterar(resposta.id, 1, requisicao))
            assertEquals(resposta, repo.observar().first().alimentos.single())
        }
    }

    @Test fun criarMantemIdempotencyKeyEIdDoCliente() = runTest {
        val resposta = alimento(); val chave = Uuid.random()
        val requisicao = JsonContrato.decodeFromString<CriarAlimentoRequisicao>(recursoContrato("fase-2/criarAlimento/requisicao.json")).copy(id = resposta.id)
        comRepositorio(MockEngine { req ->
            assertEquals(HttpMethod.Post, req.method); assertEquals(chave.toString(), req.headers["Idempotency-Key"])
            assertEquals(requisicao, JsonContrato.decodeFromString<CriarAlimentoRequisicao>((req.body as TextContent).text))
            json(recursoContrato("fase-2/criarAlimento/resposta.json"), HttpStatusCode.Created)
        }) { repo, _, _ -> assertEquals(resposta, repo.criar(requisicao, chave)) }
    }

    @Test fun conflitoPreservaCacheENaoAplicaAtualAutomaticamente() = runTest {
        comRepositorio(MockEngine { req -> when {
            req.method == HttpMethod.Patch -> json(recursoContrato("fase-2/atualizarAlimento/erro.json"), HttpStatusCode.Conflict)
            req.url.encodedPath.endsWith("grupos-alimentos") -> json(recursoContrato("fase-2/listarGruposAlimentos/resposta.json"))
            req.url.parameters["ativo"] == "false" -> json(JsonContrato.encodeToString(PaginaAlimentosResposta(emptyList(), 0, 20, 0)))
            else -> json(recursoContrato("fase-2/listarAlimentos/resposta.json"))
        } }) { repo, _, _ ->
            repo.atualizar(); val antes = repo.observar().first()
            val requisicao = JsonContrato.decodeFromString<AtualizarAlimentoRequisicao>(recursoContrato("fase-2/atualizarAlimento/requisicao.json"))
            assertEquals("CONFLITO_VERSAO", assertFailsWith<ErroApi> { repo.alterar(alimento().id, 1, requisicao) }.codigo)
            assertEquals(antes, repo.observar().first())
        }
    }

    @Test fun outroUsuarioOuPerfilNaoLeCacheDaNutri() = runTest {
        comRepositorio(MockEngine { req -> when {
            req.url.encodedPath.endsWith("grupos-alimentos") -> json(recursoContrato("fase-2/listarGruposAlimentos/resposta.json"))
            req.url.parameters["ativo"] == "false" -> json(JsonContrato.encodeToString(PaginaAlimentosResposta(emptyList(), 0, 20, 0)))
            else -> json(recursoContrato("fase-2/listarAlimentos/resposta.json"))
        } }) { repo, api, banco ->
            repo.atualizar(); val usuario = api.sessao.value!!.usuarioId.toString()
            banco.sessaoQueries.salvar(Uuid.random().toString(), "NUTRICIONISTA")
            assertTrue(repo.observar().first().alimentos.isEmpty()); assertTrue(repo.observar().first().grupos.isEmpty())
            banco.sessaoQueries.salvar(usuario, "PACIENTE")
            assertTrue(repo.observar().first().alimentos.isEmpty()); assertTrue(repo.observar().first().grupos.isEmpty())
        }
    }

    @Test fun logoutOuReentradaDaMesmaContaRecusaRespostaAtrasada() = runTest {
        val iniciou = CompletableDeferred<Unit>(); val liberar = CompletableDeferred<Unit>()
        comRepositorio(MockEngine { req ->
            when {
                req.url.encodedPath.endsWith("auth/login") -> json(recursoContrato("entrar/resposta.json"))
                req.url.encodedPath.endsWith("auth/logout") -> respond("", HttpStatusCode.NoContent)
                else -> { iniciou.complete(Unit); liberar.await(); json(recursoContrato("fase-2/listarGruposAlimentos/resposta.json")) }
            }
        }) { repo, api, banco ->
            val leitura = async { runCatching { repo.atualizar() } }
            iniciou.await()
            api.entrar(JsonContrato.decodeFromString(recursoContrato("entrar/requisicao.json")))
            liberar.complete(Unit); assertTrue(leitura.await().isFailure)
            assertTrue(repo.observar().first().alimentos.isEmpty())
            val usuario = api.sessao.value!!.usuarioId.toString()
            banco.alimentoCacheQueries.salvar(usuario, "NUTRICIONISTA", "alimentos", alimento().id.toString(), JsonContrato.encodeToString(alimento()))
            api.sair(); banco.sessaoQueries.salvar(usuario, "NUTRICIONISTA")
            assertTrue(banco.alimentoCacheQueries.daSessao("alimentos").executeAsList().isEmpty())
        }
    }

    private suspend fun comRepositorio(engine: MockEngine, bloco: suspend (AlimentosRepositorio, ApiDieta, BancoDieta) -> Unit) {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
            BancoDieta.Schema.create(driver); val banco = BancoDieta(driver)
            ApiDieta(Config("https://api.example.invalid"), "1.0", Plataforma.DESKTOP,
                CofreMemoria(recursoContrato("entrar/resposta.json")), banco, engine).use { api ->
                api.restaurarSessao(); bloco(AlimentosRepositorio(api, banco), api, banco)
            }
        }
    }
}
