package org.br.dietamais

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import org.br.dietamais.plataforma.ReceptorPush

/**
 * Liga o FCM só se as credenciais do projeto Firebase foram dadas no build (propriedades `fcmAppId`, `fcmApiKey`, `fcmProjectId`,
 * `fcmSenderId`). Sem elas nada acontece. O token vai para o código comum, que o registra no servidor (`POST /eu/dispositivos`).
 */
object FirebasePush {
    const val CANAL = "avisos"

    fun iniciar(contexto: Context) {
        criarCanal(contexto)
        val opcoes = opcoes(contexto) ?: return
        try {
            if (FirebaseApp.getApps(contexto).isEmpty()) FirebaseApp.initializeApp(contexto, opcoes)
            FirebaseMessaging.getInstance().token.addOnSuccessListener { ReceptorPush.novoToken(it) }
        } catch (_: Exception) { /* push é melhor esforço: o app segue sem ele */ }
    }

    private fun opcoes(c: Context): FirebaseOptions? {
        val appId = c.getString(R.string.fcm_app_id); val chave = c.getString(R.string.fcm_api_key)
        val projeto = c.getString(R.string.fcm_project_id); val remetente = c.getString(R.string.fcm_sender_id)
        if (listOf(appId, chave, projeto, remetente).any { it.isBlank() }) return null
        return FirebaseOptions.Builder().setApplicationId(appId).setApiKey(chave).setProjectId(projeto).setGcmSenderId(remetente).build()
    }

    private fun criarCanal(c: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        c.getSystemService(NotificationManager::class.java)?.createNotificationChannel(
            NotificationChannel(CANAL, c.getString(R.string.canal_avisos), NotificationManager.IMPORTANCE_DEFAULT))
    }
}
