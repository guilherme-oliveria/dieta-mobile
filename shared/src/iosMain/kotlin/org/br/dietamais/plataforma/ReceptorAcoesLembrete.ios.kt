package org.br.dietamais.plataforma

import kotlinx.datetime.LocalDate
import org.br.dietamais.dominio.rotina.AcaoLembrete
import platform.UserNotifications.UNNotificationResponse
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNUserNotificationCenterDelegateProtocol
import platform.darwin.NSObject
import kotlin.uuid.Uuid

/**
 * Recebe os toques nos botões da notificação (MOB-18b). Segure uma referência forte (o `delegate` do centro é fraco) e chame
 * [instalar] depois de criar o ambiente. Comi e Pulei vêm com os ids no `userInfo` e viram [aoAcao]; Troquei só abre o app.
 */
internal class ReceptorAcoesLembrete(private val aoAcao: (AcaoLembrete, Uuid, Uuid, LocalDate) -> Unit) : NSObject(), UNUserNotificationCenterDelegateProtocol {
    fun instalar() { UNUserNotificationCenter.currentNotificationCenter().delegate = this }

    override fun userNotificationCenter(center: UNUserNotificationCenter, didReceiveNotificationResponse: UNNotificationResponse, withCompletionHandler: () -> Unit) {
        val acao = when (didReceiveNotificationResponse.actionIdentifier) {
            ACAO_COMI -> AcaoLembrete.COMI
            ACAO_PULEI -> AcaoLembrete.PULEI
            else -> null
        }
        val info = didReceiveNotificationResponse.notification.request.content.userInfo
        try {
            if (acao != null) aoAcao(acao, Uuid.parse(info["dieta"] as String), Uuid.parse(info["refeicao"] as String), LocalDate.parse(info["dia"] as String))
        } catch (_: Exception) { /* userInfo ausente ou inválido: ignora */ }
        withCompletionHandler()
    }
}
