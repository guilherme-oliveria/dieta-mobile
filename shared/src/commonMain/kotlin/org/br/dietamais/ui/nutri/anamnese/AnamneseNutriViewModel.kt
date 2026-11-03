package org.br.dietamais.ui.nutri.anamnese

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.EnviarQuestionarioRequisicao
import org.br.dietamais.data.repositorio.PortaAnamnese
import org.br.dietamais.data.sync.novoIdLocal
import org.br.dietamais.dominio.anamnese.*
import kotlin.uuid.Uuid

/** Aba Anamnese da ficha (MOB-24): enviar um modelo ao paciente, ler respostas e editar os modelos da nutri. */
class AnamneseNutriViewModel(private val anamnese: PortaAnamnese, private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    conectividade: Flow<Boolean>? = null) : ViewModel() {
    private val atual = MutableStateFlow(EstadoAnamneseNutri())
    val estado = atual.asStateFlow()
    private var leituraModelos: Job? = null
    private var leituraQuestionarios: Job? = null
    private var operacao: Job? = null

    init {
        conectividade?.let { fluxo -> viewModelScope.launch(dispatcher) { fluxo.collect { online -> atual.update { it.copy(semConexao = !online) } } } }
        leituraModelos = viewModelScope.launch(dispatcher) {
            anamnese.observarModelos().catch { atual.update { e -> e.copy(erro = MensagemAnamnese.ERRO) } }.collect { lista -> atual.update { it.copy(modelos = lista) } }
        }
    }

    fun abrir(pacienteId: Uuid) {
        if (atual.value.pacienteId == pacienteId) return
        leituraQuestionarios?.cancel(); operacao?.cancel()
        atual.update { EstadoAnamneseNutri(pacienteId = pacienteId, modelos = it.modelos, semConexao = it.semConexao) }
        leituraQuestionarios = viewModelScope.launch(dispatcher) {
            anamnese.observarQuestionarios(pacienteId).catch { atual.update { e -> e.copy(erro = MensagemAnamnese.ERRO) } }.collect { lista -> atual.update { it.copy(questionarios = lista) } }
        }
        recarregar()
    }

    fun recarregar() {
        val paciente = atual.value.pacienteId ?: return
        if (atual.value.atualizando) return
        atual.update { it.copy(atualizando = true, erro = null) }
        viewModelScope.launch(dispatcher) {
            try {
                anamnese.atualizarModelos(); anamnese.atualizarQuestionarios(paciente)
                atual.update { it.copy(semConexao = false) }
            }
            catch (c: CancellationException) { throw c }
            catch (falha: Exception) { apresentar(falha) }
            finally { atual.update { it.copy(atualizando = false) } }
        }
    }

    fun enviar(modeloId: Uuid) {
        val paciente = atual.value.pacienteId ?: return
        if (atual.value.enviando || atual.value.semConexao) return
        atual.update { it.copy(enviando = true, erro = null, aviso = null) }
        operacao = viewModelScope.launch(dispatcher) {
            try { anamnese.enviar(paciente, EnviarQuestionarioRequisicao(novoIdLocal(), modeloId)); atual.update { it.copy(aviso = MensagemAnamnese.ENVIADO, semConexao = false) } }
            catch (c: CancellationException) { throw c }
            catch (falha: Exception) { apresentar(falha) }
            finally { atual.update { it.copy(enviando = false) } }
        }
    }

    fun ler(id: Uuid) = atual.update { it.copy(lendoId = id, erro = null, aviso = null) }
    fun fecharLeitura() = atual.update { it.copy(lendoId = null) }

    fun gerenciarModelos(abrir: Boolean) = atual.update { it.copy(gerenciandoModelos = abrir, editor = null, erro = null, aviso = null) }
    fun novoModelo() = atual.update { it.copy(editor = editorNovo(novoIdLocal()), validacao = ValidacaoModelo(), erro = null, aviso = null) }
    fun editarModelo(id: Uuid) {
        val modelo = atual.value.modelos.firstOrNull { it.id == id } ?: return
        atual.update { it.copy(editor = editorDe(modelo), validacao = ValidacaoModelo(), erro = null, aviso = null) }
    }
    fun fecharEditor() { if (!atual.value.enviando) atual.update { it.copy(editor = null, validacao = ValidacaoModelo()) } }

    fun mudarNome(nome: String) = editor { it.mudarNome(nome.take(120)) }
    fun adicionarBloco() = editor { it.adicionarBloco() }
    fun removerBloco(indice: Int) = editor { it.removerBloco(indice) }
    fun mudarBloco(indice: Int, titulo: String) = editor { it.mudarBloco(indice, titulo.take(120)) }
    fun adicionarPergunta(bloco: Int) = editor { it.adicionarPergunta(bloco, novoIdLocal()) }
    fun removerPergunta(id: Uuid) = editor { it.removerPergunta(id) }
    fun moverPergunta(id: Uuid, delta: Int) = editor { it.moverPergunta(id, delta) }
    fun mudarPergunta(id: Uuid, mudar: (PerguntaEditavel) -> PerguntaEditavel) = editor { it.mudarPergunta(id, mudar) }
    fun ligarCondicao(id: Uuid, referencia: Uuid) = editor { it.ligarCondicao(id, referencia) }
    fun desligarCondicao(id: Uuid) = editor { it.desligarCondicao(id) }

    fun salvarModelo() {
        val e = atual.value; val editor = e.editor ?: return
        if (e.enviando || e.semConexao) return
        val validacao = editor.validar()
        if (!validacao.valido) { atual.update { it.copy(validacao = validacao) }; return }
        atual.update { it.copy(enviando = true, erro = null, aviso = null, validacao = ValidacaoModelo()) }
        operacao = viewModelScope.launch(dispatcher) {
            try {
                val requisicao = editor.paraRequisicao()
                if (editor.id == null) anamnese.criarModelo(requisicao) else anamnese.salvarModelo(editor.id, checkNotNull(editor.versao), requisicao)
                atual.update { it.copy(editor = null, aviso = MensagemAnamnese.MODELO_SALVO, semConexao = false) }
            }
            catch (c: CancellationException) { throw c }
            catch (falha: Exception) { apresentar(falha) }
            finally { atual.update { it.copy(enviando = false) } }
        }
    }

    private fun editor(mudar: (EditorModelo) -> EditorModelo) = atual.update { e -> e.editor?.let { e.copy(editor = mudar(it), validacao = ValidacaoModelo()) } ?: e }

    private fun apresentar(falha: Throwable) {
        val mensagem = when (falha) {
            is FalhaComunicacao -> MensagemAnamnese.SEM_CONEXAO
            is ErroApi -> when (falha.codigo) {
                "CONFLITO_VERSAO" -> MensagemAnamnese.CONFLITO
                "FORMATO_INVALIDO" -> MensagemAnamnese.MODELO_INVALIDO
                "ASSINATURA_INATIVA" -> MensagemAnamnese.ASSINATURA_INATIVA
                "ACESSO_NEGADO" -> MensagemAnamnese.VINCULO_ENCERRADO
                "RECURSO_NAO_ENCONTRADO" -> MensagemAnamnese.NAO_ENCONTRADO
                "TAXA_EXCEDIDA" -> MensagemAnamnese.TAXA_EXCEDIDA
                else -> MensagemAnamnese.ERRO
            }
            else -> MensagemAnamnese.ERRO
        }
        atual.update { it.copy(erro = mensagem, semConexao = it.semConexao || falha is FalhaComunicacao) }
        if (mensagem == MensagemAnamnese.CONFLITO) { atual.update { it.copy(editor = null) }; recarregar() }
    }
}
