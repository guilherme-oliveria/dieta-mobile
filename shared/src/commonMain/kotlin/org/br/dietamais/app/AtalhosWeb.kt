package org.br.dietamais.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.navigation.NavHostController

private val teclasNumericas = mapOf(Key.One to 1, Key.Two to 2, Key.Three to 3, Key.Four to 4, Key.Five to 5)

/** Guarda a navegação que o [NavegacaoApp] cria e responde a Alt+1..5 trocando de aba (MOB-45). */
class AtalhosWeb(private val perfil: PerfilNavegacao) {
    private var controlador by mutableStateOf<NavHostController?>(null)

    fun registrar(novo: NavHostController, repassar: (NavHostController) -> Unit) { controlador = novo; repassar(novo) }

    val modificador: Modifier = Modifier.onPreviewKeyEvent { evento ->
        val numero = teclasNumericas[evento.key]
        val rota = if (evento.type == KeyEventType.KeyDown && evento.isAltPressed && numero != null) rotaDoAtalho(perfil, numero) else null
        val nav = controlador
        if (rota != null && nav != null) { nav.irParaAba(rota); true } else false
    }
}
