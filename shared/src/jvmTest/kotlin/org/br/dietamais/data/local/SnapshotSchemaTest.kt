package org.br.dietamais.data.local

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SnapshotSchemaTest {
    @Test
    fun migrarSchemaTresPreservaProjecoesCursorESessaoAoAdicionarAlimentos() {
        val arquivo = java.nio.file.Files.createTempFile("dietamais-migracao-alimentos-", ".db").toFile()
        try {
            File("src/commonMain/sqldelight/schemas/3.db").copyTo(arquivo, overwrite = true)
            JdbcSqliteDriver("jdbc:sqlite:${arquivo.absolutePath}").use { driver ->
                driver.execute(null, "INSERT INTO sessao VALUES (1, 'conta-ficticia', 'NUTRICIONISTA')", 0)
                driver.execute(null, "INSERT INTO cursor_sync VALUES ('conta-ficticia','NUTRICIONISTA','3')", 0)
                driver.execute(null, "INSERT INTO registro_sync VALUES ('conta-ficticia','NUTRICIONISTA','vinculos','id-ficticio',1,0,'{}')", 0)
                driver.execute(null, "INSERT INTO mutacao_pendente (id_mutacao,usuario_id,perfil,entidade,operacao,id,dados_json) VALUES ('mutacao-ficticia','conta-ficticia','NUTRICIONISTA','teste','CRIAR','id-ficticio','{}')", 0)
                BancoDieta.Schema.migrate(driver, 3, BancoDieta.Schema.version).value
                val banco = BancoDieta(driver)
                assertEquals("conta-ficticia", banco.sessaoQueries.atual().executeAsOne().usuario_id)
                assertEquals("3", banco.cursorSyncQueries.buscar("conta-ficticia", "NUTRICIONISTA").executeAsOne())
                assertEquals(listOf("{}"), banco.registroSyncQueries.daSessao("vinculos").executeAsList())
                assertEquals(1, banco.mutacaoPendenteQueries.pendentes("conta-ficticia", "NUTRICIONISTA").executeAsList().size)
                assertTrue(banco.alimentoCacheQueries.daSessao("alimentos").executeAsList().isEmpty())
            }
        } finally { arquivo.delete() }
    }

    @Test
    fun snapshotVersionadoContemSomenteEstruturaSemDados() {
        val arquivos = File("src/commonMain/sqldelight/schemas").listFiles { arquivo -> arquivo.extension == "db" }.orEmpty()
        assertTrue(arquivos.isNotEmpty(), "Os snapshots devem existir para verificar migrações")
        arquivos.forEach { arquivo ->
        JdbcSqliteDriver("jdbc:sqlite:${arquivo.absolutePath}").use { driver ->
            val tabelas = driver.executeQuery(
                null, "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'", { cursor ->
                    val nomes = mutableListOf<String>()
                    while (cursor.next().value) nomes += cursor.getString(0)!!
                    QueryResult.Value(nomes)
                }, 0,
            ).value
            assertTrue(tabelas.isNotEmpty())
            tabelas.forEach { tabela ->
                val nomeSeguro = tabela.replace("\"", "\"\"")
                val quantidade = driver.executeQuery(null, "SELECT COUNT(*) FROM \"$nomeSeguro\"", { cursor ->
                    cursor.next()
                    QueryResult.Value(cursor.getLong(0)!!)
                }, 0).value
                assertEquals(0L, quantidade, "O snapshot de migração não pode carregar dados de aplicação")
            }
        }
        }
    }

    @Test
    fun migrarSchemaUmPreservaSessaoEFilaAoAdicionarCachePacientes() {
        val arquivo = java.nio.file.Files.createTempFile("dietamais-migracao-", ".db").toFile()
        try {
            File("src/commonMain/sqldelight/schemas/1.db").copyTo(arquivo, overwrite = true)
            JdbcSqliteDriver("jdbc:sqlite:${arquivo.absolutePath}").use { driver ->
                driver.execute(null, "INSERT INTO sessao VALUES (1, 'conta-ficticia', 'NUTRICIONISTA')", 0)
                driver.execute(null, "INSERT INTO mutacao_pendente (id_mutacao,usuario_id,perfil,entidade,operacao,id,dados_json) VALUES ('mutacao-ficticia','conta-ficticia','NUTRICIONISTA','teste','CRIAR','id-ficticio','{}')", 0)
                BancoDieta.Schema.migrate(driver, 1, BancoDieta.Schema.version).value
                val banco = BancoDieta(driver)
                assertEquals("conta-ficticia", banco.sessaoQueries.atual().executeAsOne().usuario_id)
                assertEquals(1, banco.mutacaoPendenteQueries.pendentes("conta-ficticia", "NUTRICIONISTA").executeAsList().size)
                assertTrue(banco.pacienteCacheQueries.daSessao().executeAsList().isEmpty())
                banco.pacienteCacheQueries.salvar("conta-ficticia", "NUTRICIONISTA", "vinculo-ficticio", "paciente-ficticio", "{}")
                assertEquals(listOf("{}"), banco.pacienteCacheQueries.daSessao().executeAsList())
            }
        } finally { arquivo.delete() }
    }

    @Test
    fun migrarSchemaDoisPreservaCacheEFilaEReiniciaCursorParaNovaProjecao() {
        val arquivo = java.nio.file.Files.createTempFile("dietamais-migracao-sync-", ".db").toFile()
        try {
            File("src/commonMain/sqldelight/schemas/2.db").copyTo(arquivo, overwrite = true)
            JdbcSqliteDriver("jdbc:sqlite:${arquivo.absolutePath}").use { driver ->
                driver.execute(null, "INSERT INTO sessao VALUES (1, 'conta-ficticia', 'NUTRICIONISTA')", 0)
                driver.execute(null, "INSERT INTO cursor_sync VALUES ('conta-ficticia','NUTRICIONISTA','3')", 0)
                driver.execute(null, "INSERT INTO paciente_cache VALUES ('conta-ficticia','NUTRICIONISTA','vinculo-ficticio','paciente-ficticio','{}')", 0)
                driver.execute(null, "INSERT INTO mutacao_pendente (id_mutacao,usuario_id,perfil,entidade,operacao,id,dados_json) VALUES ('mutacao-ficticia','conta-ficticia','NUTRICIONISTA','teste','CRIAR','id-ficticio','{}')", 0)
                BancoDieta.Schema.migrate(driver, 2, BancoDieta.Schema.version).value
                val banco = BancoDieta(driver)
                assertEquals(listOf("{}"), banco.pacienteCacheQueries.daSessao().executeAsList())
                assertEquals(1, banco.mutacaoPendenteQueries.pendentes("conta-ficticia", "NUTRICIONISTA").executeAsList().size)
                assertEquals(null, banco.cursorSyncQueries.buscar("conta-ficticia", "NUTRICIONISTA").executeAsOneOrNull())
                assertTrue(banco.registroSyncQueries.daSessao("vinculos").executeAsList().isEmpty())
            }
        } finally { arquivo.delete() }
    }
}
