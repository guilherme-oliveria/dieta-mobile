@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.experimental.ExperimentalNativeApi::class,
    com.russhwolf.settings.ExperimentalSettingsImplementation::class, com.russhwolf.settings.ExperimentalSettingsApi::class)

package org.br.dietamais.plataforma

import com.russhwolf.settings.KeychainSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrService
import kotlin.native.ref.createCleaner

actual class ArmazenamentoSeguro(nomeServico: String = "org.br.dietamais.sessao.v1") : CofreSessao {
    private val servico = CFBridgingRetain(nomeServico)
    private val cleaner = createCleaner(servico) { CFBridgingRelease(it) }
    private val keychain = KeychainSettings(kSecAttrService to servico,
        kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly)

    actual override suspend fun ler(): String? = operar { keychain.getStringOrNull("sessao") }
    actual override suspend fun gravar(conteudo: String) = operar { keychain.putString("sessao", conteudo) }
    actual override suspend fun limpar() = operar { keychain.remove("sessao") }

    private suspend fun <T> operar(bloco: () -> T): T = withContext(Dispatchers.Default) {
        try { bloco() } catch (falha: Exception) {
            val status = Regex("^Keychain error (-?[0-9]+):").find(falha.message.orEmpty())?.groupValues?.get(1)?.toIntOrNull()
            throw ArmazenamentoSeguroIndisponivel(status)
        }
    }
}
