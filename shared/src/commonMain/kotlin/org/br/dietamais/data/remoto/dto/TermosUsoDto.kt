package org.br.dietamais.data.remoto.dto

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class TermosUsoResposta(val versao: String, val status: String, val aprovado: Boolean, val publicado: Boolean,
    val cadastroHabilitado: Boolean, val aprovadoEm: Instant? = null, val publicadoEm: Instant? = null,
    val conteudo: String, val url: String)
