package org.br.dietamais.ui.nutri.dieta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.PortaModelosDieta
import org.br.dietamais.data.repositorio.PortaPorcoes
import org.br.dietamais.data.sync.novoIdLocal
import org.br.dietamais.dominio.dieta.EscalaPorcoes
import kotlin.uuid.Uuid

/** Por que a escala existe ou não: [CALCULADA] usa VET ÷ energia do modelo; nos demais casos as porções ficam como no modelo. */
enum class SituacaoEscala { CARREGANDO, CALCULADA, SEM_AVALIACAO, SEM_BASE, FORA_DA_FAIXA }

data class LinhaEscala(val grupo: String, val modelo: Double, val paciente: Double)

/** [criada] = rascunho que o servidor devolveu; a rota fecha a tela e abre o planejador nele. */
data class EstadoAplicarModelo(
    val modelo: ModeloDietaResposta? = null, val pacienteId: Uuid? = null, val vetKcal: Double? = null, val fator: Double? = null,
    val situacao: SituacaoEscala = SituacaoEscala.CARREGANDO, val linhas: List<LinhaEscala> = emptyList(),
    val enviando: Boolean = false, val semConexao: Boolean = false, val erro: MensagemModelos? = null,
    val avisos: List<AvisoModelo> = emptyList(), val criada: DietaResposta? = null,
) { override fun toString() = "EstadoAplicarModelo[conteúdo clínico protegido]" }

/**
 * Aplicar modelo ao paciente (MOB-57, RN-MOD-02): mostra a escala prevista (mesma conta do servidor) e cria o rascunho. Nada é publicado.
 * A chave de idempotência vale por abertura da tela, então um toque repetido não cria dois rascunhos.
 */
class AplicarModeloViewModel(private val modelos: PortaModelosDieta, private val porcoes: PortaPorcoes,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate) : ViewModel() {
    private val atual = MutableStateFlow(EstadoAplicarModelo())
    val estado = atual.asStateFlow()
    private var chave = novoIdLocal()
    private var carga: Job? = null

    fun abrir(pacienteId: Uuid, modelo: ModeloDietaResposta) {
        carga?.cancel(); chave = novoIdLocal()
        atual.value = EstadoAplicarModelo(modelo = modelo, pacienteId = pacienteId)
        carga = viewModelScope.launch(dispatcher) {
            try {
                val referencia = modelos.referenciaAvaliacao(pacienteId)
                val grupos = porcoes.obter().grupos.associate { it.grupoId to it.nome }
                val vet = referencia?.metas?.vetKcal
                val fator = EscalaPorcoes.fator(vet, modelo.kcal)
                val situacao = when {
                    vet == null -> SituacaoEscala.SEM_AVALIACAO
                    modelo.kcal == null -> SituacaoEscala.SEM_BASE
                    fator == null -> SituacaoEscala.FORA_DA_FAIXA
                    else -> SituacaoEscala.CALCULADA
                }
                atual.update { it.copy(vetKcal = vet, fator = fator, situacao = situacao, linhas = linhas(modelo, grupos, fator), semConexao = false) }
            } catch (c: CancellationException) { throw c }
            catch (falha: Exception) { apresentar(falha) }
        }
    }

    /** Cria o rascunho. Só manda o VET quando há escala viável; fora da faixa o servidor responderia 400. */
    fun criar() {
        val e = atual.value
        val modelo = e.modelo ?: return; val pacienteId = e.pacienteId ?: return
        if (e.enviando || e.criada != null || e.situacao == SituacaoEscala.CARREGANDO) return
        atual.update { it.copy(enviando = true, erro = null) }
        viewModelScope.launch(dispatcher) {
            try {
                val r = modelos.aplicar(pacienteId, modelo.id, AplicarModeloRequisicao(vetKcal = e.vetKcal.takeIf { e.fator != null }), chave)
                atual.update { it.copy(criada = r.dieta, avisos = r.avisos) }
            } catch (c: CancellationException) { throw c }
            catch (falha: Exception) { apresentar(falha) }
            finally { atual.update { it.copy(enviando = false) } }
        }
    }

    fun fechar() { carga?.cancel(); atual.value = EstadoAplicarModelo() }

    /** Soma das porções por grupo nas refeições principais (alternativas não contam, RN-DIE-11). */
    private fun linhas(modelo: ModeloDietaResposta, grupos: Map<Uuid, String>, fator: Double?): List<LinhaEscala> {
        val soma = LinkedHashMap<Uuid, Double>()
        modelo.refeicoes.filter { it.tipo != "ALTERNATIVA" }.flatMap { it.itens }.forEach { i ->
            val g = i.grupoId; val p = i.porcoes
            if (g != null && p != null) soma[g] = (soma[g] ?: 0.0) + p
        }
        return soma.map { (g, total) ->
            val escalado = if (fator == null) total else modelo.refeicoes.filter { it.tipo != "ALTERNATIVA" }.flatMap { it.itens }
                .filter { it.grupoId == g && it.porcoes != null }.sumOf { EscalaPorcoes.escalar(it.porcoes!!, fator) }
            LinhaEscala(grupos[g] ?: "—", total, escalado)
        }
    }

    private fun apresentar(falha: Throwable) {
        val mensagem = when (falha) {
            is FalhaComunicacao -> MensagemModelos.SEM_CONEXAO
            is ErroApi -> when (falha.codigo) {
                "ASSINATURA_INATIVA" -> MensagemModelos.ASSINATURA_INATIVA
                "RECURSO_NAO_ENCONTRADO" -> MensagemModelos.NAO_ENCONTRADA
                "FORMATO_INVALIDO", "ITEM_INVALIDO", "PORCAO_INVALIDA" -> MensagemModelos.LIMITE
                "TAXA_EXCEDIDA" -> MensagemModelos.TAXA_EXCEDIDA
                else -> MensagemModelos.ERRO
            }
            else -> MensagemModelos.ERRO
        }
        atual.update { it.copy(erro = mensagem, semConexao = it.semConexao || falha is FalhaComunicacao) }
    }
}
