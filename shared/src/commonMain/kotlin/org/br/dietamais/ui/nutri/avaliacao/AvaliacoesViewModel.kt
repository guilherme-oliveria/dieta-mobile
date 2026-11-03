package org.br.dietamais.ui.nutri.avaliacao

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.br.dietamais.data.remoto.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.br.dietamais.data.remoto.dto.ClassificacaoImc
import org.br.dietamais.data.remoto.dto.CompartilhamentoAvaliacaoDto
import org.br.dietamais.data.remoto.dto.ProtocoloAvaliacaoDto
import org.br.dietamais.data.remoto.dto.Sexo
import org.br.dietamais.dominio.calculos.ProtocoloDobras
import org.br.dietamais.dominio.calculos.ProtocoloMassa
import org.br.dietamais.data.repositorio.PortaAvaliacoes
import org.br.dietamais.dominio.avaliacao.*
import org.br.dietamais.data.sync.novoIdLocal
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlin.uuid.Uuid

class AvaliacoesViewModel(private val avaliacoes: PortaAvaliacoes, private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    conectividade: Flow<Boolean>? = null, private val agora: () -> Instant = { Clock.System.now() }) : ViewModel() {
    private val atual = MutableStateFlow(EstadoAvaliacoes())
    val estado = atual.asStateFlow()
    private var leitura: Job? = null
    private var operacao: Job? = null

    init {
        conectividade?.let { fluxo -> viewModelScope.launch(dispatcher) { fluxo.collect { online -> atual.update { it.copy(semConexao = !online) } } } }
    }

    fun abrir(pacienteId: Uuid) {
        if (atual.value.pacienteId == pacienteId) return
        leitura?.cancel(); operacao?.cancel()
        atual.value = EstadoAvaliacoes(pacienteId = pacienteId, semConexao = atual.value.semConexao)
        leitura = viewModelScope.launch(dispatcher) {
            avaliacoes.observar(pacienteId).catch { atual.update { e -> e.copy(erro = MensagemAvaliacao.ERRO) } }.collect { lista ->
                atual.update { e -> e.copy(itens = lista, selecionadas = e.selecionadas.filter { id -> lista.any { it.id == id } }) }
            }
        }
        recarregar()
    }

    /** Sexo e nascimento da ficha: só para a prévia local dos cálculos. */
    fun definirPaciente(sexo: Sexo?, nascimento: LocalDate?) = atual.update { if (it.sexo == sexo && it.nascimento == nascimento) it else it.copy(sexo = sexo, nascimento = nascimento) }

    /** Perfil paciente: lê só o que o servidor compartilhou, direto do banco local (o sync traz as novas). */
    fun abrirComoPaciente() {
        if (atual.value.somenteLeitura) return
        leitura?.cancel(); operacao?.cancel()
        atual.value = EstadoAvaliacoes(somenteLeitura = true, semConexao = atual.value.semConexao)
        leitura = viewModelScope.launch(dispatcher) {
            avaliacoes.observar(null).catch { atual.update { e -> e.copy(erro = MensagemAvaliacao.ERRO) } }.collect { lista ->
                atual.update { e -> e.copy(itens = lista.filter { it.compartilhada }, selecionadas = e.selecionadas.filter { id -> lista.any { it.id == id } }) }
            }
        }
    }

    fun recarregar() {
        val id = atual.value.pacienteId ?: return
        if (atual.value.atualizando) return
        atual.update { it.copy(atualizando = true, erro = null) }
        viewModelScope.launch(dispatcher) {
            try { avaliacoes.atualizar(id); atual.update { it.copy(semConexao = false) } }
            catch (c: CancellationException) { throw c }
            catch (falha: Exception) { apresentar(falha) }
            finally { atual.update { it.copy(atualizando = false) } }
        }
    }

    fun nova() { if (!ocupado()) atual.update { it.copy(formulario = FormularioAvaliacao(chave = novoIdLocal()), errosMedidas = emptyMap(), justificativaFaltando = false, erro = null, aviso = null) } }

