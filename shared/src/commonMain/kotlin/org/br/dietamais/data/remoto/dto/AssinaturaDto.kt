package org.br.dietamais.data.remoto.dto

import kotlinx.serialization.Serializable
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Serializable
data class PlanoResposta(val codigo: String, val nome: String, val precoMensal: Double = 0.0, val limitePacientes: Int)

@Serializable
data class AssinaturaResposta(val organizacaoId: Uuid, val plano: PlanoResposta, val status: StatusAssinatura, val pacientesOcupados: Int,
    val fimPeriodoEm: Instant? = null, val carenciaAte: Instant? = null, val pagamentoDisponivel: Boolean = false,
    val planosDisponiveis: List<PlanoResposta> = emptyList())

@Serializable
data class CheckoutRequisicao(val plano: String)

@Serializable
data class CheckoutResposta(val url: String) {
    override fun toString() = "CheckoutResposta[protegido]"
}
