package org.br.dietamais.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.br.dietamais.recursos.Res
import org.br.dietamais.recursos.plus_jakarta_sans_bold
import org.br.dietamais.recursos.plus_jakarta_sans_extrabold
import org.br.dietamais.recursos.plus_jakarta_sans_medium
import org.br.dietamais.recursos.plus_jakarta_sans_regular
import org.br.dietamais.recursos.plus_jakarta_sans_semibold
import org.jetbrains.compose.resources.Font

@Composable
internal fun tipografiaDieta(): Typography {
    val regular = Font(Res.font.plus_jakarta_sans_regular, FontWeight.Normal)
    val medium = Font(Res.font.plus_jakarta_sans_medium, FontWeight.Medium)
    val semibold = Font(Res.font.plus_jakarta_sans_semibold, FontWeight.SemiBold)
    val bold = Font(Res.font.plus_jakarta_sans_bold, FontWeight.Bold)
    val extrabold = Font(Res.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold)
    val familia = remember(regular, medium, semibold, bold, extrabold) {
        FontFamily(regular, medium, semibold, bold, extrabold)
    }
    return remember(familia) {
        fun estilo(tamanho: Int, peso: FontWeight, altura: Int) = TextStyle(
            fontFamily = familia, fontSize = tamanho.sp, fontWeight = peso, lineHeight = altura.sp,
        )
        Typography(
            displayLarge = estilo(57, FontWeight.ExtraBold, 64),
            displayMedium = estilo(45, FontWeight.ExtraBold, 52),
            displaySmall = estilo(36, FontWeight.ExtraBold, 44),
            headlineLarge = estilo(34, FontWeight.ExtraBold, 42).copy(letterSpacing = (-0.03).em),
            headlineMedium = estilo(28, FontWeight.ExtraBold, 36).copy(letterSpacing = (-0.02).em),
            headlineSmall = estilo(24, FontWeight.ExtraBold, 32),
            titleLarge = estilo(22, FontWeight.ExtraBold, 30),
            titleMedium = estilo(17, FontWeight.Bold, 25),
            titleSmall = estilo(15, FontWeight.Bold, 23),
            bodyLarge = estilo(16, FontWeight.SemiBold, 24),
            bodyMedium = estilo(14, FontWeight.Medium, 21),
            bodySmall = estilo(13, FontWeight.Medium, 20),
            labelLarge = estilo(14, FontWeight.SemiBold, 22),
            labelMedium = estilo(12, FontWeight.Bold, 18),
            labelSmall = estilo(11, FontWeight.Bold, 17).copy(letterSpacing = 0.08.em),
        )
    }
}
