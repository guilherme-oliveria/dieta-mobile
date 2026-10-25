package org.br.dietamais.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

object FormasDieta {
    val campo = RoundedCornerShape(Dimens.raioBotao)
    val botao = RoundedCornerShape(Dimens.raioBotao)
    val card = RoundedCornerShape(Dimens.raioCard)
    val destaque = RoundedCornerShape(Dimens.raioDestaque)
    val pilula = RoundedCornerShape(percent = 50)
    val material = Shapes(
        extraSmall = RoundedCornerShape(Dimens.raioPequeno),
        small = RoundedCornerShape(Dimens.raioCampo),
        medium = botao,
        large = card,
        extraLarge = destaque,
    )
}
