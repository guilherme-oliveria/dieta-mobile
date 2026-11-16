package org.br.dietamais.ui.nutri.dieta

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import org.br.dietamais.data.remoto.dto.TotaisNutricionaisResposta
import org.br.dietamais.dominio.dieta.ClasseDri
import org.br.dietamais.dominio.dieta.linhasDri
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

/** Micronutrientes do dia contra a DRI do paciente (MOB-65, ART-P4-04, RN-DIE-13): o estado vai em texto, não só em cor. Some quando o servidor não manda adequação. */
@Composable
fun MicronutrientesDriCard(totais: TotaisNutricionaisResposta) {
    val linhas = linhasDri(totais.adequacao)
    if (linhas.isEmpty()) return
    var aberto by remember { mutableStateOf(false) }
    Text(stringResource(Res.string.dri_titulo), style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
    BotaoTexto(stringResource(if (aberto) Res.string.dri_recolher else Res.string.dri_expandir, linhas.count { it.classe != ClasseDri.ADEQUADO }), { aberto = !aberto })
    if (aberto) {
        linhas.forEach { l ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                Text(l.nome, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                l.valor?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                Text(stringResource(Res.string.dri_percentual, kotlin.math.round(l.percentual).toLong()), style = MaterialTheme.typography.bodyMedium)
                ChipStatus(if (l.classe == ClasseDri.ADEQUADO) EstiloStatus.FEITA else EstiloStatus.ALERTA, texto = stringResource(when (l.classe) {
                    ClasseDri.ABAIXO -> Res.string.dri_abaixo
                    ClasseDri.ADEQUADO -> Res.string.dri_adequado
                    ClasseDri.ACIMA -> Res.string.dri_acima
                    ClasseDri.OUTRA -> Res.string.dri_outra
                }))
            }
        }
        if (!totais.incompleto.isNullOrEmpty()) Text(stringResource(Res.string.dri_parcial), style = MaterialTheme.typography.bodySmall)
        Text(stringResource(Res.string.dri_fonte), style = MaterialTheme.typography.bodySmall)
    }
}
