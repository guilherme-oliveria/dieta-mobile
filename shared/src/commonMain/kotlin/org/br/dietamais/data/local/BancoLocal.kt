package org.br.dietamais.data.local

import app.cash.sqldelight.db.SqlDriver

class BancoLocal(private val driver: SqlDriver) : AutoCloseable {
    val banco = BancoDieta(driver)

    override fun close() = driver.close()
}
