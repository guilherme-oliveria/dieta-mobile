package org.br.dietamais

import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.runtime.remember
import org.br.dietamais.plataforma.FabricaDriver

fun MainViewController() = ComposeUIViewController {
    val fabrica = remember { FabricaDriver() }
    App(fabricaDriver = fabrica)
}
