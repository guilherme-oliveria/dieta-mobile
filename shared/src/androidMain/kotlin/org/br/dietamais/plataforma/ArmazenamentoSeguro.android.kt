package org.br.dietamais.plataforma

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.File
import java.io.FileNotFoundException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class ArmazenamentoSeguro(contexto: Context) : CofreSessao {
    private val arquivo = AtomicFile(File(contexto.applicationContext.noBackupFilesDir, "sessao.cifrada"))
    private val trava = Any()

    actual override suspend fun ler(): String? = operar {
        val dados = try { arquivo.readFully() } catch (ausente: FileNotFoundException) { return@operar null }
        check(dados.size > 12)
        val chave = keyStore().getKey(ALIAS, null) as? SecretKey ?: throw ArmazenamentoSeguroIndisponivel()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, chave, GCMParameterSpec(128, dados.copyOfRange(0, 12)))
        cipher.doFinal(dados, 12, dados.size - 12).toString(Charsets.UTF_8)
    }

    actual override suspend fun gravar(conteudo: String) = operar {
        val store = keyStore()
        val chave = store.getKey(ALIAS, null) as? SecretKey ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build())
            generateKey()
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, chave)
        val cifrado = cipher.iv + cipher.doFinal(conteudo.toByteArray(Charsets.UTF_8))
        val stream = arquivo.startWrite()
        try { stream.write(cifrado); arquivo.finishWrite(stream) }
        catch (falha: Exception) { arquivo.failWrite(stream); throw falha }
    }

    actual override suspend fun limpar() = operar {
        arquivo.delete()
        check(!arquivo.baseFile.exists())
        keyStore().deleteEntry(ALIAS)
    }

    private fun keyStore() = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private suspend fun <T> operar(bloco: () -> T): T = withContext(Dispatchers.IO) {
        try { synchronized(trava) { bloco() } }
        catch (falha: Exception) { throw ArmazenamentoSeguroIndisponivel() }
    }

    private companion object { const val ALIAS = "org.br.dietamais.sessao.v1" }
}
