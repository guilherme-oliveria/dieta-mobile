package org.br.dietamais.ui.comum.anamnese

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.dominio.anamnese.AssuntoAnamnese
import org.br.dietamais.ui.theme.*

@Preview(name = "Resultados compactos", widthDp = TamanhosPreview.compacto, heightDp = TamanhosPreview.alturaCompacta)
@Preview(name = "Resultados médios", widthDp = TamanhosPreview.medio, heightDp = TamanhosPreview.altura)
@Preview(name = "Resultados amplos", widthDp = TamanhosPreview.expandido, heightDp = TamanhosPreview.alturaExpandida)
@Composable
private fun ResultadosPreview() {
    DietaTheme { Surface {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Dimens.paddingCard)) {
            ResultadosAnamnese(AnamneseDerivada(pressaoArterial = "NORMAL", alcool = AlcoolAnamnese(3195.0, 127.8, 894.6),
                salGramasPorPessoaDia = 7, oleoMlPorPessoaDia = 16, oleoKcalPorPessoaDia = 144, atividadesDuracaoMinutos = mapOf("SEGUNDA" to 45)), AssuntoAnamnese.entries.toSet())
        }
    } }
}
