package org.br.dietamais.ui.nutri.agenda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.*
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.PortaFinanceiro
import org.br.dietamais.dominio.agenda.*
import kotlin.time.Clock
import kotlin.time.Instant

enum class MensagemFinanceiro { SEM_CONEXAO, CONFLITO, ASSINATURA_INATIVA, VINCULO_ENCERRADO, NAO_ENCONTRADA, CANCELADA, TAXA_EXCEDIDA, DADOS_INVALIDOS, ERRO, SALVO }

data class EstadoFinanceiro(val consulta: ConsultaResposta? = null, val paciente: String? = null, val versao: Long = 0, val formulario: FormularioFinanceiro = FormularioFinanceiro(),
    val erros: Set<ErroFinanceiro> = emptySet(), val carregando: Boolean = false, val enviando: Boolean = false, val semConexao: Boolean = false,
    val resumo: ResumoFinanceiroResposta? = null, val erro: MensagemFinanceiro? = null, val aviso: MensagemFinanceiro? = null) {
    val aberto: Boolean get() = consulta != null
}

/**
 * Financeiro da nutri (MOB-79, G-20, RN-FIN-01): comparecimento, valor, forma e pago por consulta, mais o resumo do mês. Só online e fora do sync:
 * dinheiro nunca é gravado no banco local nem aparece para o paciente. O backend decide; a validação aqui é só feedback.
 */
class FinanceiroViewModel(private val financeiro: PortaFinanceiro, private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate, conectividade: Flow<Boolean>? = null,
    private val agora: () -> Instant = { Clock.System.now() }, private val fuso: () -> TimeZone = { TimeZone.currentSystemDefault() }) : ViewModel() {
    private val atual = MutableStateFlow(EstadoFinanceiro())
    val estado = atual.asStateFlow()
    private var carga: Job? = null

    init {
        conectividade?.let { fluxo -> viewModelScope.launch(dispatcher) { fluxo.collect { online -> atual.update { it.copy(semConexao = !online) } } } }
    }

    fun abrir(consulta: ConsultaResposta, paciente: String?) {
        carga?.cancel()
        atual.update { it.copy(consulta = consulta, paciente = paciente, versao = 0, formulario = FormularioFinanceiro(), erros = emptySet(), carregando = true, erro = null, aviso = null) }
        carga = viewModelScope.launch(dispatcher) {
            try { val r = financeiro.da(consulta.id); atual.update { it.copy(versao = r.versao, formulario = formularioDeFinanceiro(r), semConexao = false) } }
            catch (c: CancellationException) { throw c }
            catch (falha: Exception) { apresentar(falha) }
            finally { atual.update { it.copy(carregando = false) } }
        }
    }

    fun fechar() { if (!atual.value.enviando) { carga?.cancel(); atual.update { it.copy(consulta = null, erros = emptySet(), erro = null, carregando = false) } } }
    fun mudarComparecimento(valor: ComparecimentoConsulta) = formulario { it.copy(comparecimento = valor) }
    fun mudarValor(texto: String) = formulario { it.copy(valor = texto.take(12)) }
    fun mudarForma(forma: FormaPagamento?) = formulario { it.copy(forma = forma) }
    fun mudarPago(pago: Boolean) = formulario { it.copy(pago = pago) }
    private fun formulario(muda: (FormularioFinanceiro) -> FormularioFinanceiro) = atual.update { it.copy(formulario = muda(it.formulario), erros = emptySet(), erro = null) }

    fun salvar() {
        val e = atual.value; val consulta = e.consulta ?: return
        if (e.enviando || e.carregando) return
        val erros = erroDeFinanceiro(e.formulario)
        if (erros.isNotEmpty()) { atual.update { it.copy(erros = erros, erro = MensagemFinanceiro.DADOS_INVALIDOS) }; return }
        atual.update { it.copy(enviando = true, erro = null, aviso = null) }
        viewModelScope.launch(dispatcher) {
            try {
                financeiro.salvar(consulta.id, e.versao, requisicaoDeFinanceiro(e.formulario))
                atual.update { it.copy(consulta = null, aviso = MensagemFinanceiro.SALVO, semConexao = false) }
                carregarResumo()
            }
            catch (c: CancellationException) { throw c }
            catch (falha: Exception) { apresentar(falha) }
            finally { atual.update { it.copy(enviando = false) } }
        }
    }

    /** Resumo do mês corrente (fuso local): da meia-noite do dia 1 até a do mês seguinte. */
    fun carregarResumo() {
        val z = fuso(); val hoje = agora().toLocalDateTime(z).date
        val inicio = LocalDate(hoje.year, hoje.month, 1)
        viewModelScope.launch(dispatcher) {
            try { val r = financeiro.resumo(inicio.atStartOfDayIn(z), inicio.plus(1, DateTimeUnit.MONTH).atStartOfDayIn(z)); atual.update { it.copy(resumo = r) } }
            catch (c: CancellationException) { throw c }
            catch (falha: Exception) { if (falha is FalhaComunicacao) atual.update { it.copy(semConexao = true) } }
        }
    }

    private fun apresentar(falha: Throwable) {
        val mensagem = when (falha) {
            is FalhaComunicacao -> MensagemFinanceiro.SEM_CONEXAO
            is ErroApi -> when (falha.codigo) {
                "CONFLITO_VERSAO" -> MensagemFinanceiro.CONFLITO
                "ASSINATURA_INATIVA" -> MensagemFinanceiro.ASSINATURA_INATIVA
                "ACESSO_NEGADO" -> MensagemFinanceiro.VINCULO_ENCERRADO
                "RECURSO_NAO_ENCONTRADO" -> MensagemFinanceiro.NAO_ENCONTRADA
                "CONSULTA_CANCELADA" -> MensagemFinanceiro.CANCELADA
                "FORMATO_INVALIDO" -> MensagemFinanceiro.DADOS_INVALIDOS
                "TAXA_EXCEDIDA" -> MensagemFinanceiro.TAXA_EXCEDIDA
                else -> MensagemFinanceiro.ERRO
            }
            else -> MensagemFinanceiro.ERRO
        }
        // conflito: traz de novo a versão atual para a nutri revisar e salvar outra vez
        val consulta = atual.value.consulta
        atual.update { it.copy(erro = mensagem, semConexao = it.semConexao || falha is FalhaComunicacao) }
        if (mensagem == MensagemFinanceiro.CONFLITO && consulta != null) viewModelScope.launch(dispatcher) {
            try { val r = financeiro.da(consulta.id); atual.update { it.copy(versao = r.versao, formulario = formularioDeFinanceiro(r)) } } catch (c: CancellationException) { throw c } catch (_: Exception) { }
        }
    }
}
