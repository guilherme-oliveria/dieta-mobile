package org.br.dietamais.ui.paciente.evolucao

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.br.dietamais.data.repositorio.PortaEvolucao
import org.br.dietamais.data.repositorio.PortaRotina
import org.br.dietamais.dominio.avaliacao.*
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

enum class MensagemEvolucao { PESO_INVALIDO, PESO_SALVO, ERRO }

data class EstadoEvolucao(val series: Map<SerieEvolucao, List<PontoSerie>> = emptyMap(), val serie: SerieEvolucao? = null,
    val intervalo: IntervaloEvolucao = IntervaloEvolucao.SEIS_MESES, val carregado: Boolean = false, val podeRegistrarPeso: Boolean = false,
    val registrandoPeso: Boolean = false, val pesoTexto: String = "", val aviso: MensagemEvolucao? = null, val erro: MensagemEvolucao? = null) {
    val pontos: List<PontoSerie> get() = serie?.let { series[it] }.orEmpty()
    val disponiveis: List<SerieEvolucao> get() = series.keys.toList()
}

/** Gráficos de evolução. Paciente: lê tudo do banco local e registra peso em casa pela fila; nutri: só leitura de um paciente. */
class EvolucaoViewModel(private val evolucao: PortaEvolucao, private val rotina: PortaRotina?, private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    private val agora: () -> Instant = { Clock.System.now() }) : ViewModel() {
    private val atual = MutableStateFlow(EstadoEvolucao())
    val estado = atual.asStateFlow()
    private var leitura: Job? = null
    private var alvo: Uuid? = null; private var aberto = false

    /** Sem id, perfil paciente (vê só as avaliações compartilhadas). Com id, a nutri abre a evolução daquele paciente. */
    fun abrir(pacienteId: Uuid?) {
        if (aberto && alvo == pacienteId) return
        aberto = true; alvo = pacienteId; leitura?.cancel()
        atual.value = EstadoEvolucao(podeRegistrarPeso = pacienteId == null && rotina != null)
        leitura = viewModelScope.launch(dispatcher) {
            evolucao.observar(pacienteId).catch { atual.update { e -> e.copy(erro = MensagemEvolucao.ERRO, carregado = true) } }.collect { dados ->
                val avaliacoes = if (pacienteId == null) dados.avaliacoes.filter { it.compartilhada } else dados.avaliacoes
                val series = construirSeries(avaliacoes, dados.pesosEmCasa)
                atual.update { e -> e.copy(series = series, carregado = true, erro = null, serie = e.serie?.takeIf { it in series } ?: escolhaInicial(series)) }
            }
        }
    }

    private fun escolhaInicial(series: Map<SerieEvolucao, List<PontoSerie>>) =
        listOf(SerieEvolucao.PESO, SerieEvolucao.PESO_EM_CASA).firstOrNull { it in series } ?: series.keys.firstOrNull()

    fun escolherSerie(serie: SerieEvolucao) = atual.update { if (serie in it.series) it.copy(serie = serie) else it }
    fun escolherIntervalo(intervalo: IntervaloEvolucao) = atual.update { it.copy(intervalo = intervalo) }
    fun abrirRegistroPeso() = atual.update { if (it.podeRegistrarPeso) it.copy(registrandoPeso = true, pesoTexto = "", erro = null, aviso = null) else it }
    fun fecharRegistroPeso() = atual.update { it.copy(registrandoPeso = false) }
    fun mudarPeso(texto: String) = atual.update { it.copy(pesoTexto = texto.take(8), erro = null) }

    /** Offline-first: grava no banco e na fila na mesma transação (RN-SYN-01); o servidor confere o intervalo de 20 a 400 kg. */
    fun registrarPeso() {
        val e = atual.value; val porta = rotina ?: return
        if (!e.registrandoPeso) return
        val peso = lerDecimal(e.pesoTexto)
        if (peso == null || peso < 20.0 || peso > 400.0 || e.pesoTexto.trim().replace(',', '.').substringAfter('.', "").trimEnd('0').length > 2) {
            atual.update { it.copy(erro = MensagemEvolucao.PESO_INVALIDO) }; return
        }
        try { porta.registrarPeso(peso, agora()); atual.update { it.copy(registrandoPeso = false, aviso = MensagemEvolucao.PESO_SALVO, erro = null) } }
        catch (c: CancellationException) { throw c }
        catch (_: Exception) { atual.update { it.copy(erro = MensagemEvolucao.ERRO) } }
    }
}
