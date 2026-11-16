package org.br.dietamais.ui.nutri.protocolos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.SalvarProtocolosNutriRequisicao
import org.br.dietamais.data.repositorio.PortaProtocolosNutri
import org.br.dietamais.dominio.calculos.ProtocoloDobras
import org.br.dietamais.dominio.calculos.ProtocoloMassa

enum class MensagemProtocolos { SEM_CONEXAO, CONFLITO, ASSINATURA_INATIVA, ERRO, SALVO }

/** `dobras` e `massa` começam no padrão do servidor (7 dobras e peso magro) até a leitura chegar. */
data class EstadoProtocolosNutri(val carregado: Boolean = false, val versao: Long = 0, val dobras: ProtocoloDobras = ProtocoloDobras.SETE,
    val massa: ProtocoloMassa = ProtocoloMassa.MAGRO, val alterado: Boolean = false, val enviando: Boolean = false, val semConexao: Boolean = false,
    val erro: MensagemProtocolos? = null, val aviso: MensagemProtocolos? = null)

/** "Meus protocolos" (MOB-53): o que a nutri escolhe aqui vale para as próximas avaliações; cada avaliação guarda o que usou (RN-CAL-03). */
class ProtocolosNutriViewModel(private val repositorio: PortaProtocolosNutri, private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    conectividade: Flow<Boolean>? = null) : ViewModel() {
    private val atual = MutableStateFlow(EstadoProtocolosNutri())
    val estado = atual.asStateFlow()

    init {
        conectividade?.let { fluxo -> viewModelScope.launch(dispatcher) { fluxo.collect { online -> atual.update { it.copy(semConexao = !online) } } } }
        recarregar()
    }

    fun recarregar() = executar { aplicar(repositorio.obter()) }
    private fun aplicar(p: org.br.dietamais.data.remoto.dto.ProtocolosNutriResposta) = atual.update {
        it.copy(carregado = true, versao = p.versao, dobras = ProtocoloDobras.entries.firstOrNull { d -> d.name == p.dobras } ?: ProtocoloDobras.SETE,
            massa = ProtocoloMassa.entries.firstOrNull { m -> m.name == p.massa } ?: ProtocoloMassa.MAGRO, alterado = false)
    }

    fun mudarDobras(valor: ProtocoloDobras) = atual.update { it.copy(dobras = valor, alterado = true, aviso = null) }
    fun mudarMassa(valor: ProtocoloMassa) = atual.update { it.copy(massa = valor, alterado = true, aviso = null) }

    fun salvar() {
        val e = atual.value
        if (e.enviando || !e.alterado) return
        executar { aplicar(repositorio.salvar(e.versao, SalvarProtocolosNutriRequisicao(e.dobras.name, e.massa.name))); atual.update { it.copy(aviso = MensagemProtocolos.SALVO) } }
    }

    private fun executar(bloco: suspend () -> Unit) {
        viewModelScope.launch(dispatcher) {
            if (atual.value.semConexao) { atual.update { it.copy(erro = MensagemProtocolos.SEM_CONEXAO) }; return@launch }
            atual.update { it.copy(enviando = true, erro = null) }
            try { bloco(); atual.update { it.copy(enviando = false) } }
            catch (c: CancellationException) { throw c }
            catch (erro: ErroApi) {
                val msg = when (erro.codigo) { "CONFLITO_VERSAO" -> MensagemProtocolos.CONFLITO; "ASSINATURA_INATIVA" -> MensagemProtocolos.ASSINATURA_INATIVA; else -> MensagemProtocolos.ERRO }
                atual.update { it.copy(enviando = false, erro = msg) }
                if (msg == MensagemProtocolos.CONFLITO) try { aplicar(repositorio.obter()) } catch (_: Exception) { }
            } catch (_: FalhaComunicacao) { atual.update { it.copy(enviando = false, erro = MensagemProtocolos.SEM_CONEXAO) } }
            catch (_: SessaoAlterada) { atual.update { it.copy(enviando = false) } }
            catch (_: Exception) { atual.update { it.copy(enviando = false, erro = MensagemProtocolos.ERRO) } }
        }
    }
}

/** Padrão da nutri para o formulário de avaliação nova; sem conexão ou com qualquer falha devolve nulo e vale 7 dobras e peso magro (RN-CAL-03). */
fun protocolosPadraoDe(porta: PortaProtocolosNutri): suspend () -> Pair<ProtocoloDobras, ProtocoloMassa>? = {
    try {
        val p = porta.obter()
        (ProtocoloDobras.entries.firstOrNull { it.name == p.dobras } ?: ProtocoloDobras.SETE) to (ProtocoloMassa.entries.firstOrNull { it.name == p.massa } ?: ProtocoloMassa.MAGRO)
    } catch (c: CancellationException) { throw c } catch (_: Exception) { null }
}
