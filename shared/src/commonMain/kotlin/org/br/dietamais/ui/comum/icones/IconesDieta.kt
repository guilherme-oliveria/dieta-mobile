package org.br.dietamais.ui.comum.icones

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import org.br.dietamais.ui.theme.Dimens

object IconesDieta {
    val Casa by lazy { icone("Casa", "M3 10.5 12 3l9 7.5V20a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z") }
    val Talheres by lazy { icone("Talheres", "M7 2v20M4 2v6a3 3 0 0 0 6 0V2M17 22V2c-2 0-4 2-4 6v5h4") }
    val Carrinho by lazy { icone("Carrinho", "M2 3h3l2.7 12.4a2 2 0 0 0 2 1.6h7.6a2 2 0 0 0 2-1.5L21 8H6", "M10.5 20a1.5 1.5 0 1 1-3 0a1.5 1.5 0 1 1 3 0M19.5 20a1.5 1.5 0 1 1-3 0a1.5 1.5 0 1 1 3 0") }
    val Grafico by lazy { icone("Gráfico", "M3 3v18h18M7 15l4-4 3 3 6-6") }
    val Pessoa by lazy { icone("Pessoa", "M4 21a8 8 0 0 1 16 0M16 8a4 4 0 1 1-8 0a4 4 0 1 1 8 0") }
    val Sino by lazy { icone("Sino", "M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9M10.3 21a1.94 1.94 0 0 0 3.4 0") }
    val Agua by lazy { icone("Água", "M12 2.7s6 6.4 6 11.3a6 6 0 0 1-12 0c0-4.9 6-11.3 6-11.3z") }
    val Cadeado by lazy { icone("Cadeado", "M5 11h14a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2zM7 11V7a5 5 0 0 1 10 0v4") }
    val Calendario by lazy { icone("Calendário", "M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2zM16 2v4M8 2v4M3 10h18") }
    val Lista by lazy { icone("Lista", "M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01") }
    val Mais by lazy { icone("Mais", "M12 5v14M5 12h14") }
    val Voltar by lazy { icone("Voltar", "M15 18l-6-6 6-6", espelhar = true) }
    val Fechar by lazy { icone("Fechar", "M18 6 6 18M6 6l12 12") }
    val Busca by lazy { icone("Busca", "M18 11a7 7 0 1 1-14 0a7 7 0 1 1 14 0M20 20l-3.5-3.5") }
    val Alerta by lazy { icone("Alerta", "M10.3 3.8 1.8 18.5A1.7 1.7 0 0 0 3.3 21h17.4a1.7 1.7 0 0 0 1.5-2.5L13.7 3.8a2 2 0 0 0-3.4 0zM12 9v4M12 17h.01") }
    val Informacao by lazy { icone("Informação", "M22 12a10 10 0 1 1-20 0a10 10 0 1 1 20 0M12 16v-4M12 8h.01") }
    val SemInternet by lazy { icone("Sem internet", "M2 2l20 20M8.5 8.5a12 12 0 0 1 11 2.5M5 11a12 12 0 0 1 1.5-1M8.5 14.5a5 5 0 0 1 7 0M12 18h.01M2 7a17 17 0 0 1 1.5-1M9 3.3A17 17 0 0 1 22 7") }
}

private fun icone(nome: String, vararg caminhos: String, espelhar: Boolean = false): ImageVector {
    val builder = ImageVector.Builder(
        name = nome, defaultWidth = Dimens.iconePadrao, defaultHeight = Dimens.iconePadrao,
        viewportWidth = Dimens.viewportIcone, viewportHeight = Dimens.viewportIcone,
        autoMirror = espelhar,
    )
    caminhos.forEach { caminho ->
        builder.addPath(
            pathData = PathParser().parsePathString(caminho).toNodes(),
            fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = Dimens.tracoIcone,
            strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
        )
    }
    return builder.build()
}
