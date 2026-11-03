package org.br.dietamais.data.remoto.dto

import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

@Serializable(with = TipoNotificacaoSerializer::class)
enum class TipoNotificacao { NOVA_DIETA, QUESTIONARIO_ENVIADO, CONSULTA_AMANHA, QUESTIONARIO_RESPONDIDO, PACIENTE_SEM_CHECKIN, ASSINATURA, OUTRO }
object TipoNotificacaoSerializer : EnumComOutro<TipoNotificacao>("TipoNotificacao", TipoNotificacao.entries.toTypedArray(), TipoNotificacao.OUTRO)

@Serializable
data class PreferenciaTipo(val tipo: TipoNotificacao, val ativo: Boolean)
@Serializable
data class SilencioNotificacao(val ativo: Boolean, val inicio: LocalTime, val fim: LocalTime)
@Serializable
data class PreferenciasNotificacaoResposta(val tipos: List<PreferenciaTipo>, val silencio: SilencioNotificacao, val versao: Long)
@Serializable
data class AtualizarPreferenciasRequisicao(val tipos: List<PreferenciaTipo>, val silencio: SilencioNotificacao)

@Serializable
data class RegistrarDispositivoRequisicao(val token: String, val plataforma: Plataforma, val nomeAparelho: String? = null)
@Serializable
data class DispositivoResposta(val id: kotlin.uuid.Uuid, val plataforma: Plataforma, val nomeAparelho: String? = null)

@Serializable
data class CalendarioTokenResposta(val token: String, val url: String, val urlWebcal: String) {
    // o token é a credencial do feed: não aparece em log nem em toString
    override fun toString() = "CalendarioTokenResposta[protegido]"
}
