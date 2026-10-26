package org.br.dietamais.plataforma

enum class ResultadoCompartilhamento { ABERTO, COPIADO }
fun interface Compartilhamento { suspend fun compartilhar(texto: String): ResultadoCompartilhamento }
class CompartilhamentoIndisponivel : Exception("Não foi possível abrir o compartilhamento.")
expect class Compartilhador : Compartilhamento {
    override suspend fun compartilhar(texto: String): ResultadoCompartilhamento
}
