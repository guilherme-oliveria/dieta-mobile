@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package org.br.dietamais.plataforma

import kotlinx.coroutines.suspendCancellableCoroutine
import org.br.dietamais.dominio.rotina.Lembrete
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

    actual override suspend fun agendar(lembretes: List<Lembrete>, titulo: String, corpo: (String) -> String) {
        centro.removeAllPendingNotificationRequests()
        val calendario = NSCalendar.currentCalendar
        val unidades = NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay or NSCalendarUnitHour or NSCalendarUnitMinute
        lembretes.forEach { l ->
            val conteudo = UNMutableNotificationContent().apply {
                setTitle(titulo); setBody(corpo(l.refeicao)); setSound(UNNotificationSound.defaultSound())
            }
            val data = NSDate.dateWithTimeIntervalSince1970(l.quando.toEpochMilliseconds() / 1000.0)
            val gatilho = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(calendario.components(unidades, fromDate = data), repeats = false)
            centro.addNotificationRequest(UNNotificationRequest.requestWithIdentifier(l.chave, conteudo, gatilho), withCompletionHandler = null)
        }
    }

    actual override suspend fun cancelarTudo() { centro.removeAllPendingNotificationRequests() }
}
