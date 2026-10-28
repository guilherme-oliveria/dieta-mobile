package org.br.dietamais.plataforma

import org.br.dietamais.dominio.rotina.Lembrete
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.util.Timer
import java.util.TimerTask

/** Desktop: balão na bandeja do sistema enquanto o app estiver aberto (não há agendamento do SO). */
actual class NotificadorLocal : NotificacaoLocal {
    private val trava = Any()
    private var cronometro: Timer? = null
    private var icone: TrayIcon? = null

    actual override suspend fun permitido(): Boolean = try { SystemTray.isSupported() } catch (_: Throwable) { false }

    actual override suspend fun pedirPermissao(): Boolean = permitido()

    actual override suspend fun agendar(lembretes: List<Lembrete>, titulo: String, corpo: (String) -> String) {
        cancelarTudo()
        if (!permitido()) return
        synchronized(trava) {
            val tray = SystemTray.getSystemTray()
            val bandeja = icone ?: TrayIcon(BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB), titulo).also { it.isImageAutoSize = true; tray.add(it); icone = it }
            val timer = Timer("lembretes-dieta", true); cronometro = timer
            val agora = System.currentTimeMillis()
            lembretes.forEach { l ->
                val espera = l.quando.toEpochMilliseconds() - agora
                if (espera > 0) timer.schedule(object : TimerTask() { override fun run() { bandeja.displayMessage(titulo, corpo(l.refeicao), TrayIcon.MessageType.INFO) } }, espera)
            }
        }
    }

    actual override suspend fun cancelarTudo() { synchronized(trava) { cronometro?.cancel(); cronometro = null } }
}
