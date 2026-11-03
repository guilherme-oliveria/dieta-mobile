package org.br.dietamais.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.PortaAutenticacao
import org.br.dietamais.plataforma.ArmazenamentoSeguroIndisponivel
import org.br.dietamais.data.sync.novoIdLocal

class AutenticacaoViewModel(
    private val repositorio: PortaAutenticacao,
    val termos: TermosCadastro? = null,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : ViewModel() {
    private val atual = MutableStateFlow(EstadoAutenticacao())
    val estado = atual.asStateFlow()
    private var operacao: Job? = null
    private var geracao = 0L
    private var chaveCadastro = novoIdLocal()

    init {
        viewModelScope.launch(dispatcher) {
            try {
                val usuario = repositorio.restaurar()
                atual.update { it.copy(usuario = usuario, restaurando = false) }
                if (usuario != null) atualizarContexto()
            } catch (cancelamento: CancellationException) { throw cancelamento }
            catch (falha: Exception) { apresentar(falha) }
            finally { atual.update { it.copy(restaurando = false) } }
            repositorio.sessao.collect { sessao ->
                if (sessao == null && atual.value.usuario != null) atual.value = EstadoAutenticacao(restaurando = false)
            }
        }
    }

    fun mudarEmail(valor: String) = editar("email") { copy(email = valor) }
    fun mudarSenha(valor: String) = editar("senha") { copy(senha = valor) }
    fun mudarNome(valor: String) = editar("nome") { copy(nome = valor) }
    fun mudarCrn(valor: String) = editar("crn") { copy(crn = valor) }
    fun mudarToken(valor: String) = editar("token") { copy(tokenRecuperacao = valor) }
    fun aceitarTermos(valor: Boolean) = editar("aceitouTermos") { copy(aceitouTermos = valor && termos != null) }

    fun abrirCadastro() = abrir(TelaAutenticacao.CADASTRO)
    fun abrirRecuperacao() = abrir(TelaAutenticacao.RECUPERAR_SENHA)
    private fun abrir(tela: TelaAutenticacao) {
        if (atual.value.carregando) return
        if (tela == TelaAutenticacao.CADASTRO) chaveCadastro = novoIdLocal()
        atual.update { EstadoAutenticacao(tela = tela, email = it.email, restaurando = false) }
    }

    fun voltar() {
        geracao++
        operacao?.cancel()
        atual.update { EstadoAutenticacao(email = it.email, restaurando = false, carregando = true) }
        // Incrementa também a geração da sessão de MOB-05, recusando login que já estava em voo.
        operacao = viewModelScope.launch(dispatcher) {
            try { repositorio.sair() }
            catch (cancelamento: CancellationException) { throw cancelamento }
            catch (_: Exception) { /* O cliente garante limpeza local mesmo sem revogação remota. */ }
            finally { atual.update { it.copy(carregando = false) } }
        }
    }

    fun entrar() = autenticar(null)
    fun escolherPerfil(perfil: Perfil) {
        if (perfil !in atual.value.perfis || perfil !in PERFIS_SUPORTADOS) return
        autenticar(perfil)
    }

    private fun autenticar(perfil: Perfil?) {
        if (!validarEntrada()) return
        executar {
            val entrada = atual.value
            val resposta = repositorio.entrar(entrada.email.trim(), entrada.senha, perfil)
            val perfis = resposta.usuario.perfis.filter { it in PERFIS_SUPORTADOS }.distinct()
            if (resposta.usuario.perfil == null) {
                if (perfis.isEmpty()) throw RespostaInvalida()
                atual.update { it.copy(tela = TelaAutenticacao.ESCOLHER_PERFIL, perfis = perfis, erro = null) }
            } else {
                if (resposta.usuario.perfil !in perfis) throw RespostaInvalida()
                atual.update { EstadoAutenticacao(usuario = resposta.usuario, restaurando = false, carregando = true) }
                atualizarContexto()
            }
        }
    }

    fun cadastrar() {
        val textoTermos = termos ?: run { atual.update { it.copy(erro = MensagemAutenticacao.TERMOS_INDISPONIVEIS) }; return }
        val campos = errosEntrada().toMutableMap()
        with(atual.value) {
            if (nome.isBlank()) campos["nome"] = MensagemAutenticacao.OBRIGATORIO
            if (!Regex("CRN-[1-9][0-9]? [0-9]+").matches(crn.trim())) campos["crn"] = MensagemAutenticacao.CRN_INVALIDO
            if (senha.length < 10) campos["senha"] = MensagemAutenticacao.SENHA_CURTA
            if (!aceitouTermos) campos["aceitouTermos"] = MensagemAutenticacao.ACEITE_OBRIGATORIO
        }
        if (!validar(campos)) return
        executar {
            val entrada = atual.value
            repositorio.cadastrar(CadastroNutriRequisicao(entrada.nome.trim(), entrada.email.trim(), entrada.senha,
                entrada.crn.trim(), true, textoTermos.versao), chaveCadastro)
            atual.value = EstadoAutenticacao(email = entrada.email.trim(), aviso = MensagemAutenticacao.CADASTRO_CONCLUIDO, restaurando = false)
        }
    }

    fun recuperarSenha() {
        if (!validar(errosEmail())) return
        executar {
            repositorio.recuperar(atual.value.email.trim())
            atual.update { it.copy(aviso = MensagemAutenticacao.RECUPERACAO_ENVIADA, senha = "", tokenRecuperacao = "") }
        }
    }

    fun redefinirSenha() {
        val campos = mutableMapOf<String, MensagemAutenticacao>()
        if (atual.value.tokenRecuperacao.isBlank()) campos["token"] = MensagemAutenticacao.OBRIGATORIO
        if (atual.value.senha.length < 10) campos["senha"] = MensagemAutenticacao.SENHA_CURTA
        if (!validar(campos)) return
        executar {
            repositorio.redefinir(atual.value.tokenRecuperacao.trim(), atual.value.senha)
            atual.update { EstadoAutenticacao(email = it.email, restaurando = false, aviso = MensagemAutenticacao.SENHA_REDEFINIDA) }
        }
    }

    fun sair() {
        if (atual.value.carregando) return
        executar { try { repositorio.sair() } finally { atual.value = EstadoAutenticacao(restaurando = false) } }
    }

    fun recarregarSessaoDoConvite() = executar {
        val usuario = repositorio.restaurar() ?: throw RespostaInvalida()
        if (usuario.perfil != Perfil.PACIENTE) throw RespostaInvalida()
        atual.value = EstadoAutenticacao(usuario = usuario, restaurando = false, carregando = true)
        atualizarContexto()
    }

    private suspend fun atualizarContexto() {
        try {
            val eu = repositorio.consultarEu()
            if (eu.usuario.id != atual.value.usuario?.id || eu.usuario.perfil != atual.value.usuario?.perfil) {
                try { repositorio.sair() }
                finally { atual.value = EstadoAutenticacao(restaurando = false) }
                throw RespostaInvalida()
            }
            atual.update { it.copy(usuario = eu.usuario, assinatura = eu.assinatura) }
        } catch (cancelamento: CancellationException) { throw cancelamento }
        catch (_: FalhaComunicacao) { /* O contexto persistido permite abrir o shell offline. */ }
    }

    private fun executar(bloco: suspend () -> Unit) {
        if (atual.value.carregando || atual.value.restaurando) return
        val esperada = geracao
        atual.update { it.copy(carregando = true, erro = null, aviso = null, errosCampos = emptyMap()) }
        operacao = viewModelScope.launch(dispatcher) {
            try { bloco() }
            catch (cancelamento: CancellationException) { throw cancelamento }
            catch (falha: Exception) { if (esperada == geracao) apresentar(falha) }
            finally { if (esperada == geracao) atual.update { it.copy(carregando = false) } }
        }
    }

    private fun apresentar(falha: Exception) {
        val mensagem = when (falha) {
            is FalhaComunicacao -> MensagemAutenticacao.SEM_CONEXAO
            is ArmazenamentoSeguroIndisponivel -> MensagemAutenticacao.COFRE_INDISPONIVEL
            is RespostaInvalida, is SessaoAlterada -> MensagemAutenticacao.RESPOSTA_INVALIDA
            is ErroApi -> when (falha.codigo) {
                "CREDENCIAIS_INVALIDAS" -> MensagemAutenticacao.CREDENCIAIS_INVALIDAS
                "LOGIN_BLOQUEADO" -> MensagemAutenticacao.LOGIN_BLOQUEADO
                "TAXA_EXCEDIDA" -> MensagemAutenticacao.TAXA_EXCEDIDA
                "EMAIL_JA_CADASTRADO" -> MensagemAutenticacao.EMAIL_JA_CADASTRADO
                "PERFIL_INVALIDO" -> MensagemAutenticacao.PERFIL_INVALIDO
                "TOKEN_INVALIDO", "TOKEN_EXPIRADO" -> MensagemAutenticacao.TOKEN_INVALIDO
                else -> MensagemAutenticacao.ERRO_INESPERADO
            }
            else -> MensagemAutenticacao.ERRO_INESPERADO
        }
        val campos = (falha as? ErroApi)?.campos?.associate { it.campo to when (it.codigo) {
            "OBRIGATORIO" -> MensagemAutenticacao.OBRIGATORIO
            else -> MensagemAutenticacao.CAMPO_INVALIDO
        } }.orEmpty()
        atual.update { it.copy(erro = mensagem, errosCampos = campos) }
    }

    private fun editar(campo: String, mudar: EstadoAutenticacao.() -> EstadoAutenticacao) {
        if (!atual.value.carregando) atual.update {
            val novo = it.mudar()
            if (it.tela == TelaAutenticacao.CADASTRO && novo != it) chaveCadastro = novoIdLocal()
            novo.copy(erro = null, aviso = null, errosCampos = it.errosCampos - campo)
        }
    }
    private fun errosEmail() = when {
        atual.value.email.isBlank() -> mapOf("email" to MensagemAutenticacao.OBRIGATORIO)
        !Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+").matches(atual.value.email.trim()) -> mapOf("email" to MensagemAutenticacao.EMAIL_INVALIDO)
        else -> emptyMap()
    }
    private fun errosEntrada() = errosEmail() + if (atual.value.senha.isEmpty()) mapOf("senha" to MensagemAutenticacao.OBRIGATORIO) else emptyMap()
    private fun validarEntrada() = validar(errosEntrada())
    private fun validar(campos: Map<String, MensagemAutenticacao>): Boolean {
        if (atual.value.carregando || atual.value.restaurando) return false
        atual.update { it.copy(errosCampos = campos, erro = null) }
        return campos.isEmpty()
    }
    private companion object { val PERFIS_SUPORTADOS = setOf(Perfil.NUTRICIONISTA, Perfil.PACIENTE) }
}
