package org.br.dietamais.ui.theme

import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass

object Dimens {
    val nenhum = 0.dp
    val espacoMinimo = 4.dp
    val espacoPequeno = 8.dp
    val espacoMedio = 12.dp
    val paddingCampo = 14.dp
    val paddingCard = 16.dp
    val paddingDestaque = 18.dp
    val margemCompacta = 20.dp
    val margemAmpla = 24.dp
    val alturaBotao = 52.dp
    val alvoToque = 48.dp
    val larguraMinimaAcao = 96.dp
    val larguraLeitura = 640.dp
    val larguraListaPacientes = 360.dp
    val larguraTrilho = 96.dp
    val alturaBarraNavegacao = 80.dp
    val alturaItemTrilho = 64.dp
    val janelaMinimaLargura = 1024.dp
    val janelaMinimaAltura = 700.dp
    val janelaInicialLargura = 1280.dp
    val janelaInicialAltura = 800.dp
    val borda = 1.dp
    val progressoTraco = 2.dp
    val iconePequeno = 20.dp
    val iconePadrao = 24.dp
    val iconeEstado = 36.dp
    val raioPequeno = 8.dp
    val raioCampo = 12.dp
    val raioBotao = 14.dp
    val raioCard = 20.dp
    val raioDestaque = 22.dp
    const val viewportIcone = 24f
    const val tracoIcone = 2f

    fun margemTela(classe: WindowSizeClass) = when {
        classe.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) -> margemAmpla
        classe.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) -> margemAmpla
        else -> margemCompacta
    }
}

object TamanhosPreview {
    const val compacto = 390
    const val medio = 820
    const val expandido = 1180
    const val altura = 1180
    const val alturaCompacta = 844
    const val alturaExpandida = 820
}
