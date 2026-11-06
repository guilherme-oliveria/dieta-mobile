package org.br.dietamais.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.window.core.layout.WindowSizeClass
import org.br.dietamais.ui.comum.BarraNavegacaoInferior
import org.br.dietamais.ui.comum.HospedeSobreposicao
import org.br.dietamais.ui.comum.ItemNavegacao
import org.br.dietamais.ui.comum.LocalHospedeSobreposicao
import org.br.dietamais.ui.comum.TrilhoNavegacao

@Composable
fun AppScaffold(
    itens: List<ItemNavegacao>,
    selecionada: String?,
    aoSelecionar: (String) -> Unit,
    classeJanela: WindowSizeClass,
    content: @Composable () -> Unit,
) {
    val hospede = remember { HospedeSobreposicao() }
    val compacta = !classeJanela.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = { if (compacta) BarraNavegacaoInferior(itens, selecionada, aoSelecionar) },
    ) { padding ->
        Row(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            if (!compacta) TrilhoNavegacao(itens, selecionada, aoSelecionar)
            // Telas cheias (montar dieta, avaliação...) entram aqui, acima do conteúdo da aba e abaixo do menu, que continua à vista.
            Box(Modifier.weight(1f).fillMaxSize()) {
                // Com tela cheia aberta, o que está por baixo sai da árvore de acessibilidade: leitor de tela e testes só veem a tela de cima.
                Box(Modifier.fillMaxSize().then(if (hospede.entradas.isEmpty()) Modifier else Modifier.clearAndSetSemantics {})) {
                    CompositionLocalProvider(LocalHospedeSobreposicao provides hospede) { content() }
                }
                hospede.entradas.forEach { entrada ->
                    key(entrada) { Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { entrada.conteudo() } }
                }
            }
        }
    }
}
