package org.br.dietamais.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.br.dietamais.data.sync.novoIdLocal
import kotlin.uuid.Uuid

data class PedidoNavegacao(val id: Uuid, val rota: String)

/** Pedidos de navegação por `rota` de aviso (toque na notificação ou num item da central). A navegação os consome quando há perfil. */
object SolicitacoesNavegacao {
    private val atual = MutableStateFlow<PedidoNavegacao?>(null)
    val pendente: StateFlow<PedidoNavegacao?> = atual.asStateFlow()
    fun pedir(rota: String) { if (rota.startsWith("/") && rota.length <= 200) atual.value = PedidoNavegacao(novoIdLocal(), rota) }
    fun consumir(id: Uuid) { atual.update { if (it?.id == id) null else it } }
}
