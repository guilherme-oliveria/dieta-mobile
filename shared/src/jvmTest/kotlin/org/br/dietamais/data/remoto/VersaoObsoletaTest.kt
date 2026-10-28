package org.br.dietamais.data.remoto

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import org.br.dietamais.data.local.BancoDieta
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.plataforma.Config
import kotlin.test.*

class VersaoObsoletaTest {
    private val problema = """{"type":"about:blank","title":"Atualização necessária","status":426,"detail":"Atualize o app.","codigo":"VERSAO_APP_OBSOLETA","campos":[]}"""

    @Test fun resposta426LigaOAvisoDeAtualizacaoEEnviaAVersao() = runTest {
        var versaoEnviada: String? = null
        val engine = MockEngine { req ->
            versaoEnviada = req.headers["X-App-Versao"]
            respond(problema, HttpStatusCode.UpgradeRequired, headersOf(HttpHeaders.ContentType, "application/problem+json"))
        }
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
            BancoDieta.Schema.create(driver)
            ApiDieta(Config("https://api.example.invalid"), "1.0", Plataforma.DESKTOP, CofreMemoria(), BancoDieta(driver), engine).use { api ->
                assertFalse(api.versaoObsoleta.value)
                val erro = assertFailsWith<ErroApi> { api.consultarTermosUso() }
                assertEquals("VERSAO_APP_OBSOLETA", erro.codigo); assertEquals("1.0", versaoEnviada)
                assertTrue(api.versaoObsoleta.value)
            }
        }
    }
}
