package org.br.dietamais

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.launch
import org.br.dietamais.app.criarAmbienteDieta
import org.br.dietamais.app.tituloAplicacao
import org.br.dietamais.plataforma.ArmazenamentoSeguro
import org.br.dietamais.plataforma.Compartilhador
import org.br.dietamais.plataforma.Config
import org.br.dietamais.plataforma.Conectividade
import org.br.dietamais.plataforma.FabricaDriver
import org.br.dietamais.plataforma.IdentidadeDispositivo
import org.br.dietamais.plataforma.NotificadorLocal
import org.br.dietamais.ui.theme.Dimens
import java.awt.Dimension
import kotlin.time.Duration.Companion.minutes

/** MOB-31: no consultório o app fica aberto o dia todo, então sincroniza a cada 5 min mesmo com a janela na bandeja. */
fun main() = application {
    val escopo = androidx.compose.runtime.rememberCoroutineScope()
    val titulo = tituloAplicacao()
    val notificador = remember { NotificadorLocal() }
    val fabrica = remember { FabricaDriver() }
    val cofre = remember { ArmazenamentoSeguro() }
    val identidade = remember { IdentidadeDispositivo() }
    val compartilhador = remember { Compartilhador() }
    val versao = remember { versaoAplicacao() }
    val ambiente = remember { criarAmbienteDieta(Config(), fabrica, cofre, versao, Conectividade(), intervaloSync = 5.minutes, sempreAtivo = true) }
    var visivel by remember { mutableStateOf(true) }
    var temBandeja by remember { mutableStateOf(false) }
    val sair = {
        notificador.removerBandeja()
        exitApplication()
    }
    DisposableEffect(ambiente) { onDispose { ambiente.close() } }
    DisposableEffect(notificador) { onDispose { kotlinx.coroutines.runBlocking { notificador.cancelarTudo() } } }
    LaunchedEffect(notificador) {
        temBandeja = notificador.configurarBandeja(titulo, listOf(
            "Abrir" to { visivel = true },
            "Sincronizar agora" to { ambiente.solicitarLeitura() },
            "Sair" to { sair() },
        ))
    }
    LaunchedEffect(ambiente, temBandeja) {
        if (temBandeja) ambiente.avisosNovos().collect { notificador.exibir(titulo, it.titulo) }
    }
    Window(
        onCloseRequest = { if (temBandeja) visivel = false else sair() },
        visible = visivel,
        title = titulo,
        state = rememberWindowState(size = DpSize(Dimens.janelaInicialLargura, Dimens.janelaInicialAltura)),
    ) {
        DisposableEffect(window) {
            window.minimumSize = Dimension(Dimens.janelaMinimaLargura.value.toInt(), Dimens.janelaMinimaAltura.value.toInt())
            onDispose { }
        }
        MenuBar {
            Menu("Arquivo") {
                Item("Sincronizar agora", shortcut = KeyShortcut(Key.R, ctrl = true)) { escopo.launch { ambiente.solicitarLeitura() } }
                Item("Esconder na bandeja", enabled = temBandeja, shortcut = KeyShortcut(Key.W, ctrl = true)) { visivel = false }
                Item("Sair", shortcut = KeyShortcut(Key.Q, ctrl = true)) { sair() }
            }
        }
        App(ambiente = ambiente, identidadeDispositivo = identidade, compartilhamento = compartilhador, notificacao = notificador)
    }
}
