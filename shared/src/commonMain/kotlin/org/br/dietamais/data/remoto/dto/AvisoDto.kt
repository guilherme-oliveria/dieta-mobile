package org.br.dietamais.data.remoto.dto

import kotlinx.serialization.Serializable
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Serializable(with = TipoAvisoSerializer::class)
enum class TipoAviso { NOVA_DIETA, QUESTIONARIO_ENVIADO, CONSULTA_AMANHA, QUESTIONARIO_RESPONDIDO, PACIENTE_SEM_CHECKIN, ASSINATURA, OUTRO }
object TipoAvisoSerializer : EnumComOutro<TipoAviso>("TipoAviso", TipoAviso.entries.toTypedArray(), TipoAviso.OUTRO)

/** Título e corpo vêm do servidor, sem dado clínico (RN-NOT-02). [rota] é o destino da navegação, quando houver. */
@Serializable
data class AvisoResposta(val id: Uuid, val tipo: TipoAviso, val titulo: String, val corpo: String, val rota: String? = null,
    val criadoEm: Instant, val lidoEm: Instant? = null, val versao: Long) {
    val lido: Boolean get() = lidoEm != null
}

@Serializable
data class AvisosResposta(val itens: List<AvisoResposta> = emptyList(), val pagina: Int = 0, val tamanho: Int = 20, val total: Long = 0, val naoLidos: Long = 0)
