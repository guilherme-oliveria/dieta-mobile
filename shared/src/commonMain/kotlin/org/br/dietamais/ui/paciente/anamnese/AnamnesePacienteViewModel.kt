package org.br.dietamais.ui.paciente.anamnese

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.JsonElement
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.PortaAnamnese
import org.br.dietamais.data.repositorio.RespostasPendentes
import org.br.dietamais.dominio.anamnese.*
import org.br.dietamais.ui.nutri.anamnese.MensagemAnamnese
import kotlin.uuid.Uuid

data class EstadoAnamnesePaciente(val questionarios: List<QuestionarioResposta> = emptyList(), val abertoId: Uuid? = null, val passo: Int = 0,
    val entradas: Map<Uuid, EntradaResposta> = emptyMap(), val erros: Map<Uuid, ErroResposta> = emptyMap(), val concluindo: Boolean = false,
    val semConexao: Boolean = false, val mensagem: MensagemAnamnese? = null) {
    val pendentes: List<QuestionarioResposta> get() = questionarios.filter { it.status == StatusQuestionario.PENDENTE }
    val aberto: QuestionarioResposta? get() = questionarios.firstOrNull { it.id == abertoId }
    val perguntas: List<PerguntaQuestionario> get() = aberto?.blocos?.flatMap { it.perguntas }.orEmpty()
    val respostas: Map<Uuid, JsonElement> get() = aberto?.let { respostasEfetivas(it.respostas, entradas, perguntas) }.orEmpty()
    val passos: List<PassoAnamnese> get() = aberto?.let { passos(it.blocos, respostas) }.orEmpty()
    val passoAtual: Int get() = passo.coerceIn(0, (passos.size - 1).coerceAtLeast(0))
}

