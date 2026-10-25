package org.br.dietamais.plataforma

interface CofreSessao {
    @Throws(ArmazenamentoSeguroIndisponivel::class)
    suspend fun ler(): String?
    @Throws(ArmazenamentoSeguroIndisponivel::class)
    suspend fun gravar(conteudo: String)
    @Throws(ArmazenamentoSeguroIndisponivel::class)
    suspend fun limpar()
}

expect class ArmazenamentoSeguro : CofreSessao {
    override suspend fun ler(): String?
    override suspend fun gravar(conteudo: String)
    override suspend fun limpar()
}

class ArmazenamentoSeguroIndisponivel(val codigoSistema: Int? = null) :
    Exception("O armazenamento seguro não está disponível." + (codigoSistema?.let { " OSStatus=$it." } ?: ""))
