package org.br.dietamais.data.sync

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.br.dietamais.data.local.BancoDieta
import org.br.dietamais.data.remoto.FalhaComunicacao
import org.br.dietamais.data.remoto.dto.*
import kotlin.test.*
import kotlin.uuid.Uuid

class EnviadorFilaTest {
    private val usuario = Uuid.parse("019a0f32-5000-7000-8000-000000000002")
    private val contexto = ContextoLeituraSync(usuario, Perfil.PACIENTE, "epoca")

    private class Fonte(var resposta: (List<MutacaoPendente>) -> List<ResultadoMutacaoResposta>) : FonteEnvioSync {
        var lotes = mutableListOf<List<MutacaoPendente>>()
        override suspend fun enviar(lote: List<MutacaoPendente>): List<ResultadoMutacaoResposta> { lotes += lote; return resposta(lote) }
    }

    private fun comBanco(bloco: suspend (BancoDieta) -> Unit) = runTest {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
            BancoDieta.Schema.create(driver)
            val banco = BancoDieta(driver); banco.sessaoQueries.salvar(usuario.toString(), "PACIENTE")
            bloco(banco)
        }
    }

    private fun enfileirar(banco: BancoDieta, entidade: String = "agua"): Uuid {
        val id = novoIdLocal()
        FilaMutacoes(banco).gravarEEnfileirar(NovaMutacao(entidade, OperacaoMutacao.CRIAR, id, buildJsonObject { put("ml", JsonPrimitive(250)) })) {
            registroSyncQueries.salvar(usuario.toString(), "PACIENTE", entidade, id.toString(), 0, 0, "{\"ml\":250}")
        }
        return id
    }
    private fun local(banco: BancoDieta, id: Uuid) = banco.registroSyncQueries.daSessao("agua").executeAsList().size.also { assertNotNull(id) }

    @Test fun aplicadaSaiDaFilaEMantemORegistroLocal() = comBanco { banco ->
        val id = enfileirar(banco)
        val fonte = Fonte { l -> l.map { ResultadoMutacaoResposta(it.idMutacao, StatusMutacao.APLICADA, 1) } }
        assertEquals(ResultadoEnvio.CONCLUIDO, EnviadorFila(fonte, banco, { contexto }).enviar())
        assertTrue(FilaMutacoes(banco).pendentes().isEmpty()); assertEquals(1, local(banco, id))
        assertEquals(ResultadoEnvio.VAZIA, EnviadorFila(fonte, banco, { contexto }).enviar())
    }

    @Test fun rejeitadaDesfazACriacaoLocal() = comBanco { banco ->
        val id = enfileirar(banco)
        val fonte = Fonte { l -> l.map { ResultadoMutacaoResposta(it.idMutacao, StatusMutacao.REJEITADA, codigo = "VALOR_FORA_DO_INTERVALO") } }
        EnviadorFila(fonte, banco, { contexto }).enviar()
        assertTrue(FilaMutacoes(banco).pendentes().isEmpty()); assertEquals(0, local(banco, id))
        assertEquals("REJEITADA", banco.mutacaoPendenteQueries.doEscopo(usuario.toString(), "PACIENTE").executeAsList().single().status)
    }

    @Test fun conflitoGuardaEstadoDoServidor() = comBanco { banco ->
        enfileirar(banco)
        val fonte = Fonte { l -> l.map { ResultadoMutacaoResposta(it.idMutacao, StatusMutacao.CONFLITO, codigo = "CONFLITO_VERSAO", atual = buildJsonObject { put("versao", JsonPrimitive(5)) }) } }
        EnviadorFila(fonte, banco, { contexto }).enviar()
        val m = banco.mutacaoPendenteQueries.doEscopo(usuario.toString(), "PACIENTE").executeAsList().single()
        assertEquals("CONFLITO", m.status)
        assertNotNull(banco.conflitoQueries.buscar(m.id_mutacao, usuario.toString(), "PACIENTE").executeAsOneOrNull())
    }

    @Test fun semRedeMantemNaFilaEContaTentativa() = comBanco { banco ->
        enfileirar(banco)
        val fonte = Fonte { throw FalhaComunicacao() }
        assertEquals(ResultadoEnvio.SEM_REDE, EnviadorFila(fonte, banco, { contexto }).enviar())
        val pendente = FilaMutacoes(banco).pendentes().single()
        assertEquals(1, pendente.tentativas)
    }

    @Test fun respostaIncompletaNaoDescartaNada() = comBanco { banco ->
        enfileirar(banco); enfileirar(banco)
        val fonte = Fonte { l -> listOf(ResultadoMutacaoResposta(l.first().idMutacao, StatusMutacao.APLICADA, 1)) }
        assertEquals(ResultadoEnvio.FALHOU, EnviadorFila(fonte, banco, { contexto }).enviar())
        assertEquals(2, FilaMutacoes(banco).pendentes().size)
    }

    @Test fun enviaEmLotesNaOrdemDaFila() = comBanco { banco ->
        repeat(5) { enfileirar(banco) }
        val fonte = Fonte { l -> l.map { ResultadoMutacaoResposta(it.idMutacao, StatusMutacao.APLICADA, 1) } }
        EnviadorFila(fonte, banco, { contexto }, tamanhoLote = 2).enviar()
        assertEquals(listOf(2, 2, 1), fonte.lotes.map { it.size })
        assertEquals(fonte.lotes.flatten().map { it.idMutacao.toString() }.sorted(), fonte.lotes.flatten().map { it.idMutacao.toString() })
    }

    @Test fun semSessaoOuSessaoTrocadaNaoEnvia() = comBanco { banco ->
        enfileirar(banco)
        val fonte = Fonte { l -> l.map { ResultadoMutacaoResposta(it.idMutacao, StatusMutacao.APLICADA, 1) } }
        assertEquals(ResultadoEnvio.SEM_SESSAO, EnviadorFila(fonte, banco, { null }).enviar())
        assertTrue(fonte.lotes.isEmpty())
    }
}
