package org.br.dietamais.ui

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.br.dietamais.App
import org.br.dietamais.app.AmbienteDieta
import org.br.dietamais.data.local.BancoDieta
import org.br.dietamais.data.local.BancoLocal
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.plataforma.*
import kotlin.test.*
import java.io.File
import javax.imageio.ImageIO

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class ShellSyncIntegracaoTest {
    @Test fun shellComDiECacheRealMantemPacienteLegivelComFonteDuzentosPorCento() = runSkikoComposeUiTest(
        size = Size(390f, 844f), density = Density(1f, 2f)) {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY); BancoDieta.Schema.create(driver)
        val local = BancoLocal(driver); val config = Config("https://api.example.invalid")
        val api = ApiDieta(config, "1.0", Plataforma.DESKTOP, CofreMemoria(recursoContrato("entrar/resposta.json")), local.banco, MockEngine { req ->
            val arquivo = when (req.url.encodedPath) {
                "/api/v1/eu" -> "consultarEu/resposta.json"
                "/api/v1/grupos-alimentos" -> "fase-2/listarGruposAlimentos/resposta.json"
                "/api/v1/consultas" -> return@MockEngine respond("""{"itens":[]}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json")) // Início da nutri
                "/api/v1/alimentos" -> if (req.url.parameters["ativo"] == "true") "fase-2/listarAlimentos/resposta.json"
                    else return@MockEngine respond(JsonContrato.encodeToString(PaginaAlimentosResposta(emptyList(), 0, 20, 0)), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                else -> { assertEquals("/api/v1/sync", req.url.encodedPath); "puxarMudancas/resposta.json" }
            }
            respond(recursoContrato(arquivo), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        })
        val rede = object : ObservadorConectividade {
            override val estado = MutableStateFlow(EstadoConexao.DISPONIVEL)
            override fun close() = Unit
        }
        val ambiente = AmbienteDieta(config, local, api, rede)
        var visivel by mutableStateOf(true)
        try {
            runBlocking { ambiente.sincronizarAgora() }
            setContent { if (visivel) {
                val identidade = remember { IdentidadeDispositivo() }
                App(ambiente = ambiente, identidadeDispositivo = identidade)
            } }
            waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Pacientes").fetchSemanticsNodes().isNotEmpty() }
            onNodeWithText("Pacientes").performClick()
            onNodeWithTag("lista-pacientes").performScrollToNode(hasText("Paciente de Exemplo", substring = true))
            val nome = onNodeWithText("Paciente de Exemplo", useUnmergedTree = true).performScrollTo().assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            val navegacao = onNodeWithContentDescription("Navegação principal").fetchSemanticsNode().boundsInRoot
            assertTrue(nome.bottom <= navegacao.top, "O cabeçalho global não deve encobrir a lista acima da navegação inferior")
            onNodeWithText("Atualizar dados").performScrollTo().assertIsDisplayed()
            val arquivo = File("build/reports/sincronizacao/shell-390-fonte-2.0.png")
            arquivo.parentFile.mkdirs(); ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", arquivo)
            onNodeWithText("Alimentos").performClick()
            waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Novo alimento") and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
            onNodeWithText("Novo alimento").performScrollTo().performClick()
            val salvar = onNodeWithText("Salvar alimento").performScrollTo().assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            assertTrue(salvar.bottom <= navegacao.top)
            val alimentos = File("build/reports/alimentos/shell-390-fonte-2.0.png")
            alimentos.parentFile.mkdirs(); ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", alimentos)
        } finally {
            try { runOnUiThread { visivel = false }; waitForIdle(); runBlocking { ambiente.encerrar() } }
            finally { Dispatchers.resetMain() }
        }
    }
}
