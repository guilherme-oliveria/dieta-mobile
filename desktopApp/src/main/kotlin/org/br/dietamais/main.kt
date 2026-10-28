package org.br.dietamais

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import org.br.dietamais.app.tituloAplicacao
import org.br.dietamais.plataforma.FabricaDriver
import org.br.dietamais.plataforma.ArmazenamentoSeguro
import org.br.dietamais.plataforma.IdentidadeDispositivo
import org.br.dietamais.plataforma.Compartilhador
import org.br.dietamais.plataforma.Conectividade
import org.br.dietamais.plataforma.Config
import org.br.dietamais.plataforma.NotificadorLocal
import org.br.dietamais.app.criarAmbienteDieta
import org.br.dietamais.ui.theme.Dimens
import java.awt.Dimension

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = tituloAplicacao(),
        state = rememberWindowState(size = DpSize(Dimens.janelaInicialLargura, Dimens.janelaInicialAltura)),
    ) {
        DisposableEffect(window) {
            window.minimumSize = Dimension(Dimens.janelaMinimaLargura.value.toInt(), Dimens.janelaMinimaAltura.value.toInt())
            onDispose { }
        }
        val fabrica = remember { FabricaDriver() }
        val cofre = remember { ArmazenamentoSeguro() }
        val identidade = remember { IdentidadeDispositivo() }
        val compartilhador = remember { Compartilhador() }
        val notificador = remember { NotificadorLocal() }
        DisposableEffect(notificador) { onDispose { kotlinx.coroutines.runBlocking { notificador.cancelarTudo() } } }
        val versao = remember { versaoAplicacao() }
        val ambiente = remember { criarAmbienteDieta(Config(), fabrica, cofre, versao, Conectividade()) }
        DisposableEffect(ambiente) { onDispose { ambiente.close() } }
        App(ambiente = ambiente, identidadeDispositivo = identidade, compartilhamento = compartilhador, notificacao = notificador)
    }
}
