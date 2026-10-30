package org.br.dietamais.data.local

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Dado clínico não pode sobrar no aparelho depois do logout (LGPD). Enche TODAS as tabelas do schema atual e confere que
 * `ArmazemSessaoSql.limpar()` esvazia todas. Tabela nova sem linha de exemplo aqui, ou fora da limpeza, faz o teste falhar.
 */
class LimpezaLogoutTest {
    /** Uma linha válida (respeitando os CHECK) por tabela. */
    private val exemplos = mapOf(
        "sessao" to "INSERT INTO sessao VALUES (1, 'conta-ficticia', 'NUTRICIONISTA')",
        "cursor_sync" to "INSERT INTO cursor_sync VALUES ('conta-ficticia', 'NUTRICIONISTA', '3')",
        "registro_sync" to "INSERT INTO registro_sync VALUES ('conta-ficticia', 'NUTRICIONISTA', 'vinculos', 'id-ficticio', 1, 0, '{}')",
        "mutacao_pendente" to "INSERT INTO mutacao_pendente (id_mutacao, usuario_id, perfil, entidade, operacao, id, dados_json) VALUES ('m-ficticia', 'conta-ficticia', 'NUTRICIONISTA', 'teste', 'CRIAR', 'id-ficticio', '{}')",
        "paciente_cache" to "INSERT INTO paciente_cache (usuario_id, perfil, vinculo_id, paciente_id, conteudo_json) VALUES ('conta-ficticia', 'NUTRICIONISTA', 'v-ficticio', 'p-ficticio', '{}')",
        "alimento_cache" to "INSERT INTO alimento_cache (usuario_id, perfil, colecao, id, conteudo_json) VALUES ('conta-ficticia', 'NUTRICIONISTA', 'alimentos', 'id-ficticio', '{}')",
        "conflito" to "INSERT INTO conflito (id_mutacao, usuario_id, perfil, atual_json) VALUES ('m-ficticia', 'conta-ficticia', 'NUTRICIONISTA', '{}')",
    )

    private fun <T> SqlDriver.consultar(sql: String, ler: (SqlCursor) -> T): List<T> =
        executeQuery(null, sql, { cursor ->
            val itens = mutableListOf<T>()
            while (cursor.next().value) itens += ler(cursor)
            QueryResult.Value(itens)
        }, 0).value

    private fun contar(driver: SqlDriver, tabela: String): Long = driver.consultar("SELECT COUNT(*) FROM \"$tabela\"") { it.getLong(0)!! }.single()

    @Test
    fun logoutEsvaziaTodasAsTabelasDoBancoLocal() {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
            BancoDieta.Schema.create(driver).value
            val tabelas = driver.consultar("SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'") { it.getString(0)!! }
            tabelas.forEach { tabela ->
                val insercao = assertNotNull(exemplos[tabela], "tabela nova '$tabela': inclua uma linha de exemplo aqui e confira que o ArmazemSessaoSql.limpar() a esvazia")
                driver.execute(null, insercao, 0)
                assertEquals(1L, contar(driver, tabela), "a linha de exemplo devia entrar em '$tabela'")
            }

            ArmazemSessaoSql(BancoDieta(driver)).limpar()

            tabelas.forEach { assertEquals(0L, contar(driver, it), "o logout deixou dados na tabela '$it'") }
        }
    }
}
