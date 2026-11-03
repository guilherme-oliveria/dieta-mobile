package org.br.dietamais.data.remoto

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.http.content.TextContent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import org.br.dietamais.data.local.BancoDieta
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.AlimentosRepositorio
import org.br.dietamais.data.repositorio.PendenciasRepositorio
import org.br.dietamais.data.sync.*
import org.br.dietamais.plataforma.Config
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

class ReconciliacaoAlimentosTest {
    private val grupo = JsonContrato.decodeFromString<GruposAlimentosResposta>(recursoContrato("fase-2/listarGruposAlimentos/resposta.json")).itens.single()
    private val alimento = JsonContrato.decodeFromString<AlimentoResposta>(recursoContrato("fase-2/criarAlimento/resposta.json"))
    private val edicao = JsonContrato.decodeFromString<AtualizarAlimentoRequisicao>(recursoContrato("fase-2/atualizarAlimento/requisicao.json"))
    private val conta = JsonContrato.decodeFromString<SessaoResposta>(recursoContrato("entrar/resposta.json")).usuario.id
    private val contexto = ContextoLeituraSync(conta, Perfil.NUTRICIONISTA, "epoca-teste")
    private fun pagina(cursor: String, revisao: Long, dado: AlimentoResposta? = alimento, grupoAtual: GrupoAlimentoResposta? = grupo) =
        SyncResposta(cursor, false, MudancasFase1Resposta(emptyList(), emptyList(), emptyList(),
            grupos = listOf(MudancaSyncResposta(grupo.id, revisao, grupoAtual == null, grupoAtual)),
            alimentos = listOf(MudancaSyncResposta(alimento.id, revisao, dado == null, dado))))

    @Test fun httpPullProjetaGruposAlimentosAtualizacaoETombstoneNaAba() = runTest {
        var resposta = pagina("10", 10)
        comBanco(MockEngine { json(JsonContrato.encodeToString(resposta)) }) { banco, api, repo ->
            val sync = criarSincronizador(banco, api)
            assertEquals(ResultadoLeituraSync.CONCLUIDO, sync.puxar())
            assertEquals(listOf(grupo), repo.observar().first().grupos)
            assertEquals(listOf(alimento), repo.observar().first().alimentos)
            val atualizado = JsonContrato.decodeFromString<AlimentoResposta>(recursoContrato("fase-2/atualizarAlimento/resposta.json"))
            resposta = pagina("20", 20, atualizado)
            sync.puxar(); assertEquals(listOf(atualizado), repo.observar().first().alimentos)
            assertEquals(20L, banco.registroSyncQueries.revisao(conta.toString(), "NUTRICIONISTA", "alimentos", alimento.id.toString()).executeAsOne())
            resposta = pagina("21", 19, alimento)
            sync.puxar(); assertEquals(listOf(atualizado), repo.observar().first().alimentos)
            resposta = pagina("30", 30, null, null)
            sync.puxar(); assertTrue(repo.observar().first().alimentos.isEmpty()); assertTrue(repo.observar().first().grupos.isEmpty())
        }
    }

