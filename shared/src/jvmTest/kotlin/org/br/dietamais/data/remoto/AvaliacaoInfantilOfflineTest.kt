package org.br.dietamais.data.remoto

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.br.dietamais.data.local.BancoDieta
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.AvaliacaoRepositorio
import org.br.dietamais.data.sync.*
import org.br.dietamais.plataforma.Config
import kotlin.test.*
import kotlin.time.Instant
import kotlin.uuid.Uuid

class AvaliacaoInfantilOfflineTest {
    private val dia = Instant.parse("2026-11-01T12:00:00Z")
    private val paciente = Uuid.random()
    private val dados = DadosInfantisAvaliacaoDto(3301, "ESCORE_Z", "BRANCA", 1.234)
    private fun antiga() = AvaliacaoResposta(Uuid.random(), paciente, paciente, dia, 10.0, 0.811, 15.2, ClassificacaoImc.NORMAL,
        versao = 3, criadoEm = dia, atualizadoEm = dia, faixaEtaria = FaixaEtariaAvaliacao.CRIANCA, infantil = dados,
        resultadosInfantis = ResultadosInfantisAvaliacaoDto(idadeMeses = 12, energia = EnergiaInfantilDto(getRdiKcal = 810.0)))

    private suspend fun comBanco(engine: MockEngine, bloco: suspend (AvaliacaoRepositorio, ApiDieta, BancoDieta) -> Unit) {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
            BancoDieta.Schema.create(driver); val banco = BancoDieta(driver)
            ApiDieta(Config("https://app.example.invalid"), "1.0", Plataforma.DESKTOP, CofreMemoria(recursoContrato("entrar/resposta.json")), banco, engine).use { api ->
                api.restaurarSessao(); bloco(AvaliacaoRepositorio(api, banco, agora = { dia }), api, banco)
            }
        }
    }

    @Test fun criacaoGuardaEntradasEFilaSemCalcularResultadosDoServidor() = runTest {
        comBanco(MockEngine { error("A escrita deve funcionar sem rede") }) { repo, _, banco ->
            val req = SalvarAvaliacaoRequisicao(dataHora = dia, pesoKg = 3.25, estaturaM = 0.485, infantil = dados)
            val nova = repo.criar(paciente, req)
            val lida = repo.observar(paciente).first().single()
            assertEquals(dados, lida.infantil); assertNull(lida.resultadosInfantis)
            assertEquals(0.485, lida.estaturaM)
            val mutacao = FilaMutacoes(banco).pendentes().single()
            assertEquals(nova.id, mutacao.id)
            assertEquals(dados, JsonContrato.decodeFromString<SalvarAvaliacaoRequisicao>(mutacao.dados.toString()).infantil)
            assertFalse(mutacao.dados.containsKey("resultadosInfantis"))
        }
    }

    @Test fun editarPreservaEntradasOmitidasERemoveSnapshotAntigo() = runTest {
        val a = antiga()
        comBanco(MockEngine { error("A escrita deve funcionar sem rede") }) { repo, api, banco ->
            val usuario = api.sessao.value!!.usuarioId.toString()
            banco.registroSyncQueries.salvar(usuario, "NUTRICIONISTA", "avaliacoes", a.id.toString(), 3, 0, JsonContrato.encodeToString(a))
            repo.salvar(a.id, 3, SalvarAvaliacaoRequisicao(dataHora = dia, pesoKg = 11.0, estaturaM = 0.811))
            val nova = repo.observar(paciente).first().single()
            assertEquals(dados, nova.infantil); assertNull(nova.resultadosInfantis); assertEquals(11.0, nova.pesoKg)
            assertEquals(1, FilaMutacoes(banco).pendentes().size)
        }
    }

    @Test fun refreshESyncNaoApagamPendenteESnapshotSoVoltaAposConfirmacao() = runTest {
        val a = antiga()
        val servidor = a.copy(versao = 100)
        comBanco(MockEngine { respond(JsonContrato.encodeToString(AvaliacoesResposta(listOf(servidor))), HttpStatusCode.OK,
            headersOf(HttpHeaders.ContentType, "application/json")) }) { repo, api, banco ->
            val usuario = api.sessao.value!!.usuarioId
            banco.registroSyncQueries.salvar(usuario.toString(), "NUTRICIONISTA", "avaliacoes", a.id.toString(), 3, 0, JsonContrato.encodeToString(a))
            repo.salvar(a.id, 3, SalvarAvaliacaoRequisicao(dataHora = dia, pesoKg = 11.0, estaturaM = 0.811, infantil = dados))
            repo.atualizar(paciente)
            assertEquals(11.0, repo.observar(paciente).first().single().pesoKg)
            val contexto = ContextoLeituraSync(usuario, Perfil.NUTRICIONISTA, "teste")
            val leitura = ArmazemLeituraSql(banco) { contexto }
            fun pagina(av: AvaliacaoResposta) = SyncResposta("100", false, MudancasFase1Resposta(emptyList(), emptyList(), emptyList(),
                avaliacoes = listOf(MudancaSyncResposta(av.id, av.versao, false, av))))
            leitura.aplicar(contexto, pagina(servidor))
            assertNull(repo.observar(paciente).first().single().resultadosInfantis)
            banco.mutacaoPendenteQueries.limpar() // simula a confirmação da única mutação deste banco em memória
            val confirmada = servidor.copy(pesoKg = 11.0, resultadosInfantis = ResultadosInfantisAvaliacaoDto(idadeMeses = 12, energia = EnergiaInfantilDto(getRdiKcal = 899.0)))
            leitura.aplicar(contexto, pagina(confirmada))
            assertEquals(899.0, repo.observar(paciente).first().single().resultadosInfantis?.energia?.getRdiKcal)
        }
    }
}
