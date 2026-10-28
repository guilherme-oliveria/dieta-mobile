package org.br.dietamais.ui.nutri.painel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.PortaAnamnese
import org.br.dietamais.data.repositorio.PortaAvisos
import org.br.dietamais.data.repositorio.PortaConsultas
import org.br.dietamais.data.repositorio.PortaPacientes
import org.br.dietamais.dominio.painel.ResumoPainel
import org.br.dietamais.dominio.painel.montarPainel
import kotlin.time.Clock
import kotlin.time.Instant

data class EstadoPainel(val resumo: ResumoPainel = ResumoPainel(emptyList(), emptyList(), emptyList(), null), val pacientes: List<PacienteResposta> = emptyList(),
    val atualizando: Boolean = false, val semConexao: Boolean = false) {
    fun nomeDo(c: ConsultaResposta): String? = pacientes.firstOrNull { it.id == c.pacienteId }?.nome
}

/** Painel da nutri (MOB-28): tudo vem do banco local e funciona offline; só o uso do plano e as consultas de hoje pedem rede ao atualizar. */
class PainelViewModel(private val consultas: PortaConsultas, private val pacientes: PortaPacientes, avisos: PortaAvisos, anamnese: PortaAnamnese,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate, private val agora: () -> Instant = { Clock.System.now() },
    private val fuso: () -> TimeZone = { TimeZone.currentSystemDefault() }) : ViewModel() {
    private val atual = MutableStateFlow(EstadoPainel())
    val estado = atual.asStateFlow()
    private val assinatura = MutableStateFlow<AssinaturaResumoResposta?>(null)

    init {
        viewModelScope.launch(dispatcher) {
            combine(consultas.observar().catch { emit(emptyList()) }, pacientes.observar().catch { emit(emptyList()) }, avisos.observar().catch { emit(emptyList()) },
                anamnese.observarQuestionarios(null).catch { emit(emptyList()) }, assinatura) { c, p, a, q, s ->
                EstadoPainelParcial(montarPainel(c, p, a, q, s, agora(), fuso()), p)
            }.collect { parcial -> atual.update { it.copy(resumo = parcial.resumo, pacientes = parcial.pacientes) } }
        }
    }

    private data class EstadoPainelParcial(val resumo: ResumoPainel, val pacientes: List<PacienteResposta>)

    fun atualizar() {
        if (atual.value.atualizando) return
        atual.update { it.copy(atualizando = true) }
        viewModelScope.launch(dispatcher) {
            try {
                val z = fuso(); val dia = agora().toLocalDateTime(z).date
                consultas.atualizar(dia.atStartOfDayIn(z), dia.plus(1, DateTimeUnit.DAY).atStartOfDayIn(z))
                assinatura.value = pacientes.assinatura()
                atual.update { it.copy(atualizando = false, semConexao = false) }
            } catch (c: CancellationException) { throw c }
            catch (_: Exception) { atual.update { it.copy(atualizando = false, semConexao = true) } }
        }
    }
}
