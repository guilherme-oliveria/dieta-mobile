package org.br.dietamais.data.remoto.dto

import kotlinx.serialization.Serializable
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Serializable
data class DispositivoRequisicao(val id: Uuid, val plataforma: Plataforma, val nomeAparelho: String)

@Serializable
data class UsuarioResposta(
    val id: Uuid, val nome: String, val email: String, val emailConfirmado: Boolean,
    val perfis: List<Perfil>, val perfil: Perfil? = null, val nutricionistaId: Uuid? = null,
    val pacienteId: Uuid? = null, val fuso: String,
)

@Serializable
data class SessaoResposta(val accessToken: String? = null, val refreshToken: String? = null,
    val expiraEm: Instant? = null, val usuario: UsuarioResposta) {
    override fun toString() = "SessaoResposta[conteúdo protegido]"
}

@Serializable
data class LoginRequisicao(val email: String, val senha: String, val dispositivo: DispositivoRequisicao, val perfil: Perfil? = null) {
    override fun toString() = "LoginRequisicao[conteúdo protegido]"
}

@Serializable
data class RefreshRequisicao(val refreshToken: String) {
    override fun toString() = "RefreshRequisicao[conteúdo protegido]"
}

@Serializable
data class LogoutRequisicao(val refreshToken: String) {
    override fun toString() = "LogoutRequisicao[conteúdo protegido]"
}

@Serializable
data class CadastroNutriRequisicao(val nome: String, val email: String, val senha: String, val crn: String,
    val aceitouTermos: Boolean, val versaoTermos: String, val fuso: String? = null) {
    override fun toString() = "CadastroNutriRequisicao[conteúdo protegido]"
}

@Serializable
data class CadastroNutriResposta(val usuario: UsuarioResposta, val assinatura: AssinaturaResumoResposta)

@Serializable
data class AssinaturaResumoResposta(val organizacaoId: Uuid, val plano: String, val status: StatusAssinatura,
    val limitePacientes: Int, val pacientesOcupados: Int, val fimPeriodoEm: Instant)

@Serializable
data class EuResposta(val usuario: UsuarioResposta, val assinatura: AssinaturaResumoResposta? = null, val vinculos: List<VinculoResposta>)

@Serializable
data class ConfirmarEmailRequisicao(val token: String) {
    override fun toString() = "ConfirmarEmailRequisicao[conteúdo protegido]"
}

@Serializable
data class EsqueciSenhaRequisicao(val email: String)

@Serializable
data class RedefinirSenhaRequisicao(val token: String, val novaSenha: String) {
    override fun toString() = "RedefinirSenhaRequisicao[conteúdo protegido]"
}
