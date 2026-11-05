package org.br.dietamais.ui.comum

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import org.br.dietamais.data.repositorio.PortaPdf
import org.br.dietamais.data.repositorio.RecursoPdf
import org.br.dietamais.data.repositorio.TamanhoListaPdf
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.uuid.Uuid

/** MOB-73: um botão por versão do PDF da lista de substituição (pequena, média, grande e contagem de carboidratos). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BotoesPdfLista(pdf: PortaPdf, dietaId: Uuid, habilitado: Boolean) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
        TamanhoListaPdf.entries.forEach { t ->
            BotaoPdf(stringResource(rotuloTamanho(t)), habilitado, { pdf.link(RecursoPdf.LISTA, dietaId, t) })
        }
    }
}

internal fun rotuloTamanho(t: TamanhoListaPdf): StringResource = when (t) {
    TamanhoListaPdf.PEQUENA -> Res.string.pdf_lista_pequena
    TamanhoListaPdf.MEDIA -> Res.string.pdf_lista_media
    TamanhoListaPdf.GRANDE -> Res.string.pdf_lista_grande
    TamanhoListaPdf.CARBOIDRATOS -> Res.string.pdf_lista_carboidratos
}
