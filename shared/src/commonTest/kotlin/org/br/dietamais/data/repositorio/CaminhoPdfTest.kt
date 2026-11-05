package org.br.dietamais.data.repositorio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

class CaminhoPdfTest {
    private val id = Uuid.parse("0190a1b2-0000-7000-8000-000000000001")

    @Test fun dietaEAvaliacaoMantemOCaminho() {
        assertEquals("dietas/$id/pdf", caminhoPdf(RecursoPdf.DIETA, id))
        assertEquals("avaliacoes/$id/pdf", caminhoPdf(RecursoPdf.AVALIACAO, id))
    }

    @Test fun listaUsaOTamanhoPedido() {
        assertEquals("dietas/$id/pdf-lista?tamanho=PEQUENA", caminhoPdf(RecursoPdf.LISTA, id, TamanhoListaPdf.PEQUENA))
        assertEquals("dietas/$id/pdf-lista?tamanho=CARBOIDRATOS", caminhoPdf(RecursoPdf.LISTA, id, TamanhoListaPdf.CARBOIDRATOS))
    }

    @Test fun listaSemTamanhoUsaMedia() {
        assertEquals("dietas/$id/pdf-lista?tamanho=MEDIA", caminhoPdf(RecursoPdf.LISTA, id))
    }
}
