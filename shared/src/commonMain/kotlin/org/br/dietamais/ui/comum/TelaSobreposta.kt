package org.br.dietamais.ui.comum

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

internal class EntradaSobreposicao(conteudo: @Composable () -> Unit) {
    var conteudo by mutableStateOf(conteudo)
}

/** Pilha de telas cheias que o shell desenha na área de conteúdo, deixando a navegação principal (barra ou trilho) sempre visível. */
class HospedeSobreposicao {
    internal val entradas = mutableStateListOf<EntradaSobreposicao>()
}

val LocalHospedeSobreposicao = compositionLocalOf<HospedeSobreposicao?> { null }

/**
 * Tela cheia sobre o conteúdo da aba, sem cobrir o menu inferior (ou o trilho). Sem shell (testes, telas de entrada),
 * cai no [Dialog] de tela cheia de antes. O voltar do sistema chama [aoFechar].
 */
@Composable
@OptIn(ExperimentalComposeUiApi::class)
fun TelaSobreposta(aoFechar: () -> Unit, conteudo: @Composable () -> Unit) {
    val hospede = LocalHospedeSobreposicao.current
    if (hospede == null) {
        Dialog(onDismissRequest = aoFechar, properties = DialogProperties(usePlatformDefaultWidth = false), content = conteudo)
        return
    }
    val entrada = remember(hospede) { EntradaSobreposicao(conteudo) }
    SideEffect { entrada.conteudo = conteudo }
    DisposableEffect(hospede, entrada) {
        hospede.entradas.add(entrada)
        onDispose { hospede.entradas.remove(entrada) }
    }
    BackHandler(true, aoFechar)
}
