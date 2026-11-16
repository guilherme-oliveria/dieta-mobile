package org.br.dietamais.ui.comum

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import org.br.dietamais.dominio.dieta.kcalInteiro
import org.br.dietamais.dominio.dieta.percentuaisMacros
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

/**
 * Faixa compacta e fixa com o total do dia ao montar a dieta (MOB-UX-09), ex.: "1.850 de 2.000 kcal · C 52% · P 22% · L 26%".
 * Fica fora da rolagem, no celular, para a nutri não perder o total de vista; é região ao vivo.
 */
@Composable
fun FaixaTotais(kcal: Double, carboidratosG: Double, proteinasG: Double, gordurasG: Double, metaKcal: Double? = null, modifier: Modifier = Modifier) {
    val pct = percentuaisMacros(carboidratosG, proteinasG, gordurasG)
    val texto = when {
        pct == null -> stringResource(Res.string.faixa_totais_vazia, kcalInteiro(kcal))
        metaKcal != null && metaKcal > 0 -> stringResource(Res.string.faixa_totais_meta, kcalInteiro(kcal), kcalInteiro(metaKcal),
            "${pct.carboidratos}%", "${pct.proteinas}%", "${pct.lipidios}%")
        else -> stringResource(Res.string.faixa_totais, kcalInteiro(kcal), "${pct.carboidratos}%", "${pct.proteinas}%", "${pct.lipidios}%")
    }
    Surface(modifier.fillMaxWidth().testTag("faixa-totais"), color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
        Box(Modifier.padding(horizontal = Dimens.paddingCard, vertical = Dimens.espacoPequeno)) {
            Text(texto, style = MaterialTheme.typography.labelLarge, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
    }
}
