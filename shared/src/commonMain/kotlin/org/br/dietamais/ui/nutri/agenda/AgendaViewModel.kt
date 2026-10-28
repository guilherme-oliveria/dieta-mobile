package org.br.dietamais.ui.nutri.agenda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.*
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.PortaConsultas
import org.br.dietamais.data.repositorio.PortaPacientes
import org.br.dietamais.data.sync.novoIdLocal
import org.br.dietamais.dominio.agenda.*
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

/** Agenda da nutri (MOB-26): dia/semana lidos do banco local; criar, remarcar e cancelar gravam offline e vão na fila (o backend decide no sync, RN-AGE-01). */
class AgendaViewModel(private val consultas: PortaConsultas, private val pacientes: PortaPacientes, private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    conectividade: Flow<Boolean>? = null, private val agora: () -> Instant = { Clock.System.now() }, private val fuso: () -> TimeZone = { TimeZone.currentSystemDefault() }) : ViewModel() {
    private val atual = MutableStateFlow(EstadoAgenda(dia = agora().toLocalDateTime(fuso()).date))
    val estado = atual.asStateFlow()
    private var centro: LocalDate? = null
    private var operacao: Job? = null

    init {
        conectividade?.let { fluxo -> viewModelScope.launch(dispatcher) { fluxo.collect { online -> atual.update { it.copy(semConexao = !online) } } } }
        viewModelScope.launch(dispatcher) {
            consultas.observar().catch { atual.update { e -> e.copy(erro = MensagemAgenda.ERRO) } }.collect { lista -> atual.update { it.copy(consultas = lista) } }
        }
        viewModelScope.launch(dispatcher) {
            pacientes.observar().catch { }.collect { lista -> atual.update { it.copy(pacientes = lista) } }
        }
    }

    fun abrir() { if (centro == null) recarregar() }

    fun recarregar() {
        if (atual.value.atualizando) return
        val dia = atual.value.dia
        atual.update { it.copy(atualizando = true, erro = null) }
        viewModelScope.launch(dispatcher) {
            try {
                // janela larga ao redor do dia: a nutri costuma navegar para frente e para trás
                val z = fuso()
                consultas.atualizar(dia.minus(30, DateTimeUnit.DAY).atStartOfDayIn(z), dia.plus(60, DateTimeUnit.DAY).atStartOfDayIn(z))
                centro = dia
                atual.update { it.copy(semConexao = false) }
            }
            catch (c: CancellationException) { throw c }
            catch (falha: Exception) { apresentar(falha) }
            finally { atual.update { it.copy(atualizando = false) } }
        }
    }

    fun mudarModo(modo: ModoAgenda) = atual.update { it.copy(modo = modo) }
    fun hoje() = irPara(agora().toLocalDateTime(fuso()).date)
    fun anterior() = irPara(atual.value.dia.minus(passo(), DateTimeUnit.DAY))
    fun proximo() = irPara(atual.value.dia.plus(passo(), DateTimeUnit.DAY))
    fun irPara(dia: LocalDate) {
        atual.update { it.copy(dia = dia) }
        val c = centro
        if (c != null && c.daysUntil(dia).let { it > 20 || it < -20 }) recarregar()
    }
    private fun passo() = if (atual.value.modo == ModoAgenda.SEMANA) 7 else 1

    fun nova(pacienteId: Uuid? = null) {
        if (atual.value.enviando) return
        atual.update { it.copy(formulario = FormularioConsulta(pacienteId = pacienteId, data = formatarData(it.dia), chave = novoIdLocal()), errosFormulario = emptySet(), conflitosAvisados = 0, erro = null, aviso = null) }
    }

    fun remarcar(id: Uuid) {
        val c = atual.value.consultas.firstOrNull { it.id == id && it.status == StatusConsulta.AGENDADA } ?: return
        if (atual.value.enviando) return
        atual.update { it.copy(formulario = formularioDe(c, fuso()), errosFormulario = emptySet(), conflitosAvisados = 0, erro = null, aviso = null) }
    }

    fun fechar() { if (!atual.value.enviando) atual.update { it.copy(formulario = null, errosFormulario = emptySet(), conflitosAvisados = 0) } }
    fun mudarPaciente(id: Uuid) = formulario { it.copy(pacienteId = id) }
    fun mudarData(valor: String) = formulario { it.copy(data = valor.take(10)) }
    fun mudarHora(valor: String) = formulario { it.copy(hora = valor.take(5)) }
    fun mudarDuracao(valor: String) = formulario { it.copy(duracao = valor.filter(Char::isDigit).take(3)) }
    fun mudarTipo(tipo: TipoConsulta) = formulario { it.copy(tipo = tipo) }
    fun mudarLugar(valor: String) = formulario { it.copy(lugar = valor.take(200)) }