    fun editar(id: Uuid) {
        val item = atual.value.itens.firstOrNull { it.id == id } ?: return
        if (ocupado()) return
        val antiga = agora() - item.criadoEm > 30.days
        atual.update { it.copy(formulario = FormularioAvaliacao(id = item.id, versao = item.versao, dataHora = item.dataHora, valores = valoresDe(item),
            parecer = item.parecer.orEmpty(), compartilhada = item.compartilhada, exigeJustificativa = antiga,
            protocoloDobras = item.protocolo?.dobras?.let { v -> ProtocoloDobras.entries.firstOrNull { it.name == v } } ?: ProtocoloDobras.SETE,
            protocoloMassa = item.protocolo?.massa?.let { v -> ProtocoloMassa.entries.firstOrNull { it.name == v } } ?: ProtocoloMassa.MAGRO,
            compartilhamento = item.compartilhamento ?: CompartilhamentoAvaliacaoDto()), errosMedidas = emptyMap(), justificativaFaltando = false, erro = null, aviso = null) }
    }

    fun fechar() { if (!atual.value.enviando) atual.update { it.copy(formulario = null, errosMedidas = emptyMap(), justificativaFaltando = false) } }
    fun mudarMedida(medida: MedidaAvaliacao, valor: String) = formulario { it.copy(valores = it.valores + (medida to valor.take(10))) }
        .also { atual.update { e -> e.copy(errosMedidas = e.errosMedidas - medida) } }
    fun mudarParecer(valor: String) = formulario { it.copy(parecer = valor.take(10000), parecerPreenchidoAuto = false) }
    fun mudarProtocoloDobras(valor: ProtocoloDobras) = formulario { it.copy(protocoloDobras = valor) }
    fun mudarProtocoloMassa(valor: ProtocoloMassa) = formulario { it.copy(protocoloMassa = valor) }
    fun mudarCompartilhamento(medidas: Boolean? = null, composicao: Boolean? = null, resumo: Boolean? = null) = formulario {
        it.copy(compartilhamento = it.compartilhamento.copy(medidas = medidas ?: it.compartilhamento.medidas,
            composicao = composicao ?: it.compartilhamento.composicao, resumo = resumo ?: it.compartilhamento.resumo))
    }

    /** Repete as medidas da avaliação mais recente do paciente (peso, estatura, perímetros, dobras e complementares). */
    fun repetirMedidasDaUltima() {
        val ultima = atual.value.itens.firstOrNull() ?: return
        formulario { it.copy(valores = it.valores + medidasParaRepetir(ultima)) }
        atual.update { it.copy(errosMedidas = emptyMap()) }
    }

    /** Só avança se as medidas do passo atual forem válidas; a composição e o resumo nunca bloqueiam (RN-CAL-04: dado faltante só avisa). */
    fun proximoPasso() {
        val form = atual.value.formulario ?: return
        val proximo = PassoAvaliacao.entries.getOrNull(form.passo.ordinal + 1) ?: return
        val erros = validarMedidas(form.valores).filterKeys { it in medidasDoPasso(form.passo) }
        if (erros.isNotEmpty()) { atual.update { it.copy(errosMedidas = erros) }; return }
        formulario { f ->
            val resumo = if (proximo == PassoAvaliacao.RESUMO && f.parecer.isBlank()) textoGerado(f) else null
            f.copy(passo = proximo, parecer = resumo ?: f.parecer, parecerPreenchidoAuto = resumo != null || f.parecerPreenchidoAuto)
        }
        atual.update { it.copy(errosMedidas = emptyMap()) }
    }
    fun passoAnterior() { formulario { f -> PassoAvaliacao.entries.getOrNull(f.passo.ordinal - 1)?.let { f.copy(passo = it) } ?: f } }

