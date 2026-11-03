package org.br.dietamais.plataforma

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import org.br.dietamais.data.local.BancoDieta

actual class FabricaDriver(
    private val diretorio: String = File(requireNotNull(System.getProperty("user.home")), ".dietamais").path,
) {
    actual fun criar(): SqlDriver {
        val pasta = File(diretorio)
        check(pasta.isDirectory || pasta.mkdirs()) { "Não foi possível preparar o armazenamento local." }
        val driver = JdbcSqliteDriver("jdbc:sqlite:${File(pasta, NOME_BANCO_LOCAL).absolutePath}")
        try {
            prepararSchema(driver)
            return driver
        } catch (falha: Throwable) {
            driver.close()
            throw falha
        }
    }

    private fun prepararSchema(driver: SqlDriver) {
        BancoDieta(driver).transaction {
            val atual = driver.executeQuery(null, "PRAGMA user_version", { cursor ->
                cursor.next()
                QueryResult.Value(cursor.getLong(0) ?: 0L)
            }, 0).value
            val destino = BancoDieta.Schema.version
            check(atual <= destino) { "O banco local requer uma versão mais recente do aplicativo." }
            when {
                atual == 0L -> BancoDieta.Schema.create(driver)
                atual < destino -> BancoDieta.Schema.migrate(driver, atual, destino)
            }
            driver.execute(null, "PRAGMA user_version = $destino", 0)
        }
    }
}
