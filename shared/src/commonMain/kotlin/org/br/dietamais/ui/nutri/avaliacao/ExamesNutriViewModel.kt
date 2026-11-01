package org.br.dietamais.ui.nutri.avaliacao

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.PortaAvaliacoes
import org.br.dietamais.data.repositorio.PortaExames
import org.br.dietamais.dominio.avaliacao.*
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

enum class MensagemExames { SEM_CONEXAO, SEM_NASCIMENTO, NUMERO_INVALIDO, CONFLITO, JUSTIFICATIVA, ASSINATURA_INATIVA, VINCULO_ENCERRADO, NAO_ENCONTRADO, TAXA_EXCEDIDA, ERRO, SALVO }

/** [valores] e [condicoes] são o que a nutri digitou por código de exame; [invalidos] marca o campo numérico que não é número. */
data class EstadoExamesNutri(
    val avaliacao: AvaliacaoResposta? = null, val catalogo: List<ExameCatalogoDto> = emptyList(), val busca: String = "", val secao: String? = null,
    val valores: Map<String, String> = emptyMap(), val condicoes: Map<String, String> = emptyMap(), val mostrarAoPaciente: Boolean = false,
    val justificativa: String = "", val exigeJustificativa: Boolean = false, val invalidos: Set<String> = emptySet(),
    val carregando: Boolean = false, val enviando: Boolean = false, val semConexao: Boolean = false,
    val erro: MensagemExames? = null, val aviso: MensagemExames? = null,
) {
    val secoes: List<String> get() = catalogo.map { it.secao }.distinct()
    val visiveis: List<ExameCatalogoDto> get() = filtrarCatalogo(catalogo, busca, secao)
    override fun toString() = "EstadoExamesNutri[conteúdo clínico protegido]"
}

/**
 * Lançar exames na avaliação (MOB-60b, RN-EXA-01): a nutri escolhe no catálogo da faixa etária da ficha (online) e digita o resultado; quem classifica é o servidor.
 * Salvar regrava a avaliação com a lista de exames (offline-first, como as demais edições) e respeita a janela de 30 dias com justificativa (RN-AVA-04).
 */
class ExamesNutriViewModel(private val exames: PortaExames, private val avaliacoes: PortaAvaliacoes, private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    conectividade: Flow<Boolean>? = null, private val agora: () -> Instant = { Clock.System.now() }) : ViewModel() {
    private val atual = MutableStateFlow(EstadoExamesNutri())
    val estado = atual.asStateFlow()
    private var carga: Job? = null

    init {
        conectividade?.let { fluxo -> viewModelScope.launch(dispatcher) { fluxo.collect { online -> atual.update { it.copy(semConexao = !online) } } } }
    }

    fun abrir(avaliacao: AvaliacaoResposta) {
        carga?.cancel()
        val faixa = faixaDoCatalogo(avaliacao.faixaEtaria)
        val gravados = avaliacao.exames.orEmpty()
        atual.update { EstadoExamesNutri(avaliacao = avaliacao, valores = gravados.associate { it.codigo to textoDoExame(it) },
            condicoes = gravados.mapNotNull { e -> e.condicao?.let { e.codigo to it } }.toMap(), mostrarAoPaciente = avaliacao.compartilhamento?.exames == true,
            exigeJustificativa = agora() - avaliacao.criadoEm > 30.days, semConexao = it.semConexao, carregando = faixa != null,
            erro = if (faixa == null) MensagemExames.SEM_NASCIMENTO else null) }
        if (faixa == null) return
        carga = viewModelScope.launch(dispatcher) {
            try { val lista = exames.catalogo(faixa); atual.update { it.copy(catalogo = lista, secao = lista.firstOrNull()?.secao, semConexao = false) } }
            catch (c: CancellationException) { throw c }
            catch (falha: Exception) { apresentar(falha) }
            finally { atual.update { it.copy(carregando = false) } }
        }
    }

    fun fechar() { if (!atual.value.enviando) { carga?.cancel(); atual.value = EstadoExamesNutri(semConexao = atual.value.semConexao) } }
    fun mudarBusca(texto: String) = atual.update { it.copy(busca = texto.take(60)) }
    fun escolherSecao(secao: String?) = atual.update { it.copy(secao = secao) }
    fun mudarValor(codigo: String, texto: String) = atual.update { it.copy(valores = it.valores + (codigo to texto.take(100)), invalidos = it.invalidos - codigo) }
    fun mudarCondicao(codigo: String, condicao: String?) = atual.update { it.copy(condicoes = if (condicao == null) it.condicoes - codigo else it.condicoes + (codigo to condicao)) }
    fun mudarMostrarAoPaciente(valor: Boolean) = atual.update { it.copy(mostrarAoPaciente = valor) }
    fun mudarJustificativa(texto: String) = atual.update { it.copy(justificativa = texto.take(1000), erro = null) }

    fun salvar() {
        val e = atual.value; val av = e.avaliacao ?: return
        if (e.enviando || e.carregando) return
        val pronto = montarExames(e.catalogo, e.valores, e.condicoes)
        if (pronto.invalidos.isNotEmpty()) { atual.update { it.copy(invalidos = pronto.invalidos, erro = MensagemExames.NUMERO_INVALIDO) }; return }
        if (e.exigeJustificativa && e.justificativa.isBlank()) { atual.update { it.copy(erro = MensagemExames.JUSTIFICATIVA) }; return }
        val requisicao = montarRequisicao(valoresDe(av), av.dataHora, av.parecer.orEmpty(), av.compartilhada, if (e.exigeJustificativa) e.justificativa else null, av.id,
            av.protocolo, (av.compartilhamento ?: CompartilhamentoAvaliacaoDto()).copy(exames = e.mostrarAoPaciente),
            escolhasDe(av.energia, av.complementares?.nivelAtividade, av.complementares?.objetivo, av.complementares?.objetivoKgPorMes)).copy(exames = pronto.lancados)
        atual.update { it.copy(enviando = true, erro = null, aviso = null) }
        viewModelScope.launch(dispatcher) {
            try { avaliacoes.salvar(av.id, av.versao, requisicao); atual.update { it.copy(aviso = MensagemExames.SALVO, avaliacao = null) } }
            catch (c: CancellationException) { throw c }
            catch (falha: Exception) { apresentar(falha) }
            finally { atual.update { it.copy(enviando = false) } }
        }
    }

    private fun apresentar(falha: Throwable) {
        val mensagem = when (falha) {
            is FalhaComunicacao -> MensagemExames.SEM_CONEXAO
            is ErroApi -> when (falha.codigo) {
                "CONFLITO_VERSAO" -> MensagemExames.CONFLITO
                "JUSTIFICATIVA_OBRIGATORIA" -> MensagemExames.JUSTIFICATIVA
                "ASSINATURA_INATIVA" -> MensagemExames.ASSINATURA_INATIVA
                "ACESSO_NEGADO" -> MensagemExames.VINCULO_ENCERRADO
                "RECURSO_NAO_ENCONTRADO" -> MensagemExames.NAO_ENCONTRADO
                "FORMATO_INVALIDO" -> MensagemExames.NUMERO_INVALIDO
                "TAXA_EXCEDIDA" -> MensagemExames.TAXA_EXCEDIDA
                else -> MensagemExames.ERRO
            }
            else -> MensagemExames.ERRO
        }
        atual.update { it.copy(erro = mensagem, semConexao = it.semConexao || falha is FalhaComunicacao, exigeJustificativa = it.exigeJustificativa || mensagem == MensagemExames.JUSTIFICATIVA) }
    }
}
