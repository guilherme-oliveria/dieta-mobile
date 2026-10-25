package org.br.dietamais.plataforma

import kotlinx.coroutines.test.runTest
import kotlin.test.*
import kotlin.uuid.Uuid

class ArmazenamentoSeguroIOSTest {
    @Test
    fun keychainPersisteEntreInstanciasELimpaSoSeuServico() = runTest {
        val nome = "org.br.dietamais.teste.${Uuid.random()}"
        val primeiro = ArmazenamentoSeguro(nome)
        val outro = ArmazenamentoSeguro("$nome.outro")
        try {
            assertNull(primeiro.ler())
            primeiro.gravar("TOKEN_FICTICIO_TESTE_IOS")
            assertEquals("TOKEN_FICTICIO_TESTE_IOS", ArmazenamentoSeguro(nome).ler())
            assertNull(outro.ler())
            primeiro.limpar()
            assertNull(primeiro.ler())
        } finally { primeiro.limpar(); outro.limpar() }
    }
}
