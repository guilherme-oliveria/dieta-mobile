package org.br.dietamais.ui.nutri.listapaciente

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.*
import org.br.dietamais.dominio.dieta.AjusteLista
import kotlin.uuid.Uuid

class ListaPacienteViewModel(private val lista: PortaListaPaciente, private val alimentos: PortaAlimentos,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate, conectividade: Flow<Boolean>? = null) : ViewModel() {
    private val atual = MutableStateFlow(EstadoListaPaciente())
    val estado = atual.asStateFlow()
    private var catalogo = CatalogoAlimentos()
    private var baseEfetiva: List<AlimentoResposta> = emptyList()
    private var ajuste = AjusteLista()
    private var operacao: Job? = null
    init {
        viewModelScope.launch(dispatcher) { alimentos.observar().catch { }.collect { catalogo = it; publicar() } }
        conectividade?.let { fluxo -> viewModelScope.launch(dispatcher) { fluxo.collect { online -> atual.update { it.copy(semConexao = !online) } } } }
    }

    fun abrir(pacienteId: Uuid) {
        if (atual.value.pacienteId == pacienteId && !atual.value.carregando) return
        operacao?.cancel(); ajuste = AjusteLista(); baseEfetiva = emptyList()
        atual.value = EstadoListaPaciente(pacienteId = pacienteId, semConexao = atual.value.semConexao)
        recarregar()
    }
    fun recarregar() {
        val id = atual.value.pacienteId ?: return
        executar(carregar = true) {
            val resposta = lista.obter(id)
            baseEfetiva = resposta.alimentos; ajuste = AjusteLista(); atual.update { it.copy(versao = resposta.versao, semConexao = false) }
            publicar()
        }
    }
    fun mudarBusca(valor: String) { atual.update { it.copy(busca = valor.take(150)) }; publicar() }
    fun remover(id: Uuid) { if (!ocupado()) { ajuste = ajuste.remover(id); publicar() } }
    fun adicionar(id: Uuid) { if (!ocupado()) { ajuste = ajuste.adicionar(id); publicar() } }
    fun descartar() { if (!ocupado()) { ajuste = AjusteLista(); publicar() } }
    fun salvar() {
        val id = atual.value.pacienteId ?: return
        if (ocupado() || atual.value.semConexao || ajuste.vazio) return
        val pedido = ajuste
        executar {
            // o PUT recebe a diferença completa em relação à lista base da nutri, então soma ao que já existia no servidor
            val servidor = lista.obter(id)
            val adicionados = (servidor.adicionados.toSet() + pedido.adicionados) - pedido.removidos.toSet()
            val removidos = (servidor.removidos.toSet() + pedido.removidos.filter { it !in servidor.adicionados }) - pedido.adicionados.toSet()
            val resposta = lista.salvar(id, servidor.versao, AtualizarListaSubstituicaoRequisicao(adicionados.toList().sortedBy { it.toString() }, removidos.toList().sortedBy { it.toString() }))
            baseEfetiva = resposta.alimentos; ajuste = AjusteLista()
            atual.update { it.copy(versao = resposta.versao, aviso = MensagemLista.SALVO) }; publicar()
        }
    }
    private fun ocupado() = atual.value.carregando || atual.value.enviando
    private fun publicar() = atual.update { estado ->
        val efetiva = (baseEfetiva.filter { it.id !in ajuste.removidos } + catalogo.alimentos.filter { it.id in ajuste.adicionados && baseEfetiva.none { b -> b.id == it.id } })
            .sortedBy { it.nome.lowercase() }
        val ids = efetiva.map { it.id }.toSet()
        val opcoes = catalogo.alimentos.filter { it.ativo && it.id !in ids && it.nome.contains(estado.busca.trim(), ignoreCase = true) }
        estado.copy(grupos = catalogo.grupos, efetiva = efetiva, opcoes = opcoes, pendente = !ajuste.vazio)
    }
    private fun executar(carregar: Boolean = false, bloco: suspend () -> Unit) {
        atual.update { it.copy(carregando = carregar, enviando = !carregar, erro = null, aviso = null) }
        operacao = viewModelScope.launch(dispatcher) {
            try { bloco() }
            catch (c: CancellationException) { throw c }
            catch (falha: Exception) { apresentar(falha) }
            finally { atual.update { it.copy(carregando = false, enviando = false) } }
        }
    }
    private fun apresentar(falha: Throwable) {
        val mensagem = when (falha) {
            is FalhaComunicacao -> MensagemLista.SEM_CONEXAO
            is ErroApi -> when (falha.codigo) {
                "CONFLITO_VERSAO" -> MensagemLista.CONFLITO
                "ASSINATURA_INATIVA" -> MensagemLista.ASSINATURA_INATIVA
                "RECURSO_NAO_ENCONTRADO" -> MensagemLista.NAO_ENCONTRADO
                else -> MensagemLista.ERRO
            }
            else -> MensagemLista.ERRO
        }
        atual.update { it.copy(erro = mensagem, semConexao = it.semConexao || falha is FalhaComunicacao) }
    }
}
