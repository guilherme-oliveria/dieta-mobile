package org.br.dietamais.data.remoto

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.http.content.OutgoingContent
import kotlinx.coroutines.test.runTest
import org.br.dietamais.data.local.BancoDieta
import org.br.dietamais.data.remoto.dto.Plataforma
import org.br.dietamais.data.repositorio.PerfilNutriRepositorio
import org.br.dietamais.plataforma.Config
import kotlin.test.*

/** MOB-33b: a logo vai como corpo cru (PNG ou JPEG) em `PUT /eu/nutri/logo`, e sai com `DELETE`; o backend confere pelos bytes. */
class PerfilNutriLogoTest {
    private val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 9, 8, 7)

    private fun comRepositorio(engine: MockEngine, bloco: suspend (PerfilNutriRepositorio) -> Unit) = runTest {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
            BancoDieta.Schema.create(driver)
            ApiDieta(Config("https://api.example.invalid"), "1.0", Plataforma.DESKTOP, CofreMemoria(recursoContrato("entrar/resposta.json")), BancoDieta(driver), engine).use { api ->
                api.usuarioAtual() // carrega a sessão do cofre
                bloco(PerfilNutriRepositorio(api))
            }
        }
    }

    @Test fun enviaOsBytesCrusComOTipoDaImagem() {
        var visto: Triple<HttpMethod, String, ByteArray>? = null
        comRepositorio(MockEngine { req ->
            visto = Triple(req.method, req.url.encodedPath, (req.body as OutgoingContent.ByteArrayContent).bytes())
            assertEquals(ContentType.Image.PNG, req.body.contentType)
            assertNotNull(req.headers[HttpHeaders.Authorization])
            respond("", HttpStatusCode.NoContent)
        }) { it.enviarLogo(png, "image/png") }
        assertEquals(HttpMethod.Put, visto!!.first); assertEquals("/api/v1/eu/nutri/logo", visto!!.second); assertContentEquals(png, visto!!.third)
    }

    @Test fun removeComDelete() {
        var visto: Pair<HttpMethod, String>? = null
        comRepositorio(MockEngine { req -> visto = req.method to req.url.encodedPath; respond("", HttpStatusCode.NoContent) }) { it.removerLogo() }
        assertEquals(HttpMethod.Delete to "/api/v1/eu/nutri/logo", visto)
    }

    @Test fun recusaDoBackendViraErroDaApi() {
        comRepositorio(MockEngine {
            respond("""{"type":"about:blank","title":"Inválido","status":422,"detail":"Imagem inválida.","codigo":"FORMATO_INVALIDO","campos":[]}""", HttpStatusCode.UnprocessableEntity,
                headersOf(HttpHeaders.ContentType, "application/problem+json"))
        }) { repositorio -> assertEquals("FORMATO_INVALIDO", assertFailsWith<ErroApi> { repositorio.enviarLogo(png, "image/png") }.codigo) }
    }
}
