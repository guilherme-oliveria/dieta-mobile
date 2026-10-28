package org.br.dietamais.ui.paciente.compras

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.br.dietamais.data.repositorio.DadosDietaPaciente
import org.br.dietamais.data.repositorio.PortaCompras
import org.br.dietamais.data.repositorio.PortaDietaPaciente
import org.br.dietamais.dominio.rotina.GrupoCompras
import org.br.dietamais.dominio.rotina.dietaVigente
import org.br.dietamais.dominio.rotina.gerarListaCompras
import org.br.dietamais.dominio.rotina.textoListaCompras
import org.br.dietamais.plataforma.Compartilhamento
import org.br.dietamais.plataforma.ResultadoCompartilhamento
import kotlin.uuid.Uuid

enum class MensagemCompras { COPIADA, ABERTA, FALHOU }

data class EstadoCompras(val carregado: Boolean = false, val temDieta: Boolean = false, val dias: Int = 7, val grupos: List<GrupoCompras> = emptyList(),
    val marcados: Set<Uuid> = emptySet(), val aviso: MensagemCompras? = null) { override fun toString() = "EstadoCompras[protegido]" }

/** Lista de compras gerada no aparelho a partir da dieta em uso (MOB-20). Funciona offline. */
class ComprasViewModel(private val dietas: PortaDietaPaciente, private val compras: PortaCompras, private val compartilhamento: Compartilhamento,
    private val titulo: suspend () -> String = { "Lista de compras" }, private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate) : ViewModel() {
    private val atual = MutableStateFlow(EstadoCompras())
    val estado = atual.asStateFlow()
    private var dados = DadosDietaPaciente()

    init {
        viewModelScope.launch(dispatcher) { dietas.observar().catch { }.collect { dados = it; recalcular() } }
        viewModelScope.launch(dispatcher) { compras.observarMarcados().catch { }.collect { m -> atual.update { it.copy(marcados = m) } } }
    }

    private fun recalcular() = atual.update { e ->
        val dieta = dietaVigente(dados.snapshots, dados.emUso?.dietaId)
        e.copy(carregado = true, temDieta = dieta != null, grupos = dieta?.let { gerarListaCompras(it, e.dias) }.orEmpty())
    }

    fun mudarDias(dias: Int) { if (dias in 1..60) { atual.update { it.copy(dias = dias) }; recalcular() } }
    fun marcar(id: Uuid, marcado: Boolean) { try { compras.marcar(id, marcado) } catch (_: Exception) { } }
    fun limparMarcas() { try { compras.limpar() } catch (_: Exception) { } }

    fun compartilhar() {
        val e = atual.value
        if (e.grupos.isEmpty()) return
        viewModelScope.launch(dispatcher) {
            val texto = textoListaCompras(e.grupos, e.marcados, titulo())
            val aviso = try { if (compartilhamento.compartilhar(texto) == ResultadoCompartilhamento.COPIADO) MensagemCompras.COPIADA else MensagemCompras.ABERTA }
            catch (c: CancellationException) { throw c } catch (_: Exception) { MensagemCompras.FALHOU }
            atual.update { it.copy(aviso = aviso) }
        }
    }
}
