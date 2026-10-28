package org.br.dietamais.ui.nutri.assinatura

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.PortaAssinatura

enum class MensagemAssinatura { SEM_CONEXAO, JA_ATIVA, PAGAMENTO_INDISPONIVEL, ERRO }

data class EstadoAssinatura(val carregado: Boolean = false, val assinatura: AssinaturaResposta? = null, val enviando: Boolean = false,
    val semConexao: Boolean = false, val erro: MensagemAssinatura? = null, val urlCheckout: String? = null) {
    /** RN-ASS-04: sem assinatura ativa a nutri só lê. */
    val somenteLeitura get() = assinatura?.status.let { it == StatusAssinatura.CANCELADA || it == StatusAssinatura.EXPIRADA }
    val emAtraso get() = assinatura?.status == StatusAssinatura.EM_ATRASO
    override fun toString() = "EstadoAssinatura[protegido]"
}

/** Plano e uso da assinatura (MOB-32). O app nunca vende: abre o checkout hospedado no navegador (RN-ASS-06). */
class AssinaturaViewModel(private val repositorio: PortaAssinatura, private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    conectividade: Flow<Boolean>? = null) : ViewModel() {
    private val atual = MutableStateFlow(EstadoAssinatura())
    val estado = atual.asStateFlow()

    init {
        conectividade?.let { fluxo -> viewModelScope.launch(dispatcher) { fluxo.collect { online -> atual.update { it.copy(semConexao = !online) } } } }
        recarregar()
    }

    fun recarregar() = executar { val a = repositorio.obter(); atual.update { it.copy(carregado = true, assinatura = a) } }

    fun assinar(plano: String) {
        val a = atual.value.assinatura ?: return
        if (!a.pagamentoDisponivel || a.planosDisponiveis.none { it.codigo == plano }) return
        executar {
            val url = repositorio.pedirCheckout(plano).url
            if (!url.startsWith("https://")) throw RespostaInvalida()
            atual.update { it.copy(urlCheckout = url) }
        }
    }

    fun urlAberta() = atual.update { it.copy(urlCheckout = null) }

    private fun executar(bloco: suspend () -> Unit) {
        viewModelScope.launch(dispatcher) {
            if (atual.value.semConexao) { atual.update { it.copy(erro = MensagemAssinatura.SEM_CONEXAO) }; return@launch }
            if (atual.value.enviando) return@launch
            atual.update { it.copy(enviando = true, erro = null) }
            try { bloco(); atual.update { it.copy(enviando = false) } }
            catch (c: CancellationException) { throw c }
            catch (erro: ErroApi) {
                val msg = when (erro.codigo) { "ASSINATURA_JA_ATIVA" -> MensagemAssinatura.JA_ATIVA; "PAGAMENTO_INDISPONIVEL" -> MensagemAssinatura.PAGAMENTO_INDISPONIVEL; else -> MensagemAssinatura.ERRO }
                atual.update { it.copy(enviando = false, erro = msg) }
                if (msg == MensagemAssinatura.JA_ATIVA) try { val a = repositorio.obter(); atual.update { it.copy(assinatura = a) } } catch (_: Exception) { }
            } catch (_: FalhaComunicacao) { atual.update { it.copy(enviando = false, erro = MensagemAssinatura.SEM_CONEXAO) } }
            catch (_: SessaoAlterada) { atual.update { it.copy(enviando = false) } }
            catch (_: Exception) { atual.update { it.copy(enviando = false, erro = MensagemAssinatura.ERRO) } }
        }
    }
}
