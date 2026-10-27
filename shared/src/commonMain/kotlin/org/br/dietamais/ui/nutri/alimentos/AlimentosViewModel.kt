package org.br.dietamais.ui.nutri.alimentos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.*
import org.br.dietamais.data.sync.novoIdLocal
import kotlin.uuid.Uuid

class AlimentosViewModel(private val repositorio: PortaAlimentos, private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    conectividade: Flow<Boolean>? = null, sessao: StateFlow<OperacaoSessao?>? = null) : ViewModel() {
    private val atual = MutableStateFlow(EstadoAlimentos())
    val estado = atual.asStateFlow()
    private var catalogo = CatalogoAlimentos()
    private var operacao: Job? = null
    private var geracao = 0L
    init {
        viewModelScope.launch(dispatcher) { repositorio.observar().catch { apresentar(it) }.collect { catalogo = it; publicar() } }
        conectividade?.let { fluxo -> viewModelScope.launch(dispatcher) { fluxo.collect { online ->
            atual.update { it.copy(semConexao = !online, erro = if (online && it.erro == MensagemAlimentos.SEM_CONEXAO) null else it.erro) }
        } } }
        if (sessao == null) atualizar()
        else viewModelScope.launch(dispatcher) {
            var anterior = sessao.value
            var primeira = true
            sessao.collect { operacao ->
                if (primeira || operacao != anterior) {
                    if (operacao == null || (!primeira && operacao.contexto != anterior?.contexto)) catalogo = CatalogoAlimentos()
                    cancelar(); atual.value = EstadoAlimentos(semConexao = atual.value.semConexao); publicar()
                    if (operacao != null) atualizar()
                }
                primeira = false; anterior = operacao
            }
        }
    }
    fun mudarBusca(valor: String) { atual.update { it.copy(busca = valor.take(150)) }; publicar() }
    fun mudarGrupo(id: Uuid?) { atual.update { it.copy(grupoId = id) }; publicar() }
    fun mudarAtivo(valor: Boolean) { atual.update { it.copy(ativo = valor) }; publicar() }
    private fun publicar() { atual.update { estado -> estado.copy(grupos = catalogo.grupos, lista = catalogo.alimentos.filter {
        it.ativo == estado.ativo && (estado.grupoId == null || it.grupoId == estado.grupoId) && it.nome.contains(estado.busca.trim(), ignoreCase = true)
    }) } }

