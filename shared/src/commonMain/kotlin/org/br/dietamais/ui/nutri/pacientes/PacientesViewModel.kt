package org.br.dietamais.ui.nutri.pacientes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.PortaPacientes
import org.br.dietamais.data.sync.novoIdLocal
import org.br.dietamais.plataforma.*
import kotlin.time.Clock
import kotlin.uuid.Uuid

class PacientesViewModel(private val repositorio: PortaPacientes, private val compartilhamento: Compartilhamento,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate, conectividade: Flow<Boolean>? = null) : ViewModel() {
    private val atual = MutableStateFlow(EstadoPacientes())
    val estado = atual.asStateFlow()
    private var todos = emptyList<PacienteResposta>()
    private var selecionadoId: Uuid? = null
    private var operacao: Job? = null
    private var geracao = 0L
    private val chavesConvite = mutableMapOf<Uuid, Uuid>()

    init {
        conectividade?.let { fluxo -> viewModelScope.launch(dispatcher) {
            fluxo.collect { online -> atual.update { it.copy(semConexao = !online,
                erro = if (online && it.erro == MensagemPacientes.SEM_CONEXAO) null else it.erro) } }
        } }
        viewModelScope.launch(dispatcher) {
            repositorio.observar().catch { falha -> apresentar(falha) }.collect { todos = it; publicarLista() }
        }
        atualizar()
    }

    fun mudarBusca(valor: String) { atual.update { it.copy(busca = valor) }; publicarLista() }
    fun mudarFiltro(valor: StatusVinculo?) { atual.update { it.copy(filtro = valor) }; publicarLista() }
    fun mudarAba(valor: AbaFicha) { atual.update { it.copy(abaFicha = valor) } }
    private fun publicarLista() {
        atual.update { estado -> estado.copy(lista = todos.filter { it.nome.contains(estado.busca.trim(), ignoreCase = true) && (estado.filtro == null || it.status == estado.filtro) },
            selecionado = todos.firstOrNull { it.vinculoId == selecionadoId }) }
    }

    fun selecionar(vinculo: Uuid?) {
        if (selecionadoId != vinculo) { cancelarOperacao(); atual.update { it.copy(convite = null, erro = null, aviso = null, abaFicha = AbaFicha.RESUMO) } }
        selecionadoId = vinculo
        publicarLista()
    }

    fun atualizar() {
        if (atual.value.atualizando) return
        atual.update { it.copy(atualizando = true, erro = null) }
        viewModelScope.launch(dispatcher) {
            try { repositorio.atualizar(); val assinatura = repositorio.assinatura(); atual.update { it.copy(semConexao = false, assinatura = assinatura) } }
            catch (cancelamento: CancellationException) { throw cancelamento }
            catch (falha: Exception) { apresentar(falha) }
            finally { atual.update { it.copy(atualizando = false) } }
        }
    }

    fun abrirCriacao() {
        if (atual.value.envioEmAndamento || atual.value.atualizando) return
        atual.update { it.copy(formulario = FormularioPaciente(), erro = null, aviso = null, errosCampos = emptyMap()) }
    }
    fun fecharCriacao() { cancelarOperacao(); atual.update { it.copy(formulario = null, errosCampos = emptyMap(), erro = null) } }
    fun mudarCampo(campo: CampoPaciente, valor: String) {
        if (atual.value.envioEmAndamento) return
        atual.update { estado -> estado.copy(formulario = estado.formulario?.let { form ->
            when (campo) {
                CampoPaciente.NOME -> form.copy(nome = valor)
                CampoPaciente.EMAIL -> form.copy(email = valor)
                CampoPaciente.NASCIMENTO -> form.copy(nascimento = valor)
                CampoPaciente.TELEFONE -> form.copy(telefone = valor)
                CampoPaciente.ESTATURA -> form.copy(estatura = valor)
                CampoPaciente.OBJETIVO -> form.copy(objetivo = valor)
                CampoPaciente.OBSERVACOES -> form.copy(observacoes = valor)
            }.copy(chave = novoIdLocal())
        }, errosCampos = emptyMap(), erro = null) }
    }
    fun mudarSexo(sexo: Sexo?) {
        if (!atual.value.envioEmAndamento) atual.update { it.copy(formulario = it.formulario?.copy(sexo = sexo, chave = novoIdLocal())) }
    }

