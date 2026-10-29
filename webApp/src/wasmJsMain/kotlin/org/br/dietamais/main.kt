package org.br.dietamais

import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import org.br.dietamais.app.criarAmbienteWeb
import org.br.dietamais.plataforma.ArmazenamentoSeguro
import org.br.dietamais.plataforma.Config
import org.br.dietamais.plataforma.Conectividade
import org.br.dietamais.plataforma.IdentidadeDispositivo

/** Versão enviada em `X-App-Versao`; o `APP_VERSAO_MINIMA_WEB` do backend (vazio = sem bloqueio) compara com ela. */
private const val VERSAO_WEB = "1.1.0"

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    document.getElementById("carregando")?.let { it.parentNode?.removeChild(it) }
    ComposeViewport(document.body!!) {
        val ambiente = remember { criarAmbienteWeb(Config(), ArmazenamentoSeguro(), VERSAO_WEB, Conectividade()) }
        AppWeb(ambiente, remember { IdentidadeDispositivo() })
    }
}