    @Test fun duasEdicoesMesmoRegistroPreservamRemotoConsumidoEAOutraPendenciaAposRejeicao() = runTest {
        var resposta = pagina("10", 10)
        var rejeitarSegunda = false; var envios = 0
        val remoto = alimento.copy(versao = 9, atualizadoEm = alimento.atualizadoEm + 1.seconds)
        comBanco(MockEngine { req ->
            if (req.method == HttpMethod.Get) json(JsonContrato.encodeToString(resposta))
            else {
                val lote = JsonContrato.decodeFromString<EnviarMutacoesRequisicao>((req.body as TextContent).text)
                if (envios++ > 0 && !rejeitarSegunda) throw java.io.IOException("Falha controlada antes da segunda resposta")
                json(JsonContrato.encodeToString(EnviarMutacoesResposta(lote.mutacoes.map {
                    ResultadoMutacaoResposta(it.idMutacao, StatusMutacao.REJEITADA, codigo = "ASSINATURA_INATIVA")
                })))
            }
        }) { banco, api, repo ->
            val sync = criarSincronizador(banco, api); sync.puxar()
            editar(banco, edicao, 1)
            editar(banco, edicao.copy(ativo = true), 2)
            val pendentes = FilaMutacoes(banco).pendentes()
            resposta = pagina("90", 90, remoto); sync.puxar()
            assertEquals("90", banco.cursorSyncQueries.buscar(conta.toString(), "NUTRICIONISTA").executeAsOne())
            assertEquals(pendentes, FilaMutacoes(banco).pendentes())
            assertTrue(repo.observar().first().alimentos.single().ativo)
            val enviador = EnviadorFila(FonteEnvioHttp(api), banco, { contexto }, tamanhoLote = 1)
            assertEquals(ResultadoEnvio.SEM_REDE, enviador.enviar())
            val segunda = FilaMutacoes(banco).pendentes().single()
            assertEquals(pendentes.last().copy(tentativas = 1), segunda)
            val projetado = repo.observar().first().alimentos.single()
            assertTrue(projetado.ativo); assertEquals(3L, projetado.versao)
            assertEquals(alimento.atualizadoEm, projetado.atualizadoEm) // metadado da projeção otimista até resolver a fila
            rejeitarSegunda = true; assertEquals(ResultadoEnvio.CONCLUIDO, enviador.enviar())
            assertEquals(listOf(remoto), repo.observar().first().alimentos)
            assertTrue(FilaMutacoes(banco).pendentes().isEmpty())
            assertEquals(2, banco.mutacaoPendenteQueries.doEscopo(conta.toString(), "NUTRICIONISTA").executeAsList().size)
            resposta = pagina("91", 89, alimento); sync.puxar()
            assertEquals(listOf(remoto), repo.observar().first().alimentos)
        }
    }

    @Test fun tombstoneRemotoNaoRessuscitaNoCacheComPendencia() = runTest {
        var resposta = pagina("10", 10)
        comBanco(MockEngine { json(JsonContrato.encodeToString(resposta)) }) { banco, api, repo ->
            val sync = criarSincronizador(banco, api); sync.puxar(); editar(banco, edicao, 1)
            val antes = FilaMutacoes(banco).pendentes()
            resposta = pagina("20", 20, null); sync.puxar()
            assertTrue(repo.observar().first().alimentos.isEmpty()); assertEquals(antes, FilaMutacoes(banco).pendentes())
            resposta = pagina("21", 19, alimento); sync.puxar(); assertTrue(repo.observar().first().alimentos.isEmpty())
        }
    }

    @Test fun primeiraAplicadaESegundaRejeitadaPreservamEdicaoJaConfirmada() = runTest {
        var envio = 0
        comBanco(MockEngine { req ->
            if (req.method == HttpMethod.Get) json(JsonContrato.encodeToString(pagina("10", 10)))
            else {
                val m = JsonContrato.decodeFromString<EnviarMutacoesRequisicao>((req.body as TextContent).text).mutacoes.single()
                val resultado = if (envio++ == 0) ResultadoMutacaoResposta(m.idMutacao, StatusMutacao.APLICADA, 2)
                    else ResultadoMutacaoResposta(m.idMutacao, StatusMutacao.REJEITADA, codigo = "ASSINATURA_INATIVA")
                json(JsonContrato.encodeToString(EnviarMutacoesResposta(listOf(resultado))))
            }
        }) { banco, api, repo ->
            criarSincronizador(banco, api).puxar(); editar(banco, edicao, 1); editar(banco, edicao.copy(ativo = true), 2)
            assertEquals(ResultadoEnvio.CONCLUIDO, EnviadorFila(FonteEnvioHttp(api), banco, { contexto }, tamanhoLote = 1).enviar())
            val final = repo.observar().first().alimentos.single()
            assertFalse(final.ativo); assertEquals(2L, final.versao)
            assertEquals("REJEITADA", banco.mutacaoPendenteQueries.doEscopo(conta.toString(), "NUTRICIONISTA").executeAsList().single().status)
        }
    }

