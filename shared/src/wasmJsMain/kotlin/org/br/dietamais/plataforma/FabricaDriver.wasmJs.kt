package org.br.dietamais.plataforma

import app.cash.sqldelight.db.SqlDriver

/** Provisório (MOB-40): a web não tem banco local. MOB-41 move `FabricaDriver` para o `offlineMain` e este arquivo some. */
actual class FabricaDriver {
    actual fun criar(): SqlDriver = error("A web não usa banco local.")
}
