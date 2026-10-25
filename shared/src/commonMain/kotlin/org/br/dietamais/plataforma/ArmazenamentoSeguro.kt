package org.br.dietamais.plataforma

interface CofreSessao {
    suspend fun ler(): String?
    suspend fun gravar(conteudo: String)
    suspend fun limpar()
}

expect class ArmazenamentoSeguro : CofreSessao {
    override suspend fun ler(): String?
    override suspend fun gravar(conteudo: String)
    override suspend fun limpar()
}

class ArmazenamentoSeguroIndisponivel : Exception("O armazenamento seguro não está disponível.")
