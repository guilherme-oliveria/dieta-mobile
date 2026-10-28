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

    @Test fun alterarGravaOtimistaEEnfileiraAtualizarSemChamarARede() = runTest {
        val requisicao = JsonContrato.decodeFromString<AtualizarAlimentoRequisicao>(recursoContrato("fase-2/atualizarAlimento/requisicao.json"))
        val id = alimento().id
        comRepositorio(MockEngine { req -> when {
            req.url.encodedPath.endsWith("grupos-alimentos") -> json(recursoContrato("fase-2/listarGruposAlimentos/resposta.json"))
            req.url.parameters["ativo"] == "false" -> json(JsonContrato.encodeToString(PaginaAlimentosResposta(emptyList(), 0, 20, 0)))
            req.method == HttpMethod.Get -> json(recursoContrato("fase-2/listarAlimentos/resposta.json"))
            else -> error("escrita offline não chama a API: ${req.method} ${req.url}")
        } }) { repo, _, banco ->
            repo.atualizar()
            val nova = repo.alterar(id, 1, requisicao)
            assertFalse(nova.ativo); assertEquals(2, nova.versao)
            assertEquals(nova, repo.observar().first().alimentos.single { it.id == id })
            val m = org.br.dietamais.data.sync.FilaMutacoes(banco).pendentes().single()
            assertEquals(org.br.dietamais.data.sync.OperacaoMutacao.ATUALIZAR, m.operacao); assertEquals(1L, m.versaoBase); assertEquals(id, m.id)
            repo.atualizar() // recarregar pela API não apaga a edição ainda não enviada
            assertEquals(nova, repo.observar().first().alimentos.single { it.id == id })
        }
    }

    @Test fun criarGravaOtimistaEEnfileiraCriarComIdDoCliente() = runTest {
        val resposta = alimento(); val chave = Uuid.random()
        val requisicao = JsonContrato.decodeFromString<CriarAlimentoRequisicao>(recursoContrato("fase-2/criarAlimento/requisicao.json")).copy(id = resposta.id)
        comRepositorio(MockEngine { error("escrita offline não chama a API") }) { repo, _, banco ->
            val criado = repo.criar(requisicao, chave)
            assertEquals(resposta.id, criado.id); assertEquals(requisicao.nome, criado.nome); assertTrue(criado.ativo)
            assertEquals(criado, repo.observar().first().alimentos.single())
            val m = org.br.dietamais.data.sync.FilaMutacoes(banco).pendentes().single()
            assertEquals(org.br.dietamais.data.sync.OperacaoMutacao.CRIAR, m.operacao); assertEquals(resposta.id, m.id)
            assertEquals(requisicao, JsonContrato.decodeFromString<CriarAlimentoRequisicao>(m.dados.toString()))
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
