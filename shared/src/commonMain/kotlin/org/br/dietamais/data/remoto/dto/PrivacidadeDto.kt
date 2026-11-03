package org.br.dietamais.data.remoto.dto

import kotlinx.serialization.Serializable
import kotlin.time.Instant
import kotlin.uuid.Uuid

/** `status` fica texto livre (PENDENTE, PRONTA, FALHOU, EXPIRADA): o backend pode acrescentar valores. */
@Serializable
data class ExportacaoResposta(val id: Uuid, val status: String, val criadoEm: Instant, val url: String? = null,
    val prontaEm: Instant? = null, val expiraEm: Instant? = null) {
    override fun toString() = "ExportacaoResposta[protegido]"
}

@Serializable
data class ExcluirContaRequisicao(val senha: String) {
    override fun toString() = "ExcluirContaRequisicao[conteúdo protegido]"
}
