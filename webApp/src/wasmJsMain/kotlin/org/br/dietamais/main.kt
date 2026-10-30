package org.br.dietamais

import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import androidx.navigation.bindToBrowserNavigation
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.br.dietamais.app.criarAmbienteWeb
import org.br.dietamais.plataforma.ArmazenamentoSeguro
import org.br.dietamais.plataforma.Compartilhador
import org.br.dietamais.plataforma.Config
import org.br.dietamais.plataforma.Conectividade
import org.br.dietamais.dominio.convite.codigoDoCaminhoWeb
import org.br.dietamais.plataforma.IdentidadeDispositivo
import org.br.dietamais.plataforma.receberLinkConvite

/** Versão enviada em `X-App-Versao`; o `APP_VERSAO_MINIMA_WEB` do backend (vazio = sem bloqueio) compara com ela. */
private const val VERSAO_WEB = "1.1.0"

@OptIn(ExperimentalComposeUiApi::class, androidx.navigation.ExperimentalBrowserHistoryApi::class)
fun main() {
    document.getElementById("carregando")?.let { it.parentNode?.removeChild(it) }
    // Link direto do convite (`/convite/{codigo}`): entra pelo mesmo caminho do link aberto no app
    codigoDoCaminhoWeb(window.location.pathname)?.let { receberLinkConvite("dietamais://convite/$it") }
    ComposeViewport(document.getElementById("app")!!) {
        val ambiente = remember { criarAmbienteWeb(Config(), ArmazenamentoSeguro(), VERSAO_WEB, Conectividade()) }
        AppWeb(ambiente, remember { IdentidadeDispositivo() }, remember { Compartilhador() }, aoPrepararNavegacao = { controlador ->
            // MOB-45: URL e botão voltar do navegador acompanham a navegação do app
            MainScope().launch { controlador.bindToBrowserNavigation() }
        })
    }
}
