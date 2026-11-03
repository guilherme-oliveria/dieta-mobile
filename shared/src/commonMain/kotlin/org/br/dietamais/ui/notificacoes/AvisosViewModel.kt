package org.br.dietamais.ui.notificacoes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.br.dietamais.data.remoto.ErroApi
import org.br.dietamais.data.remoto.FalhaComunicacao
import org.br.dietamais.data.remoto.dto.AvisoResposta
import org.br.dietamais.data.repositorio.PortaAvisos
import kotlin.uuid.Uuid

enum class MensagemAvisos { SEM_CONEXAO, ERRO }

data class EstadoAvisos(val avisos: List<AvisoResposta> = emptyList(), val aberto: Boolean = false, val trabalhando: Boolean = false,
    val mensagem: MensagemAvisos? = null) {
    val naoLidos: Int get() = avisos.count { !it.lido }
}

/**
 * Central de avisos (MOB-27): o histórico vem do banco local (sync) e funciona offline. Marcar como lido exige rede; quando o
 * servidor aceita, pedimos um sync para a marca voltar pelo diário.
 */
class AvisosViewModel(private val avisos: PortaAvisos, private val sincronizar: suspend () -> Unit = {},
    private val aoNavegar: (String) -> Unit = {}, private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate) : ViewModel() {
    private val atual = MutableStateFlow(EstadoAvisos())
    val estado = atual.asStateFlow()

    init {
        viewModelScope.launch(dispatcher) {
            avisos.observar().catch { atual.update { e -> e.copy(mensagem = MensagemAvisos.ERRO) } }.collect { lista -> atual.update { it.copy(avisos = lista) } }
        }
    }

    fun abrir() = atual.update { it.copy(aberto = true, mensagem = null) }
    fun fechar() = atual.update { it.copy(aberto = false, mensagem = null) }
    fun limparMensagem() = atual.update { it.copy(mensagem = null) }

    /** Tocar num aviso leva à tela da `rota` e o marca como lido (se falhar, a navegação acontece mesmo assim). */
    fun tocar(id: Uuid) {
        val aviso = atual.value.avisos.firstOrNull { it.id == id } ?: return
        atual.update { it.copy(aberto = false, mensagem = null) }
        aviso.rota?.let(aoNavegar)
        if (!aviso.lido) marcar { avisos.marcarLido(id) }
    }

    fun marcarTodosLidos() {
        if (atual.value.trabalhando || atual.value.naoLidos == 0) return
        marcar { avisos.marcarTodosLidos() }
    }

    private fun marcar(acao: suspend () -> Unit) {
        atual.update { it.copy(trabalhando = true) }
        viewModelScope.launch(dispatcher) {
            try { acao(); sincronizar(); atual.update { it.copy(trabalhando = false) } }
            catch (c: CancellationException) { throw c }
            catch (falha: Exception) {
                val mensagem = when {
                    falha is FalhaComunicacao -> MensagemAvisos.SEM_CONEXAO
                    falha is ErroApi && falha.status == 404 -> null // o aviso já saiu do histórico
                    else -> MensagemAvisos.ERRO
                }
                atual.update { it.copy(trabalhando = false, mensagem = mensagem) }
            }
        }
    }
}