    fun criar() {
        val form = atual.value.formulario ?: return
        val campos = mutableMapOf<String, MensagemPacientes>()
        if (form.nome.isBlank()) campos["nome"] = MensagemPacientes.OBRIGATORIO
        if (form.email.isNotBlank() && !Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+").matches(form.email.trim())) campos["email"] = MensagemPacientes.EMAIL_INVALIDO
        val data = if (form.nascimento.isBlank()) null else dataNascimento(form.nascimento)
        if (form.nascimento.isNotBlank() && (data == null || data > Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date)) campos["dataNascimento"] = MensagemPacientes.DATA_INVALIDA
        val estatura = form.estatura.replace(',', '.').toDoubleOrNull()
        if (form.estatura.isNotBlank() && (estatura == null || !estatura.isFinite() || estatura <= 0)) campos["estaturaReferencia"] = MensagemPacientes.ALTURA_INVALIDA
        atual.update { it.copy(errosCampos = campos) }
        if (campos.isNotEmpty()) return
        executar(exigeRede = false) {
            val resposta = repositorio.criar(CriarPacienteRequisicao(form.id, form.nome.trim(), form.email.opcional(), data, form.sexo,
                form.telefone.opcional(), estatura, form.objetivo.opcional(), form.observacoes.opcional()), form.chave)
            selecionadoId = resposta.vinculoId
            atual.update { it.copy(formulario = null, aviso = MensagemPacientes.PACIENTE_CRIADO) }
            publicarLista()
        }
    }

    fun gerarConvite() {
        val paciente = atual.value.selecionado?.takeIf { it.status == StatusVinculo.CONVIDADO } ?: return
        val chave = chavesConvite.getOrPut(paciente.vinculoId) { novoIdLocal() }
        executar {
            val convite = repositorio.convidar(paciente.id, chave)
            chavesConvite.remove(paciente.vinculoId)
            atual.update { it.copy(convite = convite) }
        }
    }
    fun fecharConvite() { cancelarOperacao(); atual.update { it.copy(convite = null) } }
    fun fecharLimite() { atual.update { it.copy(limiteAtingido = false) } }
    fun compartilharConvite(texto: String) {
        if (atual.value.convite == null) return
        executar(exigeRede = false) {
            val resultado = compartilhamento.compartilhar(texto)
            atual.update { it.copy(aviso = if (resultado == ResultadoCompartilhamento.COPIADO) MensagemPacientes.CONVITE_COPIADO else MensagemPacientes.COMPARTILHAMENTO_ABERTO) }
        }
    }

    private fun executar(exigeRede: Boolean = true, bloco: suspend () -> Unit) {
        if (atual.value.envioEmAndamento || (exigeRede && atual.value.semConexao)) return
        val esperada = geracao
        atual.update { it.copy(envioEmAndamento = true, erro = null, aviso = null) }
        operacao = viewModelScope.launch(dispatcher) {
            try { bloco() }
            catch (cancelamento: CancellationException) { throw cancelamento }
            catch (falha: Exception) { if (esperada == geracao) apresentar(falha) }
            finally { if (esperada == geracao) atual.update { it.copy(envioEmAndamento = false) } }
        }
    }
    private fun cancelarOperacao() { geracao++; operacao?.cancel(); atual.update { it.copy(envioEmAndamento = false) } }
    private fun apresentar(falha: Throwable) {
        val mensagem = when (falha) {
            is FalhaComunicacao -> MensagemPacientes.SEM_CONEXAO
            is CompartilhamentoIndisponivel -> MensagemPacientes.COMPARTILHAMENTO_FALHOU
            is ErroApi -> when (falha.codigo) {
                "LIMITE_PACIENTES_ATINGIDO" -> MensagemPacientes.LIMITE_ATINGIDO
                "ASSINATURA_INATIVA" -> MensagemPacientes.ASSINATURA_INATIVA
                "EMAIL_NAO_CONFIRMADO" -> MensagemPacientes.EMAIL_NAO_CONFIRMADO
                "VINCULO_JA_EXISTE" -> MensagemPacientes.VINCULO_JA_EXISTE
                "TAXA_EXCEDIDA" -> MensagemPacientes.TAXA_EXCEDIDA
                "NAO_IMPLEMENTADO" -> MensagemPacientes.CONVITES_INDISPONIVEIS
                "CONSENTIMENTO_OBRIGATORIO" -> MensagemPacientes.CONSENTIMENTO_INDISPONIVEL
                "RECURSO_NAO_ENCONTRADO" -> MensagemPacientes.NAO_ENCONTRADO
                else -> MensagemPacientes.ERRO_PADRAO
            }
            else -> MensagemPacientes.ERRO_PADRAO
        }
        atual.update { it.copy(erro = mensagem, semConexao = it.semConexao || falha is FalhaComunicacao,
            limiteAtingido = mensagem == MensagemPacientes.LIMITE_ATINGIDO,
            errosCampos = (falha as? ErroApi)?.campos?.associate { it.campo to if (it.codigo == "OBRIGATORIO") MensagemPacientes.OBRIGATORIO else MensagemPacientes.CAMPO_INVALIDO }.orEmpty()) }
    }
    private fun String.opcional() = trim().takeIf { it.isNotEmpty() }
    private fun dataNascimento(valor: String): LocalDate? = try {
        val partes = valor.split('/')
        if (partes.size != 3 || partes[2].length != 4) null else LocalDate(partes[2].toInt(), partes[1].toInt(), partes[0].toInt())
    } catch (_: Exception) { null }
}
