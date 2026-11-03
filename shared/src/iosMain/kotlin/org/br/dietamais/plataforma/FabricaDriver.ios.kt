package org.br.dietamais.plataforma

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import org.br.dietamais.data.local.BancoDieta

actual class FabricaDriver {
    actual fun criar(): SqlDriver = NativeSqliteDriver(BancoDieta.Schema, NOME_BANCO_LOCAL)
}
