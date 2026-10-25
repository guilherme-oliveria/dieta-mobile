package org.br.dietamais.plataforma

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import org.br.dietamais.data.local.BancoDieta

actual class FabricaDriver(private val contexto: Context) {
    actual fun criar(): SqlDriver = AndroidSqliteDriver(BancoDieta.Schema, contexto.applicationContext, NOME_BANCO_LOCAL)
}