    @Test fun refreshCompletoGuardaBaseRemotaSemApagarPendenciaDoMesmoRegistro() = runTest {
        val remoto = alimento.copy(versao = 7, atualizadoEm = alimento.atualizadoEm + 1.seconds)
        comBanco(MockEngine { req -> when {
            req.url.encodedPath.endsWith("sync") && req.method == HttpMethod.Get -> json(JsonContrato.encodeToString(pagina("10", 10)))
            req.url.encodedPath.endsWith("grupos-alimentos") -> json(recursoContrato("fase-2/listarGruposAlimentos/resposta.json"))
            req.url.encodedPath.endsWith("alimentos") -> json(JsonContrato.encodeToString(PaginaAlimentosResposta(if (req.url.parameters["ativo"] == "true") listOf(remoto) else emptyList(), 0, 20, if (req.url.parameters["ativo"] == "true") 1 else 0)))
            else -> {
                val lote = JsonContrato.decodeFromString<EnviarMutacoesRequisicao>((req.body as TextContent).text)
                json(JsonContrato.encodeToString(EnviarMutacoesResposta(lote.mutacoes.map { ResultadoMutacaoResposta(it.idMutacao, StatusMutacao.REJEITADA, codigo = "ASSINATURA_INATIVA") })))
            }
        } }) { banco, api, repo ->
            criarSincronizador(banco, api).puxar(); editar(banco, edicao, 1)
            val fila = FilaMutacoes(banco).pendentes()
            repo.atualizar(); assertFalse(repo.observar().first().alimentos.single().ativo); assertEquals(fila, FilaMutacoes(banco).pendentes())
            EnviadorFila(FonteEnvioHttp(api), banco, { contexto }).enviar()
            assertEquals(listOf(remoto), repo.observar().first().alimentos)
        }
    }

    @Test fun paginaInvalidaReverteCacheBaseCursorEPreservacaoDaFila() = runTest {
        var resposta = pagina("10", 10)
        comBanco(MockEngine { json(JsonContrato.encodeToString(resposta)) }) { banco, api, repo ->
            val sync = criarSincronizador(banco, api); sync.puxar(); editar(banco, edicao, 1)
            val fila = FilaMutacoes(banco).pendentes(); val antes = repo.observar().first()
            resposta = pagina("20", 20).let { it.copy(mudancas = it.mudancas.copy(alimentos = listOf(MudancaSyncResposta(Uuid.random(), 20, false, alimento)))) }
            assertEquals(ResultadoLeituraSync.FALHOU, sync.puxar())
            assertEquals(antes, repo.observar().first()); assertEquals(fila, FilaMutacoes(banco).pendentes())
            assertEquals("10", banco.cursorSyncQueries.buscar(conta.toString(), "NUTRICIONISTA").executeAsOne())
            val remoto = alimento.copy(versao = 3)
            resposta = pagina("15", 15, remoto); sync.puxar()
            PendenciasRepositorio(banco).descartar(fila.single().idMutacao)
            assertEquals(listOf(remoto), repo.observar().first().alimentos)
            banco.sessaoQueries.salvar(Uuid.random().toString(), "NUTRICIONISTA")
            assertTrue(repo.observar().first().alimentos.isEmpty()); assertTrue(repo.observar().first().grupos.isEmpty())
        }
    }