/** Assistente de resposta (MOB-25): salva parcial offline a cada passo; só concluir exige rede (RN-ANA-03). */
class AnamnesePacienteViewModel(private val anamnese: PortaAnamnese, private val sincronizar: suspend () -> Unit = {},
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate, conectividade: Flow<Boolean>? = null) : ViewModel() {
    private val atual = MutableStateFlow(EstadoAnamnesePaciente())
    val estado = atual.asStateFlow()

    init {
        conectividade?.let { fluxo -> viewModelScope.launch(dispatcher) { fluxo.collect { online -> atual.update { it.copy(semConexao = !online) } } } }
        viewModelScope.launch(dispatcher) {
            anamnese.observarQuestionarios(null).catch { atual.update { e -> e.copy(mensagem = MensagemAnamnese.ERRO) } }
                .collect { lista -> atual.update { it.copy(questionarios = lista) } }
        }
    }

    fun abrir(id: Uuid) {
        val q = atual.value.questionarios.firstOrNull { it.id == id } ?: return
        if (q.status != StatusQuestionario.PENDENTE) return
        atual.update { it.copy(abertoId = id, passo = 0, entradas = emptyMap(), erros = emptyMap(), mensagem = null) }
    }

    /** Sair do assistente guarda o que já está válido; o que ficou inválido se perde. */
    fun fechar() {
        if (atual.value.concluindo) return
        gravarEntradas()
        atual.update { it.copy(abertoId = null, entradas = emptyMap(), erros = emptyMap(), passo = 0) }
    }

    fun limparMensagem() = atual.update { it.copy(mensagem = null) }

    fun mudar(pergunta: Uuid, entrada: EntradaResposta) {
        if (atual.value.concluindo) return
        atual.update { it.copy(entradas = it.entradas + (pergunta to entrada), erros = it.erros - pergunta, mensagem = null) }
        // escolhas não passam por digitação: gravam na hora, para não perder se o app fechar
        if (entrada is EntradaResposta.Valor) gravarEntradas()
    }

    fun proximo() {
        val e = atual.value; val passoAtual = e.passoAtual
        val erros = validarPasso(passoAtual)
        if (erros.isNotEmpty()) { atual.update { it.copy(erros = erros, mensagem = mensagemDe(erros)) }; return }
        gravarEntradas()
        atual.update { it.copy(passo = (passoAtual + 1).coerceAtMost((it.passos.size - 1).coerceAtLeast(0)), erros = emptyMap(), mensagem = null) }
    }

    fun voltar() {
        val atualPasso = atual.value.passoAtual
        gravarEntradas()
        atual.update { it.copy(passo = (atualPasso - 1).coerceAtLeast(0), erros = emptyMap(), mensagem = null) }
    }

    fun concluir() {
        val e = atual.value; val q = e.aberto ?: return
        if (e.concluindo || e.semConexao) return
        val invalidas = gravarEntradas()
        val ausentes = faltando(q.blocos, atual.value.respostas).associateWith { ErroResposta.OBRIGATORIA }
        val erros = invalidas + ausentes
        if (erros.isNotEmpty()) {
            val primeiro = atual.value.passos.indexOfFirst { p -> p.perguntas.any { it.chave in erros } }.coerceAtLeast(0)
            atual.update { it.copy(erros = erros, passo = primeiro, mensagem = mensagemDe(erros)) }
            return
        }
        atual.update { it.copy(concluindo = true, mensagem = null, erros = emptyMap()) }
        viewModelScope.launch(dispatcher) {
            try {
                sincronizar()
                anamnese.concluir(q.id)
                atual.update { it.copy(abertoId = null, entradas = emptyMap(), passo = 0, mensagem = MensagemAnamnese.CONCLUIDO, semConexao = false) }
            }
            catch (c: CancellationException) { throw c }
            catch (falha: Exception) { apresentar(falha) }
            finally { atual.update { it.copy(concluindo = false) } }
        }
    }

    /** Grava as digitações válidas na fila offline (só o que mudou); devolve as inválidas. As entradas ficam em memória como camada por cima do banco, que só reflete a gravação depois. */
    private fun gravarEntradas(): Map<Uuid, ErroResposta> {
        val e = atual.value; val q = e.aberto ?: return emptyMap()
        val consolidado = consolidar(e.perguntas, e.entradas)
        val mudou = consolidado.gravar.filter { (pergunta, valor) -> q.respostas[pergunta] != valor }
        if (mudou.isNotEmpty()) {
            try { anamnese.salvarRespostas(q.id, mudou) }
            catch (_: RespostasPendentes) { atual.update { it.copy(mensagem = MensagemAnamnese.FECHADO, abertoId = null) }; return emptyMap() }
            catch (c: CancellationException) { throw c }
            catch (_: Exception) { atual.update { it.copy(mensagem = MensagemAnamnese.ERRO) }; return consolidado.erros }
        }
        return consolidado.erros
    }

    private fun validarPasso(indice: Int): Map<Uuid, ErroResposta> {
        val e = atual.value
        val invalidas = consolidar(e.perguntas, e.entradas).erros
        val respostas = e.respostas
        val ausentes = e.passos.getOrNull(indice)?.perguntas.orEmpty().filter { it.obrigatoria && respostas[it.chave] == null && it.chave !in invalidas }
            .associate { it.chave to ErroResposta.OBRIGATORIA }
        return invalidas + ausentes
    }

    private fun mensagemDe(erros: Map<Uuid, ErroResposta>) =
        if (erros.values.any { it == ErroResposta.INVALIDA }) MensagemAnamnese.ENTRADA_INVALIDA else MensagemAnamnese.OBRIGATORIAS

    private fun apresentar(falha: Throwable) {
        val mensagem = when (falha) {
            is RespostasPendentes -> MensagemAnamnese.PENDENTES
            is FalhaComunicacao -> MensagemAnamnese.SEM_CONEXAO
            is ErroApi -> when (falha.codigo) {
                "QUESTIONARIO_FECHADO" -> MensagemAnamnese.FECHADO
                "OBRIGATORIO" -> MensagemAnamnese.OBRIGATORIAS
                "ASSINATURA_INATIVA" -> MensagemAnamnese.ASSINATURA_INATIVA
                "ACESSO_NEGADO" -> MensagemAnamnese.VINCULO_ENCERRADO
                "TAXA_EXCEDIDA" -> MensagemAnamnese.TAXA_EXCEDIDA
                else -> MensagemAnamnese.ERRO
            }
            else -> MensagemAnamnese.ERRO
        }
        atual.update { it.copy(mensagem = mensagem, semConexao = it.semConexao || falha is FalhaComunicacao || falha is RespostasPendentes) }
    }
}
