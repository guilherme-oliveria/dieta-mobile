package org.br.dietamais.plataforma

import app.cash.sqldelight.db.SqlDriver

expect class FabricaDriver {
    fun criar(): SqlDriver
}

internal const val NOME_BANCO_LOCAL = "dietamais.db"
