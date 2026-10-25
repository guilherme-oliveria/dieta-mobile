package org.br.dietamais.data.local

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SnapshotSchemaTest {
    @Test
    fun snapshotVersionadoContemSomenteEstruturaSemDados() {
        val arquivo = File("src/commonMain/sqldelight/schemas/1.db")
        assertTrue(arquivo.isFile, "O snapshot inicial deve existir para verificar migrações")
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
