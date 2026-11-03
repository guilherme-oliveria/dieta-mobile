package org.br.dietamais.ui.auth.convite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.PortaConvites
import org.br.dietamais.data.sync.novoIdLocal
import org.br.dietamais.dominio.convite.extrairCodigoConvite
import org.br.dietamais.dominio.contas.permiteCadastro
import org.br.dietamais.plataforma.idFusoDoAparelho
import org.br.dietamais.plataforma.ArmazenamentoSeguroIndisponivel

class ConviteViewModel(private val repositorio: PortaConvites, private val dominios: Set<String> = emptySet(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate) : ViewModel() {
    private val atual = MutableStateFlow(EstadoConvite())
    val estado = atual.asStateFlow()
    private var usuario: UsuarioResposta? = null
    private var geracao = 0L
    private var operacao: Job? = null
    private var chave = novoIdLocal()

    fun abrir(entrada: String = ""): Boolean {
        val codigo = extrairCodigoConvite(entrada, dominios)
        if (atual.value.visivel && atual.value.convite != null && codigo != null && codigo == atual.value.codigo) return true
        if (atual.value.aceitando) return false
        cancelarConsulta()
        chave = novoIdLocal()
        atual.value = EstadoConvite(visivel = true, codigo = entrada.take(2049))
        if (entrada.isNotBlank()) consultar()
        return true
    }
    fun mudarCodigo(valor: String) {
        if (!atual.value.carregando) atual.update { it.copy(codigo = valor.take(2049), convite = null, aceitouConsentimento = false, erro = null) }
    }
    fun consultar() {
        if (atual.value.carregando) return
        val codigo = extrairCodigoConvite(atual.value.codigo, dominios)
        if (codigo == null) { atual.update { it.copy(erro = MensagemConvite.CODIGO_INVALIDO) }; return }
        chave = novoIdLocal()
        atual.update { it.copy(codigo = codigo, convite = null, aceitouConsentimento = false, cadastro = false, senha = "") }
        executar {
            val convite = repositorio.consultar(codigo)
            atual.value.copy(convite = convite)
        }
    }
    fun sessaoMudou(valor: UsuarioResposta?) {
        if (usuario?.id != valor?.id || usuario?.perfil != valor?.perfil) {
            chave = novoIdLocal()
            atual.update { it.copy(aceitouConsentimento = false, aceitouTermosUso = false, senha = "") }
        }
        usuario = valor
        if (valor != null && atual.value.aguardandoLogin) atual.update { it.copy(visivel = true, aguardandoLogin = false, cadastro = false) }
    }
    fun aceitarConsentimento(valor: Boolean) {
        if (!atual.value.carregando) atual.update { it.copy(aceitouConsentimento = valor && it.convite != null, erro = null) }
    }
    fun aguardarLogin() {
        if (atual.value.aceitando || atual.value.convite == null) return
        cancelarConsulta()
        atual.update { it.copy(visivel = false, aguardandoLogin = true, aceitouConsentimento = false,
            aceitouTermosUso = false, senha = "", erro = null, carregando = false) }
    }
    fun abrirCadastro() {
        if (usuario == null && !atual.value.carregando && atual.value.convite != null) {
            atual.update { it.copy(cadastro = true, erro = null) }
            atualizarTermos()
        }
    }
    fun atualizarTermos() {
        if (usuario != null || !atual.value.cadastro || atual.value.carregando) return
        chave = novoIdLocal()
        atual.update { it.copy(termosUso = null, aceitouTermosUso = false) }
        executar {
            val termos = repositorio.consultarTermosUso()
            atual.value.copy(termosUso = termos, aceitouTermosUso = false)
        }
    }
    fun aceitarTermosUso(valor: Boolean) {
        if (!atual.value.carregando) atual.update { it.copy(aceitouTermosUso = valor && it.termosUso?.permiteCadastro() == true, erro = null) }
    }
    fun mudarCampo(campo: CampoCadastroPaciente, valor: String) {
        if (atual.value.carregando) return
        chave = novoIdLocal()
        atual.update { when (campo) {
            CampoCadastroPaciente.NOME -> it.copy(nome = valor)
            CampoCadastroPaciente.EMAIL -> it.copy(email = valor)
            CampoCadastroPaciente.SENHA -> it.copy(senha = valor)
        }.copy(erro = null, errosCampos = emptyMap()) }
    }
    fun aceitar() {
        val entrada = atual.value
        val convite = entrada.convite ?: return
        if (entrada.carregando) return
        if (!entrada.aceitouConsentimento) { atual.update { it.copy(erro = MensagemConvite.ACEITE_OBRIGATORIO) }; return }
        val cadastro = if (usuario == null) {
            val termos = entrada.termosUso?.takeIf { it.permiteCadastro() }
            if (termos == null) { atual.update { it.copy(erro = MensagemConvite.TERMOS_INDISPONIVEIS) }; return }
            if (!entrada.aceitouTermosUso) { atual.update { it.copy(erro = MensagemConvite.ACEITE_TERMOS_OBRIGATORIO) }; return }
            val campos = mutableMapOf<String, MensagemConvite>()
            if (entrada.nome.isBlank()) campos["nome"] = MensagemConvite.OBRIGATORIO
            if (entrada.email.isBlank()) campos["email"] = MensagemConvite.OBRIGATORIO
            else if (!Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+").matches(entrada.email.trim())) campos["email"] = MensagemConvite.EMAIL_INVALIDO
            if (entrada.senha.length < 10) campos["senha"] = MensagemConvite.SENHA_CURTA
            atual.update { it.copy(errosCampos = campos) }
            if (campos.isNotEmpty()) return
            CadastroPacienteRequisicao(entrada.nome.trim(), entrada.email.trim(), entrada.senha, idFusoDoAparelho(),
                aceitouTermos = true, versaoTermos = termos.versao)
        } else null
        val chaveEnvio = chave
        executar(aceitando = true) {
            repositorio.aceitar(entrada.codigo, convite.termo.versao, cadastro, chaveEnvio)
            EstadoConvite(concluido = true)
        }
    }
    fun fechar() {
        if (atual.value.aceitando) return
        cancelarConsulta()
        atual.value = EstadoConvite()
    }
    fun consumirConclusao() { atual.update { it.copy(concluido = false) } }
    fun retomar() {
        if (atual.value.aguardandoLogin) atual.update { it.copy(visivel = true, aguardandoLogin = false) }
        else abrir()
    }
    private fun cancelarConsulta() { geracao++; operacao?.cancel() }
    private fun executar(aceitando: Boolean = false, bloco: suspend () -> EstadoConvite) {
        val esperada = geracao
        atual.update { it.copy(carregando = true, aceitando = aceitando, erro = null, errosCampos = emptyMap()) }
        operacao = viewModelScope.launch(dispatcher) {
            try { val resposta = bloco(); if (esperada == geracao) atual.value = resposta }
            catch (cancelamento: CancellationException) { throw cancelamento }
            catch (falha: Exception) { if (esperada == geracao) apresentar(falha) }
            finally { if (esperada == geracao) atual.update { it.copy(carregando = false, aceitando = false) } }
        }
    }
    private fun apresentar(falha: Exception) {
        val mensagem = when (falha) {
            is FalhaComunicacao -> MensagemConvite.SEM_CONEXAO
            is ArmazenamentoSeguroIndisponivel -> MensagemConvite.COFRE_INDISPONIVEL
            is ErroApi -> when (falha.codigo) {
                "CONVITE_EXPIRADO" -> MensagemConvite.EXPIRADO
                "CONVITE_INVALIDO", "RECURSO_NAO_ENCONTRADO" -> MensagemConvite.INVALIDO
                "CONSENTIMENTO_OBRIGATORIO" -> MensagemConvite.CONSENTIMENTO_INDISPONIVEL
                "TERMOS_INDISPONIVEIS", "TERMOS_NAO_ACEITOS" -> MensagemConvite.TERMOS_INDISPONIVEIS
                "TAXA_EXCEDIDA" -> MensagemConvite.TAXA_EXCEDIDA
                "CREDENCIAIS_INVALIDAS", "NAO_AUTENTICADO", "EMAIL_JA_CADASTRADO" -> MensagemConvite.ENTRAR_NECESSARIO
                "VINCULO_JA_EXISTE" -> MensagemConvite.VINCULO_EXISTENTE
                else -> MensagemConvite.ERRO_PADRAO
            }
            else -> MensagemConvite.ERRO_PADRAO
        }
        atual.update { it.copy(erro = mensagem,
            termosUso = if (mensagem == MensagemConvite.TERMOS_INDISPONIVEIS) null else it.termosUso,
            aceitouTermosUso = it.aceitouTermosUso && mensagem != MensagemConvite.TERMOS_INDISPONIVEIS,
            aceitouConsentimento = it.aceitouConsentimento && mensagem != MensagemConvite.CONSENTIMENTO_INDISPONIVEL,
            errosCampos = (falha as? ErroApi)?.campos?.associate { it.campo.removePrefix("cadastro.") to MensagemConvite.CAMPO_INVALIDO }.orEmpty()) }
    }
}
