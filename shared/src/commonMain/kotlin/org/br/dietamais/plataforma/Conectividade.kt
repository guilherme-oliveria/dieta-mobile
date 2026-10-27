package org.br.dietamais.plataforma

import kotlinx.coroutines.flow.StateFlow

enum class EstadoConexao { DESCONHECIDA, DISPONIVEL, INDISPONIVEL }
interface ObservadorConectividade : AutoCloseable {
    val estado: StateFlow<EstadoConexao>
}
expect class Conectividade : ObservadorConectividade {
    override val estado: StateFlow<EstadoConexao>
    override fun close()
}
