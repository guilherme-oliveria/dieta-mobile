package org.br.dietamais.data.remoto

import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertSame

class FalhaDeRedeTest {
    @Test fun excecaoEErroPuroDoKtorJsViramFalhaDeComunicacao() {
        assertIs<FalhaComunicacao>(comoFalhaDeRede(java.io.IOException("sem rede")))
        assertIs<FalhaComunicacao>(comoFalhaDeRede(IllegalStateException("x")))
        assertIs<FalhaComunicacao>(comoFalhaDeRede(Error("Fail to fetch")))
    }

    @Test fun subclassesDeErroSeguemSubindo() {
        val oom = OutOfMemoryError("sem memória")
        assertSame(oom, comoFalhaDeRede(oom))
        val teste = AssertionError("falha de teste")
        assertSame(teste, comoFalhaDeRede(teste))
    }
}
