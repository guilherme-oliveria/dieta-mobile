package org.br.dietamais.ui.auth

import org.br.dietamais.data.remoto.dto.*

data class TermosCadastro(val versao: String, val texto: String) {
    init { require(versao.isNotBlank() && versao.length <= 40 && texto.isNotBlank()) }
}

enum class TelaAutenticacao { ENTRAR, CADASTRO, RECUPERAR_SENHA, ESCOLHER_PERFIL }
enum class MensagemAutenticacao {
    OBRIGATORIO, EMAIL_INVALIDO, SENHA_CURTA, CRN_INVALIDO, ACEITE_OBRIGATORIO, TERMOS_INDISPONIVEIS,
    CREDENCIAIS_INVALIDAS, LOGIN_BLOQUEADO, TAXA_EXCEDIDA, EMAIL_JA_CADASTRADO, PERFIL_INVALIDO,
    TOKEN_INVALIDO, CAMPO_INVALIDO, SEM_CONEXAO, COFRE_INDISPONIVEL, RESPOSTA_INVALIDA, ERRO_INESPERADO,
    CADASTRO_CONCLUIDO, RECUPERACAO_ENVIADA, SENHA_REDEFINIDA,
}

data class EstadoAutenticacao(
    val tela: TelaAutenticacao = TelaAutenticacao.ENTRAR,
    val email: String = "", val senha: String = "", val nome: String = "", val crn: String = "",
    val tokenRecuperacao: String = "", val aceitouTermos: Boolean = false,
    val carregando: Boolean = false, val restaurando: Boolean = true,
    val erro: MensagemAutenticacao? = null, val aviso: MensagemAutenticacao? = null,
    val errosCampos: Map<String, MensagemAutenticacao> = emptyMap(),
    val perfis: List<Perfil> = emptyList(), val usuario: UsuarioResposta? = null,
    val assinatura: AssinaturaResumoResposta? = null,
) {
    override fun toString() = "EstadoAutenticacao[tela=$tela, carregando=$carregando]"
}
