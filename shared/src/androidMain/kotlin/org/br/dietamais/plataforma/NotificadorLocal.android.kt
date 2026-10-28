package org.br.dietamais.plataforma

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import org.br.dietamais.dominio.rotina.Lembrete
import org.json.JSONArray
import org.json.JSONObject

private const val ACAO = "org.br.dietamais.LEMBRETE"
private const val CANAL = "lembretes_refeicao"
private const val PREFS = "dietamais_lembretes"
private const val CHAVE_LISTA = "lista"

/**
 * Lembretes por AlarmManager (inexato, sem permissão de alarme exato). A lista agendada fica em SharedPreferences
 * para o [ReinicioLembretesReceiver] reagendar depois de reiniciar o aparelho. Nada de dado clínico no texto.
 */
actual class NotificadorLocal(private val contexto: Context, private val pedirNoSistema: suspend () -> Boolean = { false }) : NotificacaoLocal {
    private val app = contexto.applicationContext

    actual override suspend fun permitido(): Boolean = app.getSystemService(NotificationManager::class.java)?.areNotificationsEnabled() ?: false

    actual override suspend fun pedirPermissao(): Boolean = permitido() || (Build.VERSION.SDK_INT >= 33 && pedirNoSistema())

    actual override suspend fun agendar(lembretes: List<Lembrete>, titulo: String, corpo: (String) -> String) {
        cancelarTudo()
        val lista = JSONArray()
        lembretes.forEach { l ->
            val texto = corpo(l.refeicao); val quando = l.quando.toEpochMilliseconds()
            AgendaLembretes.agendar(app, l.chave, quando, titulo, texto)
            lista.put(JSONObject().put("c", l.chave).put("q", quando).put("t", titulo).put("b", texto))
        }
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(CHAVE_LISTA, lista.toString()).apply()
    }

    actual override suspend fun cancelarTudo() {
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        AgendaLembretes.lerLista(app).forEach { AgendaLembretes.cancelar(app, it.getString("c")) }
        prefs.edit().remove(CHAVE_LISTA).apply()
    }
}

internal object AgendaLembretes {
    private fun intencao(c: Context, chave: String, titulo: String? = null, corpo: String? = null) =
        PendingIntent.getBroadcast(c, chave.hashCode(),
            Intent(c, LembreteReceiver::class.java).setAction(ACAO).putExtra("chave", chave).putExtra("titulo", titulo).putExtra("corpo", corpo),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    fun agendar(c: Context, chave: String, quando: Long, titulo: String, corpo: String) {
        if (quando <= System.currentTimeMillis()) return
        c.getSystemService(AlarmManager::class.java)?.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, quando, intencao(c, chave, titulo, corpo))
    }

    fun cancelar(c: Context, chave: String) {
        val pi = intencao(c, chave)
        c.getSystemService(AlarmManager::class.java)?.cancel(pi); pi.cancel()
    }

    fun lerLista(c: Context): List<JSONObject> = try {
        val json = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(CHAVE_LISTA, null) ?: "[]"
        JSONArray(json).let { a -> (0 until a.length()).map { a.getJSONObject(it) } }
    } catch (_: Exception) { emptyList() }
}

class LembreteReceiver : BroadcastReceiver() {
    override fun onReceive(contexto: Context, intent: Intent) {
        val titulo = intent.getStringExtra("titulo") ?: return
        val corpo = intent.getStringExtra("corpo") ?: return
        val chave = intent.getStringExtra("chave") ?: return
        val gerenciador = contexto.getSystemService(NotificationManager::class.java) ?: return
        if (!gerenciador.areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= 26) gerenciador.createNotificationChannel(NotificationChannel(CANAL, "Lembretes de refeição", NotificationManager.IMPORTANCE_DEFAULT))
        val abrir = contexto.packageManager.getLaunchIntentForPackage(contexto.packageName)
            ?.let { PendingIntent.getActivity(contexto, 0, it, PendingIntent.FLAG_IMMUTABLE) }
        val construtor = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(contexto, CANAL) else @Suppress("DEPRECATION") Notification.Builder(contexto)
        gerenciador.notify(chave.hashCode(), construtor.setSmallIcon(contexto.applicationInfo.icon).setContentTitle(titulo).setContentText(corpo)
            .setContentIntent(abrir).setAutoCancel(true).build())
    }
}

/** Depois de reiniciar o aparelho os alarmes somem: reagenda o que ainda está no futuro. */
class ReinicioLembretesReceiver : BroadcastReceiver() {
    override fun onReceive(contexto: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        AgendaLembretes.lerLista(contexto).forEach { AgendaLembretes.agendar(contexto, it.getString("c"), it.getLong("q"), it.getString("t"), it.getString("b")) }
    }
}
