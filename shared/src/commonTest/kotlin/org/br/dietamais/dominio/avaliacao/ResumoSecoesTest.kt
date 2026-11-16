package org.br.dietamais.dominio.avaliacao

import kotlin.test.*

class ResumoSecoesTest {
    @Test fun contaSoCamposComNumero() {
        val basicas = MedidaAvaliacao.entries.filter { it.secao == SecaoAvaliacao.BASICAS }
        assertTrue(basicas.size >= 2)
        val valores = mapOf(basicas[0] to "80,5", basicas[1] to "abc")
        val p = progressoDasSecoes(valores, listOf(SecaoAvaliacao.BASICAS)).single()
        assertEquals(1, p.preenchidas); assertEquals(basicas.size, p.total)
        assertFalse(p.completa); assertFalse(p.vazia)
    }
    @Test fun secaoSemValorEVazia() {
        val p = progressoDasSecoes(emptyMap(), listOf(SecaoAvaliacao.PERIMETROS)).single()
        assertTrue(p.vazia); assertFalse(p.completa)
    }
    @Test fun secaoCompleta() {
        val todas = MedidaAvaliacao.entries.filter { it.secao == SecaoAvaliacao.DOBRAS }
        val p = progressoDasSecoes(todas.associateWith { "10" }, listOf(SecaoAvaliacao.DOBRAS)).single()
        assertTrue(p.completa)
    }
    @Test fun limitaPelasMedidasInformadas() {
        val duas = MedidaAvaliacao.entries.filter { it.secao == SecaoAvaliacao.DOBRAS }.take(2)
        val p = progressoDasSecoes(duas.associateWith { "5" }, listOf(SecaoAvaliacao.DOBRAS)) { duas }.single()
        assertEquals(2, p.total); assertTrue(p.completa)
    }
}
