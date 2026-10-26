package org.br.dietamais

import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.runtime.remember
import org.br.dietamais.plataforma.FabricaDriver
import org.br.dietamais.plataforma.ArmazenamentoSeguro
import org.br.dietamais.plataforma.IdentidadeDispositivo
import org.br.dietamais.plataforma.Compartilhador
import org.br.dietamais.plataforma.Config
import platform.Foundation.NSBundle

fun MainViewController() = ComposeUIViewController {
    val fabrica = remember { FabricaDriver() }
    val cofre = remember { ArmazenamentoSeguro() }
    val identidade = remember { IdentidadeDispositivo() }
    val compartilhador = remember { Compartilhador() }
    val config = remember {
        val dominio = NSBundle.mainBundle.objectForInfoDictionaryKey("ConviteHost") as? String
        Config(DOMINIOS_CONVITE = listOfNotNull(dominio).filter { it.isNotBlank() && it != "nao-configurado.invalid" }.toSet())
    }
    val versao = remember { NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "1.0" }
    App(config = config, fabricaDriver = fabrica, cofreSessao = cofre, versaoApp = versao, identidadeDispositivo = identidade, compartilhamento = compartilhador)
}