    /** Prévia dos resultados com os dados da ficha; nula no perfil paciente. */
    fun previa(): PreviaAvaliacao? = atual.value.formulario?.let { previaDe(atual.value, it, agora()) }
    private fun textoGerado(f: FormularioAvaliacao) = previaDe(atual.value, f, agora()).resultado?.textoResumo.orEmpty()
    fun mudarCompartilhada(valor: Boolean) = formulario { it.copy(compartilhada = valor) }
    fun mudarJustificativa(valor: String) { formulario { it.copy(justificativa = valor.take(1000)) }; atual.update { it.copy(justificativaFaltando = false) } }

    /** Prévia do IMC; nula enquanto peso ou estatura não forem válidos. */
    fun previaImc(): Pair<Double, ClassificacaoImc>? = atual.value.formulario?.let { previaImc(it.valores) }

    fun salvar() {
        val e = atual.value; val form = e.formulario ?: return; val paciente = e.pacienteId ?: return
        if (ocupado()) return
        val erros = validarMedidas(form.valores)
        val semJustificativa = form.exigeJustificativa && form.justificativa.isBlank()
        if (erros.isNotEmpty() || semJustificativa) { atual.update { it.copy(errosMedidas = erros, justificativaFaltando = semJustificativa) }; return }
        val requisicao = montarRequisicao(form.valores, form.dataHora ?: agora(), form.parecer, form.compartilhada,
            if (form.exigeJustificativa) form.justificativa else null, form.id,
            ProtocoloAvaliacaoDto(form.protocoloDobras.name, form.protocoloMassa.name), form.compartilhamento)
        atual.update { it.copy(enviando = true, erro = null, aviso = null, errosMedidas = emptyMap()) }
        operacao = viewModelScope.launch(dispatcher) {
            try {
                if (form.id == null) avaliacoes.criar(paciente, requisicao.copy(id = form.chave)) else avaliacoes.salvar(form.id, checkNotNull(form.versao), requisicao)
                atual.update { it.copy(formulario = null, aviso = MensagemAvaliacao.SALVO) }
            }
            catch (c: CancellationException) { throw c }
            catch (falha: Exception) { apresentar(falha) }
            finally { atual.update { it.copy(enviando = false) } }
        }
    }

    fun alternarSelecao(id: Uuid) = atual.update { e ->
        val novas = if (id in e.selecionadas) e.selecionadas - id else (e.selecionadas + id).takeLast(2)
        e.copy(selecionadas = novas, comparando = e.comparando && novas.size == 2)
    }
    fun comparar() { if (atual.value.selecionadas.size == 2) atual.update { it.copy(comparando = true) } }
    fun voltarDaComparacao() = atual.update { it.copy(comparando = false) }

    private fun ocupado() = atual.value.enviando
    private fun formulario(bloco: (FormularioAvaliacao) -> FormularioAvaliacao) = atual.update { e -> e.formulario?.let { e.copy(formulario = bloco(it)) } ?: e }

    private fun apresentar(falha: Throwable) {
        if (falha is ErroApi && falha.codigo == "VALOR_FORA_DO_INTERVALO") {
            val medida = falha.campos.firstNotNullOfOrNull { medidaDoCampo(it.campo) }
            if (medida != null) { atual.update { it.copy(errosMedidas = it.errosMedidas + (medida to ErroMedida.FORA_DO_INTERVALO)) }; return }
        }
        if (falha is ErroApi && falha.codigo == "JUSTIFICATIVA_OBRIGATORIA") {
            formulario { it.copy(exigeJustificativa = true) }; atual.update { it.copy(justificativaFaltando = true, erro = MensagemAvaliacao.JUSTIFICATIVA) }; return
        }
        val mensagem = when (falha) {
            is FalhaComunicacao -> MensagemAvaliacao.SEM_CONEXAO
            is ErroApi -> when (falha.codigo) {
                "CONFLITO_VERSAO" -> MensagemAvaliacao.CONFLITO
                "ASSINATURA_INATIVA" -> MensagemAvaliacao.ASSINATURA_INATIVA
                "ACESSO_NEGADO" -> MensagemAvaliacao.VINCULO_ENCERRADO
                "RECURSO_NAO_ENCONTRADO" -> MensagemAvaliacao.NAO_ENCONTRADO
                "TAXA_EXCEDIDA" -> MensagemAvaliacao.TAXA_EXCEDIDA
                else -> MensagemAvaliacao.ERRO
            }
            else -> MensagemAvaliacao.ERRO
        }
        atual.update { it.copy(erro = mensagem, semConexao = it.semConexao || falha is FalhaComunicacao) }
        if (mensagem == MensagemAvaliacao.CONFLITO) recarregarDepoisDoConflito()
    }

