package org.br.dietamais.ui.nutri.assinatura

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.PortaPerfilNutri

enum class MensagemPerfilNutri { NOME_OBRIGATORIO, CRN_INVALIDO, TELEFONE_LONGO, SEM_CONEXAO, CONFLITO, ASSINATURA_INATIVA, ERRO, SALVO }

data class EstadoPerfilNutri(val carregado: Boolean = false, val versao: Long = 0, val nome: String = "", val crn: String = "", val telefone: String = "",
    val orientacao: String = "", val alterado: Boolean = false, val enviando: Boolean = false, val semConexao: Boolean = false,
    val errosCampos: Map<String, MensagemPerfilNutri> = emptyMap(), val erro: MensagemPerfilNutri? = null, val aviso: MensagemPerfilNutri? = null) {
    override fun toString() = "EstadoPerfilNutri[protegido]"
}

/** "Meus dados" da nutri (MOB-36). A assinatura entra na MOB-32. */
class PerfilNutriViewModel(private val repositorio: PortaPerfilNutri, private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    conectividade: Flow<Boolean>? = null) : ViewModel() {
    private val atual = MutableStateFlow(EstadoPerfilNutri())
    val estado = atual.asStateFlow()

    init {
        conectividade?.let { fluxo -> viewModelScope.launch(dispatcher) { fluxo.collect { online -> atual.update { it.copy(semConexao = !online) } } } }
        recarregar()
    }

    fun recarregar() = executar { aplicar(repositorio.obter()) }
    private fun aplicar(p: PerfilNutriResposta) = atual.update {
        it.copy(carregado = true, versao = p.versao, nome = p.nome, crn = p.crn, telefone = p.telefone.orEmpty(), orientacao = p.orientacaoPadrao.orEmpty(),
            alterado = false, errosCampos = emptyMap())
    }

    fun mudarNome(v: String) = editar { it.copy(nome = v.take(150)) }
    fun mudarCrn(v: String) = editar { it.copy(crn = v.take(20)) }
    fun mudarTelefone(v: String) = editar { it.copy(telefone = v.take(30)) }
    fun mudarOrientacao(v: String) = editar { it.copy(orientacao = v.take(10_000)) }
    private fun editar(bloco: (EstadoPerfilNutri) -> EstadoPerfilNutri) = atual.update { bloco(it).copy(alterado = true, aviso = null, errosCampos = emptyMap()) }

    fun salvar() {
        val e = atual.value
        if (e.enviando || !e.alterado) return
        val campos = buildMap {
            if (e.nome.isBlank()) put("nome", MensagemPerfilNutri.NOME_OBRIGATORIO)
            if (!CRN.matches(e.crn.trim())) put("crn", MensagemPerfilNutri.CRN_INVALIDO)
            if (e.telefone.length > 30) put("telefone", MensagemPerfilNutri.TELEFONE_LONGO)
        }
        if (campos.isNotEmpty()) { atual.update { it.copy(errosCampos = campos) }; return }
        executar { aplicar(repositorio.salvar(e.versao, AtualizarPerfilNutriRequisicao(e.nome.trim(), e.crn.trim(), e.telefone.trim().ifBlank { null }, e.orientacao.trim().ifBlank { null })))
            atual.update { it.copy(aviso = MensagemPerfilNutri.SALVO) } }
    }

    private fun executar(bloco: suspend () -> Unit) {
        viewModelScope.launch(dispatcher) {
            if (atual.value.semConexao) { atual.update { it.copy(erro = MensagemPerfilNutri.SEM_CONEXAO) }; return@launch }
            atual.update { it.copy(enviando = true, erro = null) }
            try { bloco(); atual.update { it.copy(enviando = false) } }
            catch (c: CancellationException) { throw c }
            catch (erro: ErroApi) {
                val msg = when (erro.codigo) { "CONFLITO_VERSAO" -> MensagemPerfilNutri.CONFLITO; "ASSINATURA_INATIVA" -> MensagemPerfilNutri.ASSINATURA_INATIVA; else -> MensagemPerfilNutri.ERRO }
                atual.update { it.copy(enviando = false, erro = msg) }
                if (msg == MensagemPerfilNutri.CONFLITO) try { aplicar(repositorio.obter()) } catch (_: Exception) { }
            } catch (_: FalhaComunicacao) { atual.update { it.copy(enviando = false, erro = MensagemPerfilNutri.SEM_CONEXAO) } }
            catch (_: SessaoAlterada) { atual.update { it.copy(enviando = false) } }
            catch (_: Exception) { atual.update { it.copy(enviando = false, erro = MensagemPerfilNutri.ERRO) } }
        }
    }

    private companion object { val CRN = Regex("CRN-[1-9][0-9]* [0-9]+") }
}
