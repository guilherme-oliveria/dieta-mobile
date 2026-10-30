@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package org.br.dietamais.plataforma

import kotlinx.coroutines.suspendCancellableCoroutine
import org.br.dietamais.dominio.rotina.Lembrete
import org.br.dietamais.dominio.rotina.TextosLembrete
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationAction
import platform.UserNotifications.UNNotificationActionOptionForeground
import platform.UserNotifications.UNNotificationActionOptionNone
import platform.UserNotifications.UNNotificationCategory
import platform.UserNotifications.UNNotificationCategoryOptionNone
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

/** UNUserNotificationCenter com gatilhos de calendário (até 64 pendentes). Texto sem dado clínico. */
actual class NotificadorLocal : NotificacaoLocal {
    private val centro get() = UNUserNotificationCenter.currentNotificationCenter()

    actual override suspend fun permitido(): Boolean = suspendCancellableCoroutine { c ->
        centro.getNotificationSettingsWithCompletionHandler { s ->
            val status = s?.authorizationStatus
            c.resume(status == UNAuthorizationStatusAuthorized || status == UNAuthorizationStatusProvisional)
        }
    }

    actual override suspend fun pedirPermissao(): Boolean = suspendCancellableCoroutine { c ->
        centro.requestAuthorizationWithOptions(UNAuthorizationOptionAlert or UNAuthorizationOptionSound) { concedido, _ -> c.resume(concedido) }
    }

    actual override suspend fun agendar(lembretes: List<Lembrete>, textos: TextosLembrete) {
        registrarCategoria(textos)
        centro.removeAllPendingNotificationRequests()
        val calendario = NSCalendar.currentCalendar
        val unidades = NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay or NSCalendarUnitHour or NSCalendarUnitMinute
        lembretes.forEach { l ->
            val conteudo = UNMutableNotificationContent().apply {
                setTitle(textos.titulo); setBody(textos.corpo(l.refeicao)); setSound(UNNotificationSound.defaultSound())
                if (l.dietaId != null && l.dia != null) {
                    // Só ids e o dia, sem dado clínico: os botões Comi e Pulei registram o check-in (MOB-18b).
                    setCategoryIdentifier(CATEGORIA)
                    setUserInfo(mapOf("dieta" to l.dietaId.toString(), "refeicao" to l.refeicaoId.toString(), "dia" to l.dia.toString()))
                }
            }
            val data = NSDate.dateWithTimeIntervalSince1970(l.quando.toEpochMilliseconds() / 1000.0)
            val gatilho = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(calendario.components(unidades, fromDate = data), repeats = false)
            centro.addNotificationRequest(UNNotificationRequest.requestWithIdentifier(l.chave, conteudo, gatilho), withCompletionHandler = null)
        }
    }

    actual override suspend fun cancelarTudo() { centro.removeAllPendingNotificationRequests() }
}

internal const val CATEGORIA = "REFEICAO"
internal const val ACAO_COMI = "COMI"
internal const val ACAO_TROQUEI = "TROQUEI"
internal const val ACAO_PULEI = "PULEI"

/** Botões da notificação de refeição; Comi e Pulei rodam sem abrir o app, Troquei abre (a troca escolhe o alimento). */
private fun registrarCategoria(textos: TextosLembrete) {
    val acoes = listOf(
        UNNotificationAction.actionWithIdentifier(ACAO_COMI, textos.acoes.comi, UNNotificationActionOptionNone),
        UNNotificationAction.actionWithIdentifier(ACAO_TROQUEI, textos.acoes.troquei, UNNotificationActionOptionForeground),
        UNNotificationAction.actionWithIdentifier(ACAO_PULEI, textos.acoes.pulei, UNNotificationActionOptionNone),
    )
    val categoria = UNNotificationCategory.categoryWithIdentifier(CATEGORIA, acoes, emptyList<Any>(), UNNotificationCategoryOptionNone)
    UNUserNotificationCenter.currentNotificationCenter().setNotificationCategories(setOf(categoria))
}
