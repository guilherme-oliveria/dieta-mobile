package org.br.dietamais.plataforma

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.*

@RunWith(AndroidJUnit4::class)
class ArmazenamentoSeguroAndroidTest {
    @Test
    fun recuperaBackupAtomicoQuandoArquivoBaseNaoExiste() = runBlocking {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val cofre = ArmazenamentoSeguro(contexto)
        val arquivo = File(contexto.noBackupFilesDir, "sessao.cifrada")
        try {
            cofre.gravar("TOKEN_FICTICIO_TESTE_BACKUP")
            assertTrue(arquivo.renameTo(File(arquivo.path + ".bak")))
            assertFalse(arquivo.exists())
            assertEquals("TOKEN_FICTICIO_TESTE_BACKUP", cofre.ler())
            assertTrue(arquivo.exists())
        } finally { cofre.limpar() }
    }

    @Test
    fun keystoreProtegeArquivoERecusaAdulteracao() = runBlocking {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val cofre = ArmazenamentoSeguro(contexto)
        val arquivo = File(contexto.noBackupFilesDir, "sessao.cifrada")
        try {
            cofre.gravar("TOKEN_FICTICIO_TESTE_ANDROID")
            assertFalse(arquivo.readBytes().toString(Charsets.UTF_8).contains("TOKEN_FICTICIO_TESTE_ANDROID"))
            assertEquals("TOKEN_FICTICIO_TESTE_ANDROID", ArmazenamentoSeguro(contexto).ler())
            val adulterado = arquivo.readBytes()
            adulterado[adulterado.lastIndex] = (adulterado.last().toInt() xor 1).toByte()
            arquivo.writeBytes(adulterado)
            assertFailsWith<ArmazenamentoSeguroIndisponivel> { cofre.ler() }
        } finally { cofre.limpar() }
        assertNull(cofre.ler())
    }
}
