package org.br.dietamais.plataforma

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

actual class Conectividade(contexto: Context) : ObservadorConectividade {
    private val gerente = contexto.applicationContext.getSystemService(ConnectivityManager::class.java)
    private val atual = MutableStateFlow(EstadoConexao.DESCONHECIDA)
    actual override val estado = atual.asStateFlow()
    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = atualizar()
        override fun onLost(network: Network) = atualizar()
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) = atualizar()
    }
    private var registrado = false
    init {
        try { gerente?.registerDefaultNetworkCallback(callback); registrado = gerente != null; atualizar() }
        catch (_: Exception) { atual.value = EstadoConexao.DESCONHECIDA }
    }
    private fun atualizar() {
        atual.value = try {
            val rede = gerente?.activeNetwork
            val capacidades = rede?.let { gerente?.getNetworkCapabilities(it) }
            when {
                gerente == null -> EstadoConexao.DESCONHECIDA
                rede == null -> EstadoConexao.INDISPONIVEL
                capacidades == null -> EstadoConexao.DESCONHECIDA
                capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) -> EstadoConexao.DISPONIVEL
                else -> EstadoConexao.INDISPONIVEL
            }
        } catch (_: Exception) { EstadoConexao.DESCONHECIDA }
    }
    actual override fun close() {
        if (registrado) runCatching { gerente?.unregisterNetworkCallback(callback) }
        registrado = false
    }
}
