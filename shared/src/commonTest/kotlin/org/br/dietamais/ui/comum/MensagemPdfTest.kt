package org.br.dietamais.ui.comum

import org.br.dietamais.data.remoto.*
import kotlin.test.*

class MensagemPdfTest {
    private fun erro(codigo: String, status: Int) = ErroApi(codigo, null, emptyList(), status, null, null)

    @Test fun mapeiaFalhasPeloCodigo() {
        assertEquals(MensagemPdf.INDISPONIVEL, mensagemPdf(erro("RECURSO_NAO_ENCONTRADO", 404)))
        assertEquals(MensagemPdf.ERRO, mensagemPdf(erro("ERRO_INTERNO", 500)))
        assertEquals(MensagemPdf.SEM_CONEXAO, mensagemPdf(FalhaComunicacao()))
        assertEquals(MensagemPdf.ERRO, mensagemPdf(RespostaInvalida()))
        assertNull(mensagemPdf(SessaoAlterada()))
    }
}
