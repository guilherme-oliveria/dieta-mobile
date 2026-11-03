package org.br.dietamais.ui.pendencias

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.br.dietamais.data.repositorio.EstadoPendencia
import org.br.dietamais.data.repositorio.Pendencia
import org.br.dietamais.data.repositorio.PortaPendencias
import kotlin.uuid.Uuid

data class EstadoPendencias(val pendencias: List<Pendencia> = emptyList(), val aberto: Boolean = false, val selecionada: Uuid? = null) {
    val total: Int get() = pendencias.size
    /** O que precisa de decisão da nutri; o que só aguarda envio não pede nada. */
    val precisaDeAtencao: Int get() = pendencias.count { it.estado != EstadoPendencia.AGUARDANDO }
    val detalhe: Pendencia? get() = pendencias.firstOrNull { it.idMutacao == selecionada }
}

/** Pendências de sincronização (MOB-30): lista o que ficou em conflito ou foi rejeitado e deixa escolher o que fazer. */
class PendenciasViewModel(private val pendencias: PortaPendencias, private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate) : ViewModel() {
    private val atual = MutableStateFlow(EstadoPendencias())
    val estado = atual.asStateFlow()

    init {
        viewModelScope.launch(dispatcher) {
            pendencias.observar().catch { }.collect { lista ->
                atual.update { e ->
                    val aindaExiste = lista.any { it.idMutacao == e.selecionada }
                    e.copy(pendencias = lista, selecionada = e.selecionada.takeIf { aindaExiste }, aberto = e.aberto && lista.isNotEmpty())
                }
            }
        }
    }

    fun abrir() = atual.update { it.copy(aberto = true, selecionada = null) }
    fun fechar() = atual.update { it.copy(aberto = false, selecionada = null) }
    fun selecionar(id: Uuid) = atual.update { it.copy(selecionada = id) }
    fun voltar() = atual.update { it.copy(selecionada = null) }
    fun manterMinha(id: Uuid) = pendencias.manterMinha(id)
    fun usarServidor(id: Uuid) = pendencias.usarServidor(id)
    fun descartar(id: Uuid) = pendencias.descartar(id)
}
