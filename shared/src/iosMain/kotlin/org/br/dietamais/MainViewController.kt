package org.br.dietamais

import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.runtime.remember
import org.br.dietamais.plataforma.FabricaDriver
import org.br.dietamais.plataforma.ArmazenamentoSeguro
import platform.Foundation.NSBundle

fun MainViewController() = ComposeUIViewController {
    val fabrica = remember { FabricaDriver() }
    val cofre = remember { ArmazenamentoSeguro() }
    val versao = remember { NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "1.0" }
    App(fabricaDriver = fabrica, cofreSessao = cofre, versaoApp = versao)
}