    private fun recarregarDepoisDoConflito() { atual.update { it.copy(formulario = null) }; recarregar() }
}

fun previaImc(valores: Map<MedidaAvaliacao, String>): Pair<Double, ClassificacaoImc>? {
    val peso = lerDecimal(valores[MedidaAvaliacao.PESO].orEmpty()) ?: return null
    val altura = lerDecimal(valores[MedidaAvaliacao.ESTATURA].orEmpty()) ?: return null
    if (peso !in MedidaAvaliacao.PESO.min..MedidaAvaliacao.PESO.max || altura !in MedidaAvaliacao.ESTATURA.min..MedidaAvaliacao.ESTATURA.max) return null
    val imc = imcDe(peso, altura)
    return imc to classificarImc(imc)
}

/** O backend nomeia os campos como `pesoKg`, `perimetros.cintura`, `dobras.triceps`... */
fun medidaDoCampo(campo: String): MedidaAvaliacao? = when (campo) {
    "pesoKg" -> MedidaAvaliacao.PESO
    "estaturaM" -> MedidaAvaliacao.ESTATURA
    "perimetros.pescoco" -> MedidaAvaliacao.PESCOCO
    "perimetros.torax" -> MedidaAvaliacao.TORAX
    "perimetros.cintura" -> MedidaAvaliacao.CINTURA
    "perimetros.abdomen" -> MedidaAvaliacao.ABDOMEN
    "perimetros.quadril" -> MedidaAvaliacao.QUADRIL
    "perimetros.braco" -> MedidaAvaliacao.BRACO
    "perimetros.antebraco" -> MedidaAvaliacao.ANTEBRACO
    "perimetros.coxa" -> MedidaAvaliacao.COXA
    "perimetros.panturrilha" -> MedidaAvaliacao.PANTURRILHA
    "dobras.triceps" -> MedidaAvaliacao.DOBRA_TRICEPS
    "dobras.subescapular" -> MedidaAvaliacao.DOBRA_SUBESCAPULAR
    "dobras.peitoral" -> MedidaAvaliacao.DOBRA_PEITORAL
    "dobras.axilarMedia" -> MedidaAvaliacao.DOBRA_AXILAR_MEDIA
    "dobras.suprailiaca" -> MedidaAvaliacao.DOBRA_SUPRAILIACA
    "dobras.abdominal" -> MedidaAvaliacao.DOBRA_ABDOMINAL
    "dobras.coxa" -> MedidaAvaliacao.DOBRA_COXA
    "composicao.percentualGordura" -> MedidaAvaliacao.PERCENTUAL_GORDURA
    "composicao.pesoGordoKg" -> MedidaAvaliacao.PESO_GORDO
    "composicao.pesoMagroKg" -> MedidaAvaliacao.PESO_MAGRO
    else -> null
}

/** Prévia com o dia da avaliação no fuso do aparelho (o servidor usa o de São Paulo; a diferença só aparece no dia exato do aniversário). */
fun previaDe(estado: EstadoAvaliacoes, form: FormularioAvaliacao, agora: Instant, fuso: TimeZone = TimeZone.currentSystemDefault()): PreviaAvaliacao =
    previaAvaliacao(form.valores, estado.sexo, estado.nascimento, (form.dataHora ?: agora).toLocalDateTime(fuso).date, form.protocoloDobras, form.protocoloMassa)
