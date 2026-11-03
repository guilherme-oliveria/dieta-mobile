package org.br.dietamais.data.sync

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.br.dietamais.data.local.BancoDieta
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class FilaMutacoesTest {
    private val usuarioA = "019a0f32-5000-7000-8000-000000000100"
    private val usuarioB = "019a0f32-5000-7000-8000-000000000101"
    private val idFixo = Uuid.parse("019a0f32-5000-7000-8000-000000000102")

    @Test
    fun confirmaEscritaLocalEPayloadOficialNaMesmaTransacao() = comBanco { driver, banco ->
        val fila = FilaMutacoes(banco)
        val mutacao = novaMutacao()

        val id = fila.gravarEEnfileirar(mutacao) { atualizarRegistro(driver, 1) }

        assertEquals(1, valorRegistro(driver))
        val pendente = fila.pendentes().single()
        assertEquals(id, pendente.idMutacao)
        assertEquals(mutacao.id, pendente.id)
        assertEquals(mutacao.dados, pendente.dados)
        assertEquals(OperacaoMutacao.CRIAR, pendente.operacao)
        assertEquals(0L, pendente.tentativas)
        assertEquals('7', id.toString()[14], "RN-SYN-01: IDs novos devem usar UUIDv7")
    }

    @Test
    fun falhaAoEnfileirarDesfazEscritaLocal() = comBanco { driver, banco ->
        val fila = FilaMutacoes(banco, novoId = { idFixo })
        fila.gravarEEnfileirar(novaMutacao()) { atualizarRegistro(driver, 1) }

        assertFails {
            fila.gravarEEnfileirar(novaMutacao()) { atualizarRegistro(driver, 2) }
        }

        assertEquals(1, valorRegistro(driver))
        assertEquals(1, fila.pendentes().size)
    }

    @Test
    fun falhaDuranteEscritaDesfazMudancaParcialENaoCriaPendencia() = comBanco { driver, banco ->
        val fila = FilaMutacoes(banco)
        assertFailsWith<IllegalStateException> {
            fila.gravarEEnfileirar(novaMutacao()) {
                atualizarRegistro(driver, 3)
                error("Falha simulada na escrita local")
            }
        }
        assertEquals(0, valorRegistro(driver))
        assertTrue(fila.pendentes().isEmpty())
    }

    @Test
    fun filaLeContextoDaSessaoENaoExibeMutacaoDeOutroUsuarioOuPerfil() = comBanco { _, banco ->
        val fila = FilaMutacoes(banco)
        val id = fila.gravarEEnfileirar(novaMutacao()) {}

        banco.sessaoQueries.salvar(usuarioB, "NUTRICIONISTA")
        assertTrue(fila.pendentes().isEmpty())
        banco.sessaoQueries.salvar(usuarioA, "PACIENTE")
        assertTrue(fila.pendentes().isEmpty())
        banco.sessaoQueries.salvar(usuarioA, "NUTRICIONISTA")
        assertEquals(id, fila.pendentes().single().idMutacao)
    }

    @Test
    fun semSessaoNaoExecutaEscritaNemEnfileira() = comBanco(ativa = false) { driver, banco ->
        var executou = false
        val fila = FilaMutacoes(banco)
        assertFailsWith<IllegalStateException> {
            fila.gravarEEnfileirar(novaMutacao()) {
                executou = true
                atualizarRegistro(driver, 1)
            }
        }
        assertFalse(executou)
        assertEquals(0, valorRegistro(driver))
        assertTrue(fila.pendentes().isEmpty())
    }

    @Test
    fun preservaVersaoBaseEOrdemDeCriacaoDasPendencias() = comBanco { _, banco ->
        val fila = FilaMutacoes(banco)
        val primeira = fila.gravarEEnfileirar(novaMutacao()) {}
        val segunda = fila.gravarEEnfileirar(novaMutacao().copy(operacao = OperacaoMutacao.ATUALIZAR, versaoBase = 4L)) {}
        assertEquals(listOf(primeira, segunda), fila.pendentes().map { it.idMutacao })
        assertEquals(listOf(null, 4L), fila.pendentes().map { it.versaoBase })
    }

    @Test
    fun cursorOpacoEConflitoSaoSeparadosPorUsuarioEPerfil() = comBanco { _, banco ->
        val cursor = "9007199254740993"
        val atual = recurso("atualizarPaciente/erro-conflito-versao.json").getValue("atual")
        banco.cursorSyncQueries.salvar(usuarioA, "NUTRICIONISTA", cursor)
        banco.conflitoQueries.salvar(idFixo.toString(), usuarioA, "NUTRICIONISTA", atual.toString())

        assertEquals(cursor, banco.cursorSyncQueries.buscar(usuarioA, "NUTRICIONISTA").executeAsOne())
        assertEquals(null, banco.cursorSyncQueries.buscar(usuarioA, "PACIENTE").executeAsOneOrNull())
        assertEquals(null, banco.conflitoQueries.buscar(idFixo.toString(), usuarioB, "NUTRICIONISTA").executeAsOneOrNull())
        assertEquals(atual, Json.parseToJsonElement(banco.conflitoQueries.buscar(idFixo.toString(), usuarioA, "NUTRICIONISTA").executeAsOne()))
    }

    private fun novaMutacao(): NovaMutacao {
        val dados = recurso("criarPaciente/requisicao.json")
        return NovaMutacao(
            entidade = "pacientes", operacao = OperacaoMutacao.CRIAR,
            id = Uuid.parse(dados.getValue("id").jsonPrimitive.content), dados = dados,
        )
    }

    private fun recurso(nome: String): JsonObject {
        val texto = javaClass.getResourceAsStream("/contrato/$nome")!!.bufferedReader(Charsets.UTF_8).use { it.readText() }
        return Json.parseToJsonElement(texto).jsonObject
    }

    private fun <T> comBanco(ativa: Boolean = true, bloco: (SqlDriver, BancoDieta) -> T): T {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            BancoDieta.Schema.create(driver)
            // Tabela exclusiva do teste: nenhuma entidade clínica futura é inventada para provar atomicidade.
            driver.execute(null, "CREATE TABLE registro_local_teste (valor INTEGER NOT NULL)", 0)
            driver.execute(null, "INSERT INTO registro_local_teste VALUES (0)", 0)
            val banco = BancoDieta(driver)
            if (ativa) banco.sessaoQueries.salvar(usuarioA, "NUTRICIONISTA")
            return bloco(driver, banco)
        } finally {
            driver.close()
        }
    }

    private fun atualizarRegistro(driver: SqlDriver, valor: Int) {
        driver.execute(null, "UPDATE registro_local_teste SET valor = ?", 1) { bindLong(0, valor.toLong()) }
    }

    private fun valorRegistro(driver: SqlDriver): Int = driver.executeQuery(
        null, "SELECT valor FROM registro_local_teste", { cursor ->
            cursor.next()
            QueryResult.Value(cursor.getLong(0)!!.toInt())
        }, 0,
    ).value
}
