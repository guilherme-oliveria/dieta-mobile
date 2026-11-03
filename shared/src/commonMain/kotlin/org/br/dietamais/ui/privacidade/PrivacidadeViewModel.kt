package org.br.dietamais.ui.privacidade

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.ExportacaoResposta
import org.br.dietamais.data.repositorio.PortaPrivacidade

enum class MensagemPrivacidade { SEM_CONEXAO, EXPORTACAO_EM_ANDAMENTO, SENHA_INCORRETA, SENHA_OBRIGATORIA, VINCULO_ATIVO, PEDIDO_ENVIADO, ERRO }

data class EstadoPrivacidade(val exportacao: ExportacaoResposta? = null, val enviando: Boolean = false, val semConexao: Boolean = false,
    val confirmandoExclusao: Boolean = false, val senha: String = "", val erro: MensagemPrivacidade? = null, val aviso: MensagemPrivacidade? = null,
    val urlParaAbrir: String? = null, val contaExcluida: Boolean = false) {
    override fun toString() = "EstadoPrivacidade[protegido]"
}

/** Privacidade (MOB-34): termos, exportar meus dados e excluir conta (RN-LGPD-02/03). Exportar e excluir exigem internet. */
class PrivacidadeViewModel(private val repositorio: PortaPrivacidade, private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    conectividade: Flow<Boolean>? = null, private val exportacaoDisponivel: Boolean = true) : ViewModel() {
    private val atual = MutableStateFlow(EstadoPrivacidade())
    val estado = atual.asStateFlow()

    init {
        conectividade?.let { fluxo -> viewModelScope.launch(dispatcher) { fluxo.collect { online -> atual.update { it.copy(semConexao = !online) } } } }
        if (exportacaoDisponivel) atualizarExportacao()
    }

    fun atualizarExportacao() = executar { val e = repositorio.exportacao(); atual.update { it.copy(exportacao = e) } }

    fun pedirExportacao() = executar {
        val e = repositorio.pedirExportacao()
        atual.update { it.copy(exportacao = e, aviso = MensagemPrivacidade.PEDIDO_ENVIADO) }
    }

    fun abrirExportacao() { atual.value.exportacao?.takeIf { it.status == "PRONTA" }?.url?.let { u -> atual.update { it.copy(urlParaAbrir = u) } } }
    fun abrirTermos() = executar { repositorio.urlTermos()?.let { u -> atual.update { it.copy(urlParaAbrir = u) } } }
    fun urlAberta() = atual.update { it.copy(urlParaAbrir = null) }

    fun pedirExclusao() = atual.update { it.copy(confirmandoExclusao = true, senha = "", erro = null) }
    fun cancelarExclusao() = atual.update { it.copy(confirmandoExclusao = false, senha = "") }
    fun mudarSenha(v: String) = atual.update { it.copy(senha = v.take(200), erro = null) }

    fun confirmarExclusao() {
        val senha = atual.value.senha
        if (senha.isEmpty()) { atual.update { it.copy(erro = MensagemPrivacidade.SENHA_OBRIGATORIA) }; return }
        executar {
            repositorio.excluirConta(senha)
            atual.update { it.copy(contaExcluida = true, confirmandoExclusao = false, senha = "") }
        }
    }

    private fun executar(bloco: suspend () -> Unit) {
        viewModelScope.launch(dispatcher) {
            if (atual.value.semConexao) { atual.update { it.copy(erro = MensagemPrivacidade.SEM_CONEXAO) }; return@launch }
            if (atual.value.enviando) return@launch
            atual.update { it.copy(enviando = true, erro = null, aviso = null) }
            try { bloco(); atual.update { it.copy(enviando = false) } }
            catch (c: CancellationException) { throw c }
            catch (erro: ErroApi) {
                val msg = when (erro.codigo) {
                    "EXPORTACAO_EM_ANDAMENTO" -> MensagemPrivacidade.EXPORTACAO_EM_ANDAMENTO
                    "CREDENCIAIS_INVALIDAS" -> MensagemPrivacidade.SENHA_INCORRETA
                    "VINCULO_ATIVO" -> MensagemPrivacidade.VINCULO_ATIVO
                    else -> MensagemPrivacidade.ERRO
                }
                atual.update { it.copy(enviando = false, erro = msg) }
            } catch (_: FalhaComunicacao) { atual.update { it.copy(enviando = false, erro = MensagemPrivacidade.SEM_CONEXAO) } }
            catch (_: SessaoAlterada) { atual.update { it.copy(enviando = false) } }
            catch (_: Exception) { atual.update { it.copy(enviando = false, erro = MensagemPrivacidade.ERRO) } }
        }
    }
}
