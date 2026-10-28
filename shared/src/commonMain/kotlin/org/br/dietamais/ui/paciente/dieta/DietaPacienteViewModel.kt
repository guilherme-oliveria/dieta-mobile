package org.br.dietamais.ui.paciente.dieta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.repositorio.DadosDietaPaciente
import org.br.dietamais.data.repositorio.PortaDietaPaciente
import org.br.dietamais.dominio.rotina.dietaVigente
import org.br.dietamais.dominio.rotina.proximaRefeicao
import org.br.dietamais.dominio.rotina.refeicoesOrdenadas
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.Uuid

/** Hoje e Dieta do paciente (MOB-14): lê o snapshot local; só a troca de dieta em uso exige rede. */
class DietaPacienteViewModel(private val repositorio: PortaDietaPaciente,
    private val agora: () -> LocalTime = { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time },
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate, conectividade: Flow<Boolean>? = null,
    private val tique: Flow<Unit> = flow { while (true) { emit(Unit); delay(1.minutes) } }) : ViewModel() {
    private val atual = MutableStateFlow(EstadoDietaPaciente())
    val estado = atual.asStateFlow()
    private var dados = DadosDietaPaciente()
    private var selecionando: Job? = null

    init {
        viewModelScope.launch(dispatcher) {
            combine(repositorio.observar().catch { atual.update { it.copy(erro = MensagemDietaPaciente.ERRO, carregado = true) } }, tique) { d, _ -> d }
                .collect { dados = it; recalcular() }
        }
        conectividade?.let { fluxo -> viewModelScope.launch(dispatcher) { fluxo.collect { online -> atual.update { it.copy(semConexao = !online) } } } }
    }

    private fun recalcular() = atual.update { e ->
        val dieta = dietaVigente(dados.snapshots, dados.emUso?.dietaId)
        val porNutri = dados.snapshots.sortedByDescending { it.publicadoEm }.distinctBy { it.nutricionista.id }
        e.copy(carregado = true, dieta = dieta, refeicoes = dieta?.let { refeicoesOrdenadas(it.refeicoes) }.orEmpty(),
            proxima = dieta?.let { proximaRefeicao(it.refeicoes, agora()) }, alternativas = if (porNutri.size > 1) porNutri else emptyList(),
            versaoSelecao = dados.emUso?.versao ?: 0)
    }

    fun abrirRefeicao(id: Uuid?) = atual.update { it.copy(refeicaoAberta = id) }

    fun escolher(dietaId: Uuid) {
        val e = atual.value
        if (e.enviando) return
        if (e.semConexao) { atual.update { it.copy(erro = MensagemDietaPaciente.SEM_CONEXAO) }; return }
        selecionando = viewModelScope.launch(dispatcher) {
            atual.update { it.copy(enviando = true, erro = null) }
            try {
                val resposta = repositorio.selecionar(dietaId, e.versaoSelecao)
                dados = dados.copy(emUso = resposta); recalcular()
                atual.update { it.copy(enviando = false) }
            } catch (c: CancellationException) { throw c }
            catch (erro: ErroApi) {
                atual.update { it.copy(enviando = false, erro = if (erro.codigo == "CONFLITO_VERSAO") MensagemDietaPaciente.CONFLITO else MensagemDietaPaciente.ERRO) }
            } catch (_: FalhaComunicacao) { atual.update { it.copy(enviando = false, erro = MensagemDietaPaciente.SEM_CONEXAO) } }
            catch (_: SessaoAlterada) { atual.update { it.copy(enviando = false) } }
            catch (_: Exception) { atual.update { it.copy(enviando = false, erro = MensagemDietaPaciente.ERRO) } }
        }
    }
}
