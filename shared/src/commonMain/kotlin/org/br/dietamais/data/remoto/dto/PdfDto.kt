package org.br.dietamais.data.remoto.dto

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class PdfResposta(val url: String, val expiraEm: Instant? = null) {
    override fun toString() = "PdfResposta[protegido]"
}
