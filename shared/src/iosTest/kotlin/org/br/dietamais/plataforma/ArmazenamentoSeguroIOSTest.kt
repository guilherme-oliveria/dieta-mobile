package org.br.dietamais.plataforma

import kotlinx.coroutines.test.runTest
import kotlin.test.*
import kotlin.uuid.Uuid
import platform.Security.errSecNotAvailable

class ArmazenamentoSeguroIOSTest {
    @Test
    fun executavelSemHostMapeiaKeychainIndisponivelSemFallback() = runTest {
        val nome = "org.br.dietamais.teste.${Uuid.random()}"
        val primeiro = ArmazenamentoSeguro(nome)
        var gravou = false
        try {
            val erro = assertFailsWith<ArmazenamentoSeguroIndisponivel> {
                primeiro.gravar("TOKEN_FICTICIO_TESTE_IOS")
                gravou = true
            }
            assertEquals(errSecNotAvailable, erro.codigoSistema, "OSStatus real retornado pelo executável sem host")
        } finally { if (gravou) primeiro.limpar() }
    }
}
