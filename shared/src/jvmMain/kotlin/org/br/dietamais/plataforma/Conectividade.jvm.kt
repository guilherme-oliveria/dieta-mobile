package org.br.dietamais.plataforma

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.NetworkInterface
import kotlin.time.Duration.Companion.seconds

actual class Conectividade : ObservadorConectividade {
    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val atual = MutableStateFlow(EstadoConexao.DESCONHECIDA)
    actual override val estado = atual.asStateFlow()
    init {
        escopo.launch {
            while (isActive) {
                atual.value = try {
                    if (NetworkInterface.getNetworkInterfaces()?.toList().orEmpty().any { it.isUp && !it.isLoopback }) EstadoConexao.DISPONIVEL
                    else EstadoConexao.INDISPONIVEL
                } catch (_: Exception) { EstadoConexao.DESCONHECIDA }
                delay(5.seconds)
            }
        }
    }
    actual override fun close() { escopo.cancel() }
}