    fun atualizar() {
        if (atual.value.atualizando || atual.value.enviando) return
        val esperada = geracao
        executar(atualizacao = true) { repositorio.atualizar(); if (geracao == esperada) atual.update { it.copy(semConexao = false) } }
    }
    fun novo() {
        if (!podeEscrever() || catalogo.grupos.isEmpty()) return
        cancelar(); atual.update { it.copy(formulario = FormularioAlimento(grupoId = it.grupoId ?: catalogo.grupos.first().id), conflito = false,
            erro = null, aviso = null, errosCampos = emptyMap(), desativar = null) }
    }
    fun editar(alimento: AlimentoResposta) {
        if (!podeEscrever()) return
        cancelar(); atual.update { it.copy(formulario = FormularioAlimento(alimento.id, alimento.versao, alimento.grupoId,
            alimento.nome, alimento.medidaCaseira, alimento.gramasPorPorcao.toString(), alimento.ativo), conflito = false,
            erro = null, aviso = null, errosCampos = emptyMap(), desativar = null) }
    }
    fun fechar() { cancelar(); atual.update { it.copy(formulario = null, desativar = null, conflito = false, erro = null, errosCampos = emptyMap()) } }
    fun mudarCampo(campo: CampoAlimento, valor: String) {
        if (atual.value.enviando || atual.value.conflito) return
        atual.update { estado -> estado.copy(formulario = estado.formulario?.let { form -> when (campo) {
            CampoAlimento.NOME -> form.copy(nome = valor.take(151))
            CampoAlimento.MEDIDA -> form.copy(medida = valor.take(151))
            CampoAlimento.GRAMAS -> form.copy(gramas = valor.take(50))
        }.copy(chave = novoIdLocal()) }, errosCampos = emptyMap(), erro = null) }
    }
    fun mudarGrupoFormulario(id: Uuid) {
        if (!atual.value.enviando && !atual.value.conflito) atual.update { it.copy(formulario = it.formulario?.copy(grupoId = id, chave = novoIdLocal()), errosCampos = emptyMap()) }
    }
    fun salvar() {
        if (!podeEscrever() || atual.value.conflito) return
        val form = atual.value.formulario ?: return
        val gramas = form.gramas.replace(',', '.').toDoubleOrNull()
        val erros = mutableMapOf<String, MensagemAlimentos>()
        if (form.nome.isBlank()) erros["nome"] = MensagemAlimentos.OBRIGATORIO
        else if (form.nome.length > 150) erros["nome"] = MensagemAlimentos.CAMPO_INVALIDO
        if (form.medida.isBlank()) erros["medidaCaseira"] = MensagemAlimentos.OBRIGATORIO
        else if (form.medida.length > 150) erros["medidaCaseira"] = MensagemAlimentos.CAMPO_INVALIDO
        if (form.grupoId == null || catalogo.grupos.none { it.id == form.grupoId }) erros["grupoId"] = MensagemAlimentos.OBRIGATORIO
        if (gramas == null || !gramas.isFinite() || gramas <= 0) erros["gramasPorPorcao"] = MensagemAlimentos.POSITIVO
        atual.update { it.copy(errosCampos = erros) }; if (erros.isNotEmpty()) return
        val esperada = geracao
        executar {
            if (form.versao == null) repositorio.criar(CriarAlimentoRequisicao(form.id, form.grupoId!!, form.nome.trim(), form.medida.trim(), gramas!!), form.chave)
            else repositorio.alterar(form.id, form.versao, AtualizarAlimentoRequisicao(form.grupoId!!, form.nome.trim(), form.medida.trim(), gramas!!, form.ativo))
            if (geracao == esperada) atual.update { it.copy(formulario = null, aviso = MensagemAlimentos.SALVO) }
        }
    }
    fun pedirDesativacao(alimento: AlimentoResposta) {
        if (podeEscrever() && alimento.ativo) atual.update { it.copy(desativar = alimento, erro = null) }
    }
    fun confirmarDesativacao() {
        if (!podeEscrever() || atual.value.conflito) return
        val alimento = atual.value.desativar ?: return
        val esperada = geracao
        executar {
            repositorio.alterar(alimento.id, alimento.versao, AtualizarAlimentoRequisicao(alimento.grupoId, alimento.nome,
                alimento.medidaCaseira, alimento.gramasPorPorcao, false))
            if (geracao == esperada) atual.update { it.copy(desativar = null, aviso = MensagemAlimentos.SALVO) }
        }
    }
    fun descartarEAtualizar() { fechar(); atualizar() }
    private fun podeEscrever() = !atual.value.semConexao && !atual.value.enviando && !atual.value.atualizando
    private fun executar(atualizacao: Boolean = false, bloco: suspend () -> Unit) {
        val esperada = geracao
        atual.update { it.copy(enviando = !atualizacao, atualizando = atualizacao, erro = null, aviso = null) }
        operacao = viewModelScope.launch(dispatcher) {
            try { bloco() }
            catch (cancelamento: CancellationException) { throw cancelamento }
            catch (falha: Exception) { if (geracao == esperada) apresentar(falha) }
            finally { if (geracao == esperada) atual.update { it.copy(enviando = false, atualizando = false) } }
        }
    }
    private fun cancelar() { geracao++; operacao?.cancel(); atual.update { it.copy(enviando = false, atualizando = false) } }
    private fun apresentar(falha: Throwable) {
        val mensagem = when (falha) {
            is FalhaComunicacao -> MensagemAlimentos.SEM_CONEXAO
            is ErroApi -> when (falha.codigo) {
                "NAO_IMPLEMENTADO" -> MensagemAlimentos.INDISPONIVEL
                "CONFLITO_VERSAO" -> MensagemAlimentos.CONFLITO
                "ASSINATURA_INATIVA" -> MensagemAlimentos.ASSINATURA_INATIVA
                "TAXA_EXCEDIDA" -> MensagemAlimentos.TAXA_EXCEDIDA
                else -> MensagemAlimentos.ERRO
            }
            else -> MensagemAlimentos.ERRO
        }
        atual.update { it.copy(erro = mensagem, conflito = it.conflito || mensagem == MensagemAlimentos.CONFLITO,
            semConexao = it.semConexao || falha is FalhaComunicacao,
            errosCampos = (falha as? ErroApi)?.campos?.associate { campo -> campo.campo to if (campo.codigo == "OBRIGATORIO") MensagemAlimentos.OBRIGATORIO else MensagemAlimentos.CAMPO_INVALIDO }.orEmpty()) }
    }
}
