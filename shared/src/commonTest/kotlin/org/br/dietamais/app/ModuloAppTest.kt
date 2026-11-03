package org.br.dietamais.app

import org.br.dietamais.plataforma.Config
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ModuloAppTest {
    @Test
    fun resolveConfiguracaoDoAmbienteSemSubstituirPeloPadraoLocal() {
        val config = Config(URL_API = "https://api.exemplo.test")
        val app = koinApplication { modules(moduloApp(config)) }

        try {
            assertSame(config, app.koin.get<Config>())
            assertEquals("https://api.exemplo.test", app.koin.get<Config>().URL_API)
        } finally {
            app.close()
        }
    }
}
