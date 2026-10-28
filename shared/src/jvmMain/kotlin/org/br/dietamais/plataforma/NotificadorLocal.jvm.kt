package org.br.dietamais.plataforma

import org.br.dietamais.dominio.rotina.Lembrete
import java.awt.MenuItem
import java.awt.PopupMenu
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
            val bandeja = garantirIcone(titulo)
            val timer = Timer("lembretes-dieta", true); cronometro = timer
            val agora = System.currentTimeMillis()
            lembretes.forEach { l ->
                val espera = l.quando.toEpochMilliseconds() - agora
                if (espera > 0) timer.schedule(object : TimerTask() { override fun run() { bandeja.displayMessage(titulo, corpo(l.refeicao), TrayIcon.MessageType.INFO) } }, espera)
            }
        }
    }

    private fun garantirIcone(titulo: String): TrayIcon = icone ?: TrayIcon(iconeVerde(), titulo).also { it.isImageAutoSize = true; SystemTray.getSystemTray().add(it); icone = it }

    private fun iconeVerde() = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB).also { imagem ->
        val g = imagem.createGraphics()
        g.color = java.awt.Color(0x2E, 0x7D, 0x4F); g.fillOval(1, 1, 14, 14); g.dispose()
    }

    /** Menu da bandeja (abrir, sincronizar, sair). Devolve false quando o sistema não tem bandeja. */
    fun configurarBandeja(titulo: String, itens: List<Pair<String, () -> Unit>>): Boolean {
        val suportado = try { SystemTray.isSupported() } catch (_: Throwable) { false }
        if (!suportado) return false
        synchronized(trava) {
            val menu = PopupMenu()
            itens.forEach { (nome, acao) -> menu.add(MenuItem(nome).also { it.addActionListener { acao() } }) }
            garantirIcone(titulo).popupMenu = menu
        }
        return true
    }

    /** Balão na bandeja; ignora se não houver bandeja. */
    fun exibir(titulo: String, corpo: String) {
        synchronized(trava) { icone?.displayMessage(titulo, corpo, TrayIcon.MessageType.INFO) }
    }

    /** Tira o ícone da bandeja ao sair. */
    fun removerBandeja() {
        synchronized(trava) { icone?.let { SystemTray.getSystemTray().remove(it) }; icone = null }
    }

    actual override suspend fun cancelarTudo() { synchronized(trava) { cronometro?.cancel(); cronometro = null } }
}
