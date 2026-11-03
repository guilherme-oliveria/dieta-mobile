package org.br.dietamais.data.sync

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import org.br.dietamais.data.local.BancoDieta
import org.br.dietamais.data.remoto.JsonContrato
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.EstadoPendencia
import org.br.dietamais.data.repositorio.PendenciasRepositorio
import org.br.dietamais.dominio.pendencias.diferencas
import kotlin.test.*
import kotlin.uuid.Uuid

class EscritaOfflineTest {
    private val usuario = Uuid.parse("019a0f32-5000-7000-8000-000000000003")
    private val contexto = ContextoLeituraSync(usuario, Perfil.NUTRICIONISTA, "epoca")
    private val id = Uuid.parse("019a0f32-5000-7000-8000-0000000000aa")

    private class Fonte(var resposta: (List<MutacaoPendente>) -> List<ResultadoMutacaoResposta>) : FonteEnvioSync {
        override suspend fun enviar(lote: List<MutacaoPendente>) = resposta(lote)
    }

    private fun comBanco(bloco: suspend (BancoDieta) -> Unit) = runTest {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
            BancoDieta.Schema.create(driver)
            val banco = BancoDieta(driver); banco.sessaoQueries.salvar(usuario.toString(), "NUTRICIONISTA")
            bloco(banco)
        }
    }

    private fun obj(vararg p: Pair<String, Any?>) = JsonObject(p.associate { (k, v) -> k to when (v) { null -> JsonNull; is Number -> JsonPrimitive(v); else -> JsonPrimitive(v.toString()) } })
    private fun registro(banco: BancoDieta, colecao: String = "consultas") =
        banco.registroSyncQueries.dadosDe(usuario.toString(), "NUTRICIONISTA", colecao, id.toString()).executeAsOneOrNull()?.dados_json?.let { Json.parseToJsonElement(it).jsonObject }
    private fun semear(banco: BancoDieta, dados: JsonObject) =
        banco.registroSyncQueries.salvar(usuario.toString(), "NUTRICIONISTA", "consultas", id.toString(), 7, 0, dados.toString())

    @Test fun criarGravaRegistroEMutacaoEPedeEnvio() = comBanco { banco ->
        var pedidos = 0
        EscritaOffline(banco, aoEnfileirar = { pedidos++ }).criar("consultas", id, obj("titulo" to "A"), obj("id" to id, "titulo" to "A", "versao" to 1).toString())
        assertEquals(1, pedidos)
        assertEquals("A", registro(banco)?.get("titulo")?.jsonPrimitive?.content)
        val m = FilaMutacoes(banco).pendentes().single()
        assertEquals(OperacaoMutacao.CRIAR, m.operacao)
    }

    @Test fun atualizarGuardaAnteriorEAplicadaLimpa() = comBanco { banco ->
        semear(banco, obj("titulo" to "velho", "versao" to 3))
        val escrita = EscritaOffline(banco)
        escrita.atualizar("consultas", id, 3, obj("titulo" to "novo"), obj("titulo" to "novo", "versao" to 4).toString())
        escrita.atualizar("consultas", id, 4, obj("titulo" to "novíssimo"), obj("titulo" to "novíssimo", "versao" to 5).toString())
        assertEquals("velho", registro(banco, ANTERIOR + "consultas")?.get("titulo")?.jsonPrimitive?.content)
        val fonte = Fonte { l -> l.map { ResultadoMutacaoResposta(it.idMutacao, StatusMutacao.APLICADA, 1) } }
        EnviadorFila(fonte, banco, { contexto }).enviar()
        assertNull(registro(banco, ANTERIOR + "consultas"))
        assertEquals("novíssimo", registro(banco)?.get("titulo")?.jsonPrimitive?.content)
    }

    @Test fun puxarNaoSobrescreveEdicaoLocalAberta() = comBanco { banco ->
        semear(banco, obj("titulo" to "velho", "versao" to 3))
        EscritaOffline(banco).atualizar("consultas", id, 3, obj("titulo" to "novo"), obj("titulo" to "novo", "versao" to 4).toString())
        val armazem = ArmazemLeituraSql(banco) { contexto }
        assertTrue(banco.mutacaoPendenteQueries.abertaDoRegistro(usuario.toString(), "NUTRICIONISTA", "consultas", id.toString()).executeAsOne() > 0)
        assertNotNull(armazem)
    }

    @Test fun conflitoManterMinhaReenfileiraComVersaoDoServidor() = comBanco { banco ->
        semear(banco, obj("titulo" to "velho", "versao" to 3))
        EscritaOffline(banco).atualizar("consultas", id, 3, obj("titulo" to "meu"), obj("titulo" to "meu", "versao" to 4).toString())
        val servidor = obj("id" to id, "titulo" to "dele", "versao" to 9)
        EnviadorFila(Fonte { l -> l.map { ResultadoMutacaoResposta(it.idMutacao, StatusMutacao.CONFLITO, codigo = "CONFLITO_VERSAO", atual = servidor) } }, banco, { contexto }).enviar()
        var pedidos = 0
        val repo = PendenciasRepositorio(banco) { pedidos++ }
        val p = repo.observar().first().single()
        assertEquals(EstadoPendencia.CONFLITO, p.estado)
        assertEquals("dele", p.servidor?.get("titulo")?.jsonPrimitive?.content)
        assertEquals(listOf("titulo"), diferencas(p.minha, p.servidor).map { it.nome })
        repo.manterMinha(p.idMutacao)
        assertEquals(1, pedidos)
        val m = FilaMutacoes(banco).pendentes().single()
        assertEquals(9L, m.versaoBase)
        assertEquals(10L, registro(banco)?.get("versao")?.jsonPrimitive?.long)
        assertEquals(EstadoPendencia.AGUARDANDO, repo.observar().first().single().estado)
    }

    @Test fun conflitoUsarServidorSubstituiERemoveTudo() = comBanco { banco ->
        semear(banco, obj("titulo" to "velho", "versao" to 3))
        EscritaOffline(banco).atualizar("consultas", id, 3, obj("titulo" to "meu"), obj("titulo" to "meu", "versao" to 4).toString())
        val servidor = obj("id" to id, "titulo" to "dele", "versao" to 9)
        EnviadorFila(Fonte { l -> l.map { ResultadoMutacaoResposta(it.idMutacao, StatusMutacao.CONFLITO, atual = servidor) } }, banco, { contexto }).enviar()
        val repo = PendenciasRepositorio(banco)
        repo.usarServidor(repo.observar().first().single().idMutacao)
        assertEquals("dele", registro(banco)?.get("titulo")?.jsonPrimitive?.content)
        assertTrue(repo.observar().first().isEmpty()); assertNull(registro(banco, ANTERIOR + "consultas"))
        assertTrue(banco.conflitoQueries.daSessaoAtual().executeAsList().isEmpty())
    }

    @Test fun rejeitadaRestauraAnteriorEDescartarLimpa() = comBanco { banco ->
        semear(banco, obj("titulo" to "velho", "versao" to 3))
        EscritaOffline(banco).atualizar("consultas", id, 3, obj("titulo" to "meu"), obj("titulo" to "meu", "versao" to 4).toString())
        EnviadorFila(Fonte { l -> l.map { ResultadoMutacaoResposta(it.idMutacao, StatusMutacao.REJEITADA, codigo = "REGRA") } }, banco, { contexto }).enviar()
        assertEquals("velho", registro(banco)?.get("titulo")?.jsonPrimitive?.content)
        val repo = PendenciasRepositorio(banco)
        assertEquals(EstadoPendencia.REJEITADA, repo.observar().first().single().estado)
        repo.descartar(repo.observar().first().single().idMutacao)
        assertTrue(repo.observar().first().isEmpty()); assertNull(registro(banco, ANTERIOR + "consultas"))
    }

    @Test fun descartarCriacaoRemoveORegistro() = comBanco { banco ->
        EscritaOffline(banco).criar("consultas", id, obj("titulo" to "A"), obj("id" to id, "titulo" to "A").toString())
        val repo = PendenciasRepositorio(banco)
        repo.descartar(repo.observar().first().single().idMutacao)
        assertNull(registro(banco)); assertTrue(FilaMutacoes(banco).pendentes().isEmpty())
    }

    @Test fun pacienteOtimistaApareceNoCacheESomeQuandoRejeitado() = comBanco { banco ->
        val ficha = PacienteResposta(id, id, "Ana", status = StatusVinculo.CONVIDADO, versao = 0, criadoEm = kotlin.time.Instant.parse("2026-10-28T12:00:00Z"), atualizadoEm = kotlin.time.Instant.parse("2026-10-28T12:00:00Z"))
        EscritaOffline(banco).criar("pacientes", id, obj("id" to id, "nome" to "Ana"), JsonContrato.encodeToString(ficha))
        assertEquals(1, banco.pacienteCacheQueries.daSessao().executeAsList().size)
        EnviadorFila(Fonte { l -> l.map { ResultadoMutacaoResposta(it.idMutacao, StatusMutacao.REJEITADA, codigo = "LIMITE_PACIENTES_ATINGIDO") } }, banco, { contexto }).enviar()
        assertTrue(banco.pacienteCacheQueries.daSessao().executeAsList().isEmpty())
        val p = PendenciasRepositorio(banco).observar().first().single()
        assertEquals("LIMITE_PACIENTES_ATINGIDO", p.codigo)
    }
}
