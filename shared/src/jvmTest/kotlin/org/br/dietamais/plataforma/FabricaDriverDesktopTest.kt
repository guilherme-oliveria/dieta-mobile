package org.br.dietamais.plataforma

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import java.nio.file.Files
import org.br.dietamais.data.local.BancoDieta
import org.br.dietamais.app.moduloBancoLocal
import org.br.dietamais.data.sync.FilaMutacoes
import org.br.dietamais.data.sync.NovaMutacao
import org.br.dietamais.data.sync.OperacaoMutacao
import org.koin.dsl.koinApplication
import kotlinx.serialization.json.JsonObject
import kotlin.uuid.Uuid
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FabricaDriverDesktopTest {
    @Test
    fun reabrirBancoPersisteMetadadosSemRecriarSchema() {
        val diretorio = novoDiretorio()
        val fabrica = FabricaDriver(diretorio.absolutePath)
        fabrica.criar().use { driver ->
            BancoDieta(driver).sessaoQueries.salvar("019a0f32-5000-7000-8000-000000000100", "NUTRICIONISTA")
        }
        fabrica.criar().use { driver ->
            val sessao = BancoDieta(driver).sessaoQueries.atual().executeAsOne()
            assertEquals("NUTRICIONISTA", sessao.perfil)
            assertEquals("019a0f32-5000-7000-8000-000000000100", sessao.usuario_id)
        }
    }

    @Test
    fun recusaSchemaMaisNovoSemApagarDados() {
        val diretorio = novoDiretorio()
        val fabrica = FabricaDriver(diretorio.absolutePath)
        fabrica.criar().use { driver ->
            BancoDieta(driver).sessaoQueries.salvar("019a0f32-5000-7000-8000-000000000100", "NUTRICIONISTA")
            driver.execute(null, "PRAGMA user_version = 999", 0)
        }
        assertFailsWith<IllegalStateException> { fabrica.criar() }
        JdbcSqliteDriver("jdbc:sqlite:${File(diretorio, NOME_BANCO_LOCAL).absolutePath}").use { driver ->
            assertEquals("NUTRICIONISTA", BancoDieta(driver).sessaoQueries.atual().executeAsOne().perfil)
        }
    }

    @Test
    fun moduloCompartilhaSessaoPersistidaEntreBancoEFila() {
        val app = koinApplication { modules(moduloBancoLocal(FabricaDriver(novoDiretorio().absolutePath))) }
        try {
            val banco = app.koin.get<BancoDieta>()
            banco.sessaoQueries.salvar("019a0f32-5000-7000-8000-000000000100", "NUTRICIONISTA")
            val fila = app.koin.get<FilaMutacoes>()
            val id = fila.gravarEEnfileirar(NovaMutacao(
                entidade = "pacientes", operacao = OperacaoMutacao.CRIAR,
                id = Uuid.parse("019a0f32-5000-7000-8000-000000000103"), dados = JsonObject(emptyMap()),
            )) {}
            assertEquals(id, fila.pendentes().single().idMutacao)
        } finally {
            app.close()
        }
    }

    private fun novoDiretorio(): File {
        val base = File("build/test-db").apply { mkdirs() }
        return Files.createTempDirectory(base.toPath(), "banco-").toFile()
    }
}
