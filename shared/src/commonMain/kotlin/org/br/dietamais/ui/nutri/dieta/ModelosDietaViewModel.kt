package org.br.dietamais.ui.nutri.dieta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.PortaModelosDieta

/**
 * Biblioteca de modelos de dieta (MOB-56, RN-MOD-01): os do sistema só se leem e se duplicam; a nutri apaga as cópias dela e salva dietas como
 * modelo. Tudo online; quem decide permissão e limite é o backend.
 */
class ModelosDietaViewModel(private val modelos: PortaModelosDieta, private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    conectividade: Flow<Boolean>? = null) : ViewModel() {
    private val atual = MutableStateFlow(EstadoModelos())
    val estado = atual.asStateFlow()
    init {
        conectividade?.let { fluxo -> viewModelScope.launch(dispatcher) { fluxo.collect { online -> atual.update { it.copy(semConexao = !online) } } } }
    }

    fun carregar() { if (!atual.value.carregado) recarregar() }
    fun recarregar() = executar(carregar = true) { val lista = modelos.listar(); atual.update { it.copy(modelos = lista, carregado = true, semConexao = false) } }
    fun escolherAba(aba: AbaModelos) = atual.update { it.copy(aba = aba, apagar = null, erro = null, aviso = null) }

    /** RN-MOD-01: a cópia nasce na lista dela, então a tela passa para "Meus". */
    fun duplicar(modelo: ModeloDietaResposta) = executar {
        val copia = modelos.duplicar(modelo.id)
        atual.update { it.copy(modelos = it.modelos + copia, aba = AbaModelos.MEUS, aviso = MensagemModelos.DUPLICADO) }
    }
    fun pedirExclusao(modelo: ModeloDietaResposta) {
        if (modelo.doSistema) { atual.update { it.copy(erro = MensagemModelos.SOMENTE_LEITURA) }; return }
        atual.update { it.copy(apagar = modelo, erro = null, aviso = null) }
    }
    fun cancelarExclusao() = atual.update { it.copy(apagar = null) }
    fun confirmarExclusao() {
        val alvo = atual.value.apagar ?: return
        atual.update { it.copy(apagar = null) }
        executar {
            modelos.excluir(alvo.id, alvo.versao)
            atual.update { it.copy(modelos = it.modelos.filterNot { m -> m.id == alvo.id }, aviso = MensagemModelos.APAGADO) }
        }
    }
    /** Salva uma dieta dela como modelo (o servidor copia o conteúdo e guarda a energia total como referência de escala). */
    fun salvarComoModelo(dieta: DietaResposta) = executar {
        val novo = modelos.salvarComoModelo(dieta.id, SalvarComoModeloRequisicao(titulo = dieta.titulo.take(150)))
        atual.update { it.copy(modelos = it.modelos + novo, aviso = MensagemModelos.SALVO) }
    }

    private fun executar(carregar: Boolean = false, bloco: suspend () -> Unit) {
        atual.update { it.copy(carregando = carregar, enviando = !carregar, erro = null, aviso = null) }
        viewModelScope.launch(dispatcher) {
            try { bloco() }
            catch (c: CancellationException) { throw c }
            catch (falha: Exception) { apresentar(falha) }
            finally { atual.update { it.copy(carregando = false, enviando = false) } }
        }
    }
    private fun apresentar(falha: Throwable) {
        val mensagem = when (falha) {
            is FalhaComunicacao -> MensagemModelos.SEM_CONEXAO
            is ErroApi -> when (falha.codigo) {
                "CONFLITO_VERSAO" -> MensagemModelos.CONFLITO
                "ASSINATURA_INATIVA" -> MensagemModelos.ASSINATURA_INATIVA
                "RECURSO_NAO_ENCONTRADO" -> MensagemModelos.NAO_ENCONTRADA
                "ACESSO_NEGADO" -> MensagemModelos.SOMENTE_LEITURA
                "FORMATO_INVALIDO", "ITEM_INVALIDO", "PORCAO_INVALIDA" -> MensagemModelos.LIMITE
                "TAXA_EXCEDIDA" -> MensagemModelos.TAXA_EXCEDIDA
                else -> MensagemModelos.ERRO
            }
            else -> MensagemModelos.ERRO
        }
        atual.update { it.copy(erro = mensagem, semConexao = it.semConexao || falha is FalhaComunicacao) }
    }
}
