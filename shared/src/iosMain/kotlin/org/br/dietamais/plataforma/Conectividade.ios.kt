package org.br.dietamais.plataforma

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect

actual class Conectividade : ObservadorConectividade {
    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val atual = MutableStateFlow(conexaoIos.value)
    actual override val estado = atual.asStateFlow()
    init { escopo.launch { conexaoIos.collect { atual.value = it } } }
    actual override fun close() { escopo.cancel() }
}
