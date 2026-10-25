package org.br.dietamais.plataforma

import java.io.File
import java.nio.file.Files
import java.util.Base64
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class ArmazenamentoSeguroDesktopTest {
    @Test
    fun windowsProtegeReabreELimpaSemTextoPuroNoArquivo() = runTest {
        if (!System.getProperty("os.name").startsWith("Windows")) return@runTest
        val pasta = diretorio()
        val segredo = "TOKEN_FICTICIO_EXCLUSIVO_DE_TESTE"
        val cofre = ArmazenamentoSeguro(pasta)
        assertNull(cofre.ler())
        cofre.gravar(segredo)
        assertFalse(File(pasta, "sessao.dpapi").readBytes().toString(Charsets.UTF_8).contains(segredo))
        assertEquals(segredo, ArmazenamentoSeguro(pasta).ler())
        cofre.limpar()
        assertNull(cofre.ler())
        assertTrue(pasta.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun windowsRecusaArquivoCorrompidoSemFallback() = runTest {
        if (!System.getProperty("os.name").startsWith("Windows")) return@runTest
        val pasta = diretorio()
        File(pasta, "sessao.dpapi").writeText("NAO_E_UM_TOKEN_CIFRADO")
        val erro = assertFailsWith<ArmazenamentoSeguroIndisponivel> { ArmazenamentoSeguro(pasta).ler() }
        assertTrue(generateSequence<Throwable>(erro) { it.cause }.all { it is ArmazenamentoSeguroIndisponivel })
        assertFalse(erro.toString().contains("NAO_E_UM_TOKEN_CIFRADO"))
    }

    @Test
    fun macOSPassaSegredoSomentePorStdin() {
        val segredo = "TOKEN_FICTICIO_EXCLUSIVO_DE_TESTE"
        val codificado = Base64.getEncoder().encodeToString(segredo.toByteArray())
        val chamadas = mutableListOf<Pair<List<String>, String>>()
        val cofre = CofreDesktop("Mac OS X", diretorio()) { args, entrada ->
            chamadas += args to entrada.toString(Charsets.UTF_8)
            if (args.contains("find-generic-password")) ResultadoProcesso(0, codificado.toByteArray()) else ResultadoProcesso(0, byteArrayOf())
        }
        cofre.gravar(segredo)
        assertTrue(chamadas.all { (args, _) -> args.none { it.contains(segredo) || it.contains(codificado) } })
        assertTrue(chamadas.any { (_, entrada) -> entrada.contains(codificado) })
    }

    @Test
    fun keyringIndisponivelFalhaSemCriarFallbackOuVazarSaida() {
        val pasta = diretorio()
        val cofre = CofreDesktop("Linux", pasta) { _, _ -> ResultadoProcesso(1, "SEGREDO_SIMULADO".toByteArray(), true) }
        val erro = assertFailsWith<ArmazenamentoSeguroIndisponivel> { cofre.gravar("TOKEN_FICTICIO") }
        assertNull(erro.cause)
        assertFalse(erro.toString().contains("SEGREDO_SIMULADO"))
        assertTrue(pasta.listFiles().orEmpty().isEmpty())
        assertFailsWith<ArmazenamentoSeguroIndisponivel> { cofre.ler() }
        assertFailsWith<ArmazenamentoSeguroIndisponivel> { cofre.limpar() }
    }

    private fun diretorio(): File {
        val base = File("build/test-secure").apply { mkdirs() }
        return Files.createTempDirectory(base.toPath(), "cofre-").toFile()
    }
}
