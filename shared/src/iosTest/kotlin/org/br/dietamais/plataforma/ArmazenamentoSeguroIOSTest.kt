package org.br.dietamais.plataforma

import kotlinx.coroutines.test.runTest
import kotlin.test.*
import kotlin.uuid.Uuid

class ArmazenamentoSeguroIOSTest {
    @Test
    fun executavelSemHostMapeiaOSStatusDeEntitlementSemFallback() = runTest {
        val nome = "org.br.dietamais.teste.${Uuid.random()}"
        val primeiro = ArmazenamentoSeguro(nome)
        var gravou = false
        try {
            val erro = assertFailsWith<ArmazenamentoSeguroIndisponivel> {
                primeiro.gravar("TOKEN_FICTICIO_TESTE_IOS")
                gravou = true
                println("Diagnóstico Keychain sem host: gravação concluída sem erro de entitlement")
            }
            println("Diagnóstico Keychain sem host: OSStatus=${erro.codigoSistema}")
            assertEquals(-34018, erro.codigoSistema, "OSStatus real retornado pelo executável sem host")
        } finally { if (gravou) primeiro.limpar() }
    }
}
