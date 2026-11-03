package org.br.dietamais.plataforma

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

actual class Conectividade : ObservadorConectividade {
    private val atual = MutableStateFlow(estadoAtual())
    actual override val estado = atual.asStateFlow()
    private val aoConectar: (JsAny) -> Unit = { atual.value = EstadoConexao.DISPONIVEL }
    private val aoDesconectar: (JsAny) -> Unit = { atual.value = EstadoConexao.INDISPONIVEL }
    init {
        ouvirEvento("online", aoConectar)
        ouvirEvento("offline", aoDesconectar)
    }
    actual override fun close() {
        removerEvento("online", aoConectar)
        removerEvento("offline", aoDesconectar)
    }
    private fun estadoAtual() = if (navegadorOnline()) EstadoConexao.DISPONIVEL else EstadoConexao.INDISPONIVEL
}

private fun navegadorOnline(): Boolean = js("navigator.onLine")
private fun ouvirEvento(nome: String, aoOcorrer: (JsAny) -> Unit): Unit = js("window.addEventListener(nome, aoOcorrer)")
private fun removerEvento(nome: String, aoOcorrer: (JsAny) -> Unit): Unit = js("window.removeEventListener(nome, aoOcorrer)")
