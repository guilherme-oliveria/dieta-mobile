package org.br.dietamais.data.remoto.dto

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Serializable
data class PacienteResposta(val id: Uuid, val vinculoId: Uuid, val nome: String, val email: String? = null,
    val dataNascimento: LocalDate? = null, val sexo: Sexo? = null, val telefone: String? = null,
    val estaturaReferencia: Double? = null, val objetivo: String? = null, val observacoes: String? = null,
    val status: StatusVinculo, val versao: Long, val criadoEm: Instant, val atualizadoEm: Instant, val adesao7d: Double? = null)

@Serializable
data class CriarPacienteRequisicao(val id: Uuid, val nome: String, val email: String? = null,
    val dataNascimento: LocalDate? = null, val sexo: Sexo? = null, val telefone: String? = null,
    val estaturaReferencia: Double? = null, val objetivo: String? = null, val observacoes: String? = null)

@Serializable
data class AtualizarPacienteRequisicao(val nome: String, val email: String? = null,
    val dataNascimento: LocalDate? = null, val sexo: Sexo? = null, val telefone: String? = null,
    val estaturaReferencia: Double? = null, val objetivo: String? = null, val observacoes: String? = null)

@Serializable
data class PaginaPacientesResposta(val itens: List<PacienteResposta>, val pagina: Int, val tamanho: Int, val total: Long)

@Serializable
data class NutricionistaResumoResposta(val id: Uuid, val nome: String, val crn: String)

@Serializable
data class VinculoResposta(val id: Uuid, val pacienteId: Uuid, val nutricionista: NutricionistaResumoResposta,
    val status: StatusVinculo, val consentimentoEm: Instant? = null, val encerradoEm: Instant? = null,
    val versao: Long, val atualizadoEm: Instant)

@Serializable
data class ConviteCriadoResposta(val codigo: String, val link: String, val expiraEm: Instant)

@Serializable
data class TermoConsentimentoResposta(val versao: String, val texto: String)

@Serializable
data class ConviteResposta(val nutricionista: NutricionistaResumoResposta, val termo: TermoConsentimentoResposta, val expiraEm: Instant)

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class CadastroPacienteRequisicao(val nome: String, val email: String, val senha: String, val fuso: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val aceitouTermos: Boolean? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val versaoTermos: String? = null) {
    override fun toString() = "CadastroPacienteRequisicao[conteúdo protegido]"
}

@Serializable
data class AceitarConviteRequisicao(val aceitouTermo: Boolean, val versaoTermo: String,
    val cadastro: CadastroPacienteRequisicao? = null, val dispositivo: DispositivoRequisicao) {
    override fun toString() = "AceitarConviteRequisicao[conteúdo protegido]"
}

@Serializable
data class AceiteConviteResposta(val sessao: SessaoResposta, val vinculo: VinculoResposta) {
    override fun toString() = "AceiteConviteResposta[conteúdo protegido]"
}