    @Test fun escolherServidorNaoRegrideBaseRecebidaDepoisDoConflito() = runTest {
        var resposta = pagina("10", 10)
        comBanco(MockEngine { req ->
            if (req.method == HttpMethod.Get) json(JsonContrato.encodeToString(resposta))
            else {
                val m = JsonContrato.decodeFromString<EnviarMutacoesRequisicao>((req.body as TextContent).text).mutacoes.single()
                json(JsonContrato.encodeToString(EnviarMutacoesResposta(listOf(ResultadoMutacaoResposta(m.idMutacao,
                    StatusMutacao.CONFLITO, atual = JsonContrato.encodeToJsonElement(alimento.copy(versao = 2)).jsonObject)))))
            }
        }) { banco, api, repo ->
            val sync = criarSincronizador(banco, api); sync.puxar(); editar(banco, edicao, 1)
            val id = FilaMutacoes(banco).pendentes().single().idMutacao
            EnviadorFila(FonteEnvioHttp(api), banco, { contexto }).enviar()
            val remoto = alimento.copy(versao = 5, atualizadoEm = alimento.atualizadoEm + 1.seconds)
            resposta = pagina("50", 50, remoto); sync.puxar()
            PendenciasRepositorio(banco).usarServidor(id)
            assertEquals(listOf(remoto), repo.observar().first().alimentos)
            assertTrue(banco.mutacaoPendenteQueries.doEscopo(conta.toString(), "NUTRICIONISTA").executeAsList().isEmpty())
        }
    }

    @Test fun criacaoConfirmadaNaoHerdaDesativacaoPosteriorRejeitada() = runTest {
        var envio = 0
        comBanco(MockEngine { req ->
            val m = JsonContrato.decodeFromString<EnviarMutacoesRequisicao>((req.body as TextContent).text).mutacoes.single()
            val r = if (envio++ == 0) ResultadoMutacaoResposta(m.idMutacao, StatusMutacao.APLICADA, 1)
                else ResultadoMutacaoResposta(m.idMutacao, StatusMutacao.REJEITADA, codigo = "ASSINATURA_INATIVA")
            json(JsonContrato.encodeToString(EnviarMutacoesResposta(listOf(r))))
        }) { banco, api, repo ->
            val criar = JsonContrato.decodeFromString<CriarAlimentoRequisicao>(recursoContrato("fase-2/criarAlimento/requisicao.json")).copy(id = alimento.id)
            repo.criar(criar, Uuid.random())
            repo.alterar(alimento.id, 1, edicao)
            assertEquals(ResultadoEnvio.CONCLUIDO, EnviadorFila(FonteEnvioHttp(api), banco, { contexto }, tamanhoLote = 1).enviar())
            val confirmado = repo.observar().first().alimentos.single()
            assertTrue(confirmado.ativo); assertEquals(1L, confirmado.versao)
            assertEquals(criar.nome, confirmado.nome)
        }
    }

    private fun editar(banco: BancoDieta, dados: AtualizarAlimentoRequisicao, base: Long) = EscritaOffline(banco).atualizar("alimentos", alimento.id, base,
        JsonContrato.encodeToJsonElement(dados).jsonObject, JsonContrato.encodeToString(alimento.copy(ativo = dados.ativo, versao = base + 1)))
    private fun criarSincronizador(banco: BancoDieta, api: ApiDieta) = Sincronizador(FonteLeituraHttp(api), ArmazemLeituraSql(banco) { contexto }, { contexto })
    private fun MockRequestHandleScope.json(texto: String) = respond(texto, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
    private suspend fun comBanco(engine: MockEngine, bloco: suspend (BancoDieta, ApiDieta, AlimentosRepositorio) -> Unit) {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
            BancoDieta.Schema.create(driver); val banco = BancoDieta(driver)
            ApiDieta(Config("https://api.example.invalid"), "1.0", Plataforma.DESKTOP, CofreMemoria(recursoContrato("entrar/resposta.json")), banco, engine).use { api ->
                api.restaurarSessao(); bloco(banco, api, AlimentosRepositorio(api, banco))
            }
        }
    }
}