    fun salvar() {
        val e = atual.value; val form = e.formulario ?: return
        if (e.enviando) return
        val resultado = validarFormulario(form, fuso(), agora(), exigePaciente = form.id == null)
        if (resultado.erros.isNotEmpty()) { atual.update { it.copy(errosFormulario = resultado.erros) }; return }
        val inicio = checkNotNull(resultado.inicio); val duracao = checkNotNull(resultado.duracaoMin)
        // aviso de sobreposição: o backend aceita, a nutri confirma tocando de novo
        val sobrepostas = conflitos(e.consultas, inicio, duracao, ignorar = form.id)
        if (sobrepostas.isNotEmpty() && e.conflitosAvisados != sobrepostas.size) { atual.update { it.copy(conflitosAvisados = sobrepostas.size) }; return }
        atual.update { it.copy(enviando = true, erro = null, aviso = null, errosFormulario = emptySet()) }
        operacao = viewModelScope.launch(dispatcher) {
            try {
                if (form.id == null) {
                    consultas.criar(CriarConsultaRequisicao(form.chave, checkNotNull(form.pacienteId), inicio, duracao, form.tipo, form.lugar.trim().ifEmpty { null }))
                    atual.update { it.copy(formulario = null, conflitosAvisados = 0, aviso = MensagemAgenda.AGENDADA, dia = diaLocal(inicio, fuso())) }
                } else {
                    val original = e.consultas.firstOrNull { it.id == form.id }
                    val pedido = AtualizarConsultaRequisicao(inicio.takeIf { original == null || it != original.inicio }, duracao.takeIf { original == null || it != original.duracaoMin },
                        form.tipo.takeIf { original == null || it != original.tipo }, form.lugar.trim().takeIf { it != original?.lugar.orEmpty() })
                    if (pedido == AtualizarConsultaRequisicao()) atual.update { it.copy(formulario = null) }
                    else {
                        consultas.alterar(form.id, checkNotNull(form.versao), pedido)
                        atual.update { it.copy(formulario = null, conflitosAvisados = 0, aviso = MensagemAgenda.REMARCADA, dia = diaLocal(inicio, fuso())) }
                    }
                }
            }
            catch (c: CancellationException) { throw c }
            catch (falha: Exception) { apresentar(falha) }
            finally { atual.update { it.copy(enviando = false) } }
        }
    }

    fun pedirCancelamento(id: Uuid) { if (!atual.value.enviando) atual.update { it.copy(cancelandoId = id, erro = null, aviso = null) } }
    fun desistirDoCancelamento() { if (!atual.value.enviando) atual.update { it.copy(cancelandoId = null) } }

    fun confirmarCancelamento() {
        val e = atual.value; val id = e.cancelandoId ?: return
        val consulta = e.consultas.firstOrNull { it.id == id } ?: run { atual.update { it.copy(cancelandoId = null) }; return }
        if (e.enviando) return
        atual.update { it.copy(enviando = true, erro = null, aviso = null) }
        operacao = viewModelScope.launch(dispatcher) {
            try { consultas.alterar(id, consulta.versao, AtualizarConsultaRequisicao(status = StatusConsulta.CANCELADA)); atual.update { it.copy(cancelandoId = null, aviso = MensagemAgenda.CANCELADA) } }
            catch (c: CancellationException) { throw c }
            catch (falha: Exception) { atual.update { it.copy(cancelandoId = null) }; apresentar(falha) }
            finally { atual.update { it.copy(enviando = false) } }
        }
    }

    private fun formulario(bloco: (FormularioConsulta) -> FormularioConsulta) =
        atual.update { e -> e.formulario?.let { e.copy(formulario = bloco(it), errosFormulario = emptySet(), conflitosAvisados = 0) } ?: e }

    private fun apresentar(falha: Throwable) {
        val mensagem = when (falha) {
            is FalhaComunicacao -> MensagemAgenda.SEM_CONEXAO
            is ErroApi -> when (falha.codigo) {
                "CONFLITO_VERSAO" -> MensagemAgenda.CONFLITO
                "CONSULTA_CANCELADA" -> MensagemAgenda.JA_CANCELADA
                "FORMATO_INVALIDO" -> MensagemAgenda.DATA_INVALIDA
                "ASSINATURA_INATIVA" -> MensagemAgenda.ASSINATURA_INATIVA
                "ACESSO_NEGADO" -> MensagemAgenda.VINCULO_ENCERRADO
                "RECURSO_NAO_ENCONTRADO" -> MensagemAgenda.NAO_ENCONTRADA
                "TAXA_EXCEDIDA" -> MensagemAgenda.TAXA_EXCEDIDA
                else -> MensagemAgenda.ERRO
            }
            else -> MensagemAgenda.ERRO
        }
        atual.update { it.copy(erro = mensagem, semConexao = it.semConexao || falha is FalhaComunicacao) }
        // outra versão (ou cancelamento) já está no servidor: fecha o formulário e traz o estado novo
        if (mensagem == MensagemAgenda.CONFLITO || mensagem == MensagemAgenda.JA_CANCELADA) { atual.update { it.copy(formulario = null) }; recarregar() }
    }
}
