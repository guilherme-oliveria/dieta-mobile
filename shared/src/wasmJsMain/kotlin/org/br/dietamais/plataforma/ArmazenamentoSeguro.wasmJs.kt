package org.br.dietamais.plataforma

/** Web: o access fica só em memória e o refresh está no cookie HttpOnly, que o JavaScript não lê. */
actual class ArmazenamentoSeguro : CofreSessao {
    private var conteudo: String? = null
    actual override suspend fun ler(): String? = conteudo
    actual override suspend fun gravar(conteudo: String) { this.conteudo = conteudo }
    actual override suspend fun limpar() { conteudo = null }
}
