package org.br.dietamais.plataforma

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Base64
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class ArmazenamentoSeguro(diretorio: File = File(System.getProperty("user.home"), ".dietamais/seguro")) : CofreSessao {
    private val cofre = CofreDesktop(System.getProperty("os.name"), diretorio, ::executarPrivado)
    actual override suspend fun ler(): String? = withContext(Dispatchers.IO) { cofre.ler() }
    actual override suspend fun gravar(conteudo: String) = withContext(Dispatchers.IO) { cofre.gravar(conteudo) }
    actual override suspend fun limpar() = withContext(Dispatchers.IO) { cofre.limpar() }
}

internal class ResultadoProcesso(val codigo: Int, val saida: ByteArray, val temErro: Boolean = false) {
    override fun toString() = "ResultadoProcesso[conteúdo protegido]"
}

internal class CofreDesktop(private val sistema: String, private val diretorio: File,
    private val executar: (List<String>, ByteArray) -> ResultadoProcesso) {
    private val arquivo get() = File(diretorio, "sessao.dpapi")
    private val base64 = Base64.getEncoder()
    private val decoder = Base64.getDecoder()

    @Synchronized
    fun ler(): String? = protegido {
        when {
            sistema.startsWith("Windows") -> if (!arquivo.exists()) null else dpapi("Unprotect", arquivo.readBytes()).toString(Charsets.UTF_8)
            sistema.startsWith("Mac") -> {
                val resultado = executar(listOf("/usr/bin/security", "find-generic-password", "-a", "sessao", "-s", SERVICO, "-w"), byteArrayOf())
                if (resultado.codigo == 44) null else decodificar(resultado)
            }
            sistema.startsWith("Linux") -> {
                val resultado = executar(listOf("/usr/bin/secret-tool", "lookup", "servico", SERVICO, "conta", "sessao"), byteArrayOf())
                if (resultado.codigo == 1 && !resultado.temErro && resultado.saida.isEmpty()) null else decodificar(resultado)
            }
            else -> throw ArmazenamentoSeguroIndisponivel()
        }
    }

    @Synchronized
    fun gravar(conteudo: String) = protegido {
        val entrada = base64.encode(conteudo.toByteArray(Charsets.UTF_8))
        when {
            sistema.startsWith("Windows") -> {
                val cifrado = dpapi("Protect", conteudo.toByteArray(Charsets.UTF_8))
                check(diretorio.isDirectory || diretorio.mkdirs())
                val temporario = Files.createTempFile(diretorio.toPath(), "sessao-", ".tmp")
                try {
                    Files.write(temporario, cifrado)
                    Files.move(temporario, arquivo.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                } finally { Files.deleteIfExists(temporario) }
            }
            sistema.startsWith("Mac") -> {
                // Modo interativo: o segredo trafega só pelo pipe, nunca em argv ou script salvo.
                val comando = "add-generic-password -U -a sessao -s $SERVICO -w ${entrada.toString(Charsets.US_ASCII)}\n"
                conferir(executar(listOf("/usr/bin/security", "-i", "-q"), comando.toByteArray(Charsets.US_ASCII)))
            }
            sistema.startsWith("Linux") -> conferir(executar(listOf("/usr/bin/secret-tool", "store", "--label=Dieta+ sessão", "servico", SERVICO, "conta", "sessao"), entrada))
            else -> throw ArmazenamentoSeguroIndisponivel()
        }
        check(ler() == conteudo)
    }

    @Synchronized
    fun limpar() = protegido {
        when {
            sistema.startsWith("Windows") -> { check(!arquivo.exists() || arquivo.delete()) }
            sistema.startsWith("Mac") -> {
                val resultado = executar(listOf("/usr/bin/security", "delete-generic-password", "-a", "sessao", "-s", SERVICO), byteArrayOf())
                check(resultado.codigo in listOf(0, 44))
            }
            sistema.startsWith("Linux") -> {
                val resultado = executar(listOf("/usr/bin/secret-tool", "clear", "servico", SERVICO, "conta", "sessao"), byteArrayOf())
                check(resultado.codigo == 0 || (resultado.codigo == 1 && !resultado.temErro))
            }
            else -> throw ArmazenamentoSeguroIndisponivel()
        }
    }

    private fun dpapi(operacao: String, dados: ByteArray): ByteArray {
        val script = "try { Add-Type -AssemblyName System.Security; " +
            "\$b=[Convert]::FromBase64String([Console]::In.ReadToEnd()); " +
            "\$r=[Security.Cryptography.ProtectedData]::$operacao(\$b, [Text.Encoding]::UTF8.GetBytes('$SERVICO'), [Security.Cryptography.DataProtectionScope]::CurrentUser); " +
            "[Console]::Out.Write([Convert]::ToBase64String(\$r)) } catch { exit 1 }"
        val powershell = File(System.getenv("SystemRoot") ?: throw ArmazenamentoSeguroIndisponivel(), "System32/WindowsPowerShell/v1.0/powershell.exe").absolutePath
        val resultado = executar(listOf(powershell, "-NoProfile", "-NonInteractive", "-WindowStyle", "Hidden", "-Command", script), base64.encode(dados))
        conferir(resultado)
        return decoder.decode(resultado.saida.toString(Charsets.US_ASCII).trim())
    }

    private fun decodificar(resultado: ResultadoProcesso): String {
        conferir(resultado)
        return decoder.decode(resultado.saida.toString(Charsets.US_ASCII).trim()).toString(Charsets.UTF_8)
    }
    private fun conferir(resultado: ResultadoProcesso) { check(resultado.codigo == 0) }
    private fun <T> protegido(bloco: () -> T): T = try { bloco() } catch (falha: Exception) { throw ArmazenamentoSeguroIndisponivel() }
    private companion object { const val SERVICO = "org.br.dietamais.sessao.v1" }
}

private fun executarPrivado(argumentos: List<String>, entrada: ByteArray): ResultadoProcesso {
    val processo = ProcessBuilder(argumentos).start()
    val saida = CompletableFuture.supplyAsync { processo.inputStream.use { it.readNBytes(65_537) } }
    val erro = CompletableFuture.supplyAsync { processo.errorStream.use { it.readNBytes(65_537).isNotEmpty() } }
    val escrita = CompletableFuture.runAsync { processo.outputStream.use { it.write(entrada) } }
    try {
        check(processo.waitFor(30, TimeUnit.SECONDS))
        escrita.get(1, TimeUnit.SECONDS)
        val bytes = saida.get(1, TimeUnit.SECONDS)
        check(bytes.size <= 65_536)
        return ResultadoProcesso(processo.exitValue(), bytes, erro.get(1, TimeUnit.SECONDS))
    } finally { processo.destroyForcibly() }
}
