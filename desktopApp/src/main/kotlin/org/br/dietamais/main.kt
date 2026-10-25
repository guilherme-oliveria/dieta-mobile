package org.br.dietamais

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.unit.DpSize
import org.br.dietamais.app.tituloAplicacao
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
        App()
    }
}
