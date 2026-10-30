package org.br.dietamais.plataforma

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.br.dietamais.data.repositorio.RotinaRepositorio
import org.br.dietamais.data.sync.RegistradorAcaoLembrete
import org.br.dietamais.data.sync.ResultadoAcaoLembrete
import org.br.dietamais.dominio.rotina.AcaoLembrete
import org.br.dietamais.dominio.rotina.Lembrete
import org.br.dietamais.dominio.rotina.TextosLembrete
import org.json.JSONArray
import org.json.JSONObject
import kotlin.uuid.Uuid

private const val ACAO = "org.br.dietamais.LEMBRETE"
private const val ACAO_CHECKIN = "org.br.dietamais.LEMBRETE_CHECKIN"
private const val CANAL = "lembretes_refeicao"
private const val PREFS = "dietamais_lembretes"
private const val CHAVE_LISTA = "lista"

/**
 * Lembretes por AlarmManager (inexato, sem permissão de alarme exato). A lista agendada fica em SharedPreferences
 * para o [ReinicioLembretesReceiver] reagendar depois de reiniciar o aparelho. Nada de dado clínico no texto nem nos extras:
 * só ids (UUID) da dieta e da refeição e o dia, para os botões Comi/Pulei registrarem o check-in (MOB-18b).
 */
actual class NotificadorLocal(private val contexto: Context, private val pedirNoSistema: suspend () -> Boolean = { false }) : NotificacaoLocal {
    private val app = contexto.applicationContext

    actual override suspend fun permitido(): Boolean = app.getSystemService(NotificationManager::class.java)?.areNotificationsEnabled() ?: false

    actual override suspend fun pedirPermissao(): Boolean = permitido() || (Build.VERSION.SDK_INT >= 33 && pedirNoSistema())

    actual override suspend fun agendar(lembretes: List<Lembrete>, textos: TextosLembrete) {
        cancelarTudo()
        val lista = JSONArray()
        lembretes.forEach { l ->
            val item = JSONObject().put("c", l.chave).put("q", l.quando.toEpochMilliseconds()).put("t", textos.titulo).put("b", textos.corpo(l.refeicao))
                .put("ac", textos.acoes.comi).put("tr", textos.acoes.troquei).put("pu", textos.acoes.pulei)
            if (l.dietaId != null && l.dia != null) item.put("d", l.dietaId.toString()).put("r", l.refeicaoId.toString()).put("x", l.dia.toString())
            AgendaLembretes.agendar(app, item)
            lista.put(item)
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
    private val EXTRAS = listOf("c" to "chave", "t" to "titulo", "b" to "corpo", "d" to "dieta", "r" to "refeicao", "x" to "dia",
        "ac" to "rotuloComi", "tr" to "rotuloTroquei", "pu" to "rotuloPulei")

    private fun intencao(c: Context, chave: String, item: JSONObject? = null) =
        PendingIntent.getBroadcast(c, chave.hashCode(),
            Intent(c, LembreteReceiver::class.java).setAction(ACAO).apply {
                putExtra("chave", chave)
                if (item != null) EXTRAS.forEach { (json, extra) -> item.optString(json).takeIf { it.isNotEmpty() }?.let { putExtra(extra, it) } }
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    fun agendar(c: Context, item: JSONObject) {
        val quando = item.getLong("q")
        if (quando <= System.currentTimeMillis()) return
        c.getSystemService(AlarmManager::class.java)?.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, quando, intencao(c, item.getString("c"), item))
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
        construtor.setSmallIcon(contexto.applicationInfo.icon).setContentTitle(titulo).setContentText(corpo).setContentIntent(abrir).setAutoCancel(true)
        if (ExtrasCheckin.de(intent) != null) {
            intent.getStringExtra("rotuloComi")?.let { construtor.addAction(acao(contexto, chave, it, AcaoLembrete.COMI, intent)) }
            // Troquei só abre o app: a troca precisa escolher o alimento.
            intent.getStringExtra("rotuloTroquei")?.let { rotulo -> abrir?.let { construtor.addAction(Notification.Action.Builder(null as Icon?, rotulo, it).build()) } }
            intent.getStringExtra("rotuloPulei")?.let { construtor.addAction(acao(contexto, chave, it, AcaoLembrete.PULEI, intent)) }
        }
        gerenciador.notify(chave.hashCode(), construtor.build())
    }

    private fun acao(contexto: Context, chave: String, rotulo: String, acao: AcaoLembrete, origem: Intent): Notification.Action {
        val intencao = Intent(contexto, AcaoLembreteReceiver::class.java).setAction(ACAO_CHECKIN).putExtra("chave", chave).putExtra("acao", acao.name)
            .putExtra("dieta", origem.getStringExtra("dieta")).putExtra("refeicao", origem.getStringExtra("refeicao")).putExtra("dia", origem.getStringExtra("dia"))
        val pi = PendingIntent.getBroadcast(contexto, (chave + acao.name).hashCode(), intencao, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return Notification.Action.Builder(null as Icon?, rotulo, pi).build()
    }
}

/** Ids da dieta, da refeição e o dia que a notificação carrega para os botões; só existe quando o lembrete os trouxe. */
internal class ExtrasCheckin(val dieta: Uuid, val refeicao: Uuid, val dia: LocalDate) {
    companion object {
        fun de(intent: Intent): ExtrasCheckin? = try {
            ExtrasCheckin(Uuid.parse(intent.getStringExtra("dieta") ?: return null), Uuid.parse(intent.getStringExtra("refeicao") ?: return null),
                LocalDate.parse(intent.getStringExtra("dia") ?: return null))
        } catch (_: Exception) { null }
    }
}

/**
 * Botões Comi e Pulei da notificação (MOB-18b): grava o check-in local e na fila, pede o envio e some com a notificação, sem abrir o app.
 * Usa o ambiente do `Application` (mesmo banco da UI). Sem sessão ou sem ambiente, a notificação fica e o toque no corpo abre o app.
 */
class AcaoLembreteReceiver : BroadcastReceiver() {
    override fun onReceive(contexto: Context, intent: Intent) {
        if (intent.action != ACAO_CHECKIN) return
        val acao = try { AcaoLembrete.valueOf(intent.getStringExtra("acao") ?: return) } catch (_: Exception) { return }
        val extras = ExtrasCheckin.de(intent) ?: return
        val chave = intent.getStringExtra("chave") ?: return
        val ambiente = (contexto.applicationContext as? ProvedorAmbienteDieta)?.ambiente ?: return
        val pendente = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val resultado = RegistradorAcaoLembrete(RotinaRepositorio(ambiente.banco)).registrar(extras.dieta, extras.refeicao, extras.dia, acao)
                if (resultado == ResultadoAcaoLembrete.GRAVADO) ambiente.solicitarLeitura()
                if (resultado != ResultadoAcaoLembrete.SO_ABRE_O_APP) contexto.getSystemService(NotificationManager::class.java)?.cancel(chave.hashCode())
            } catch (_: Exception) { /* sem sessão ou banco indisponível: a notificação fica para o usuário abrir o app */ }
            finally { pendente.finish() }
        }
    }
}

/** Depois de reiniciar o aparelho os alarmes somem: reagenda o que ainda está no futuro. */
class ReinicioLembretesReceiver : BroadcastReceiver() {
    override fun onReceive(contexto: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        AgendaLembretes.lerLista(contexto).forEach { AgendaLembretes.agendar(contexto, it) }
    }
}
