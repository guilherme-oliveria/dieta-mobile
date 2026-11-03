package org.br.dietamais.ui.comum

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.theme.Dimens
import org.br.dietamais.ui.theme.FormasDieta
import org.jetbrains.compose.resources.stringResource

@Composable
fun CardDieta(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier, shape = FormasDieta.card,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(Dimens.borda, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(Dimens.paddingCard),
            verticalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno), content = content,
        )
    }
}

@Composable
fun CardProximaRefeicao(
    nome: String,
    horario: String,
    energia: String,
    descricao: String,
    aoComer: () -> Unit,
    aoTrocar: () -> Unit,
    aoPular: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
) {
    val cores = MaterialTheme.colorScheme
    Surface(modifier, shape = FormasDieta.destaque, color = cores.primary, contentColor = cores.onPrimary) {
        Column(
            modifier = Modifier.padding(Dimens.paddingDestaque),
            verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio),
        ) {
            Text(stringResource(Res.string.proxima_refeicao), style = MaterialTheme.typography.labelSmall)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(Dimens.espacoMinimo),
            ) {
                Text(
                    stringResource(Res.string.refeicao_nome_horario, nome, horario),
                    style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() },
                )
                Text(energia, style = MaterialTheme.typography.bodyMedium)
            }
            Text(descricao, style = MaterialTheme.typography.bodyMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno),
                verticalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno),
            ) {
                BotaoPrincipal(
                    stringResource(Res.string.comi), aoComer,
                    modifier = Modifier.defaultMinSize(minWidth = Dimens.larguraMinimaAcao), habilitado = habilitado,
                    cores = ButtonDefaults.buttonColors(containerColor = cores.onPrimary, contentColor = cores.onPrimaryContainer),
                )
                BotaoContornado(
                    stringResource(Res.string.troquei), aoTrocar,
                    modifier = Modifier.defaultMinSize(minWidth = Dimens.larguraMinimaAcao), habilitado = habilitado,
                    cores = ButtonDefaults.outlinedButtonColors(contentColor = cores.onPrimary),
                    borda = BorderStroke(Dimens.borda, cores.onPrimary.copy(alpha = 0.5f)),
                )
                BotaoContornado(
                    stringResource(Res.string.pulei), aoPular,
                    modifier = Modifier.defaultMinSize(minWidth = Dimens.larguraMinimaAcao), habilitado = habilitado,
                    cores = ButtonDefaults.outlinedButtonColors(contentColor = cores.onPrimary),
                    borda = BorderStroke(Dimens.borda, cores.onPrimary.copy(alpha = 0.5f)),
                )
            }
        }
    }
}

enum class EstiloStatus { FEITA, TROCADA, AGORA, DEPOIS, ALERTA, INFO }

@Composable
fun ChipStatus(estilo: EstiloStatus, modifier: Modifier = Modifier, texto: String? = null) {
    val cores = MaterialTheme.colorScheme
    val (fundo, corTexto, rotulo) = when (estilo) {
        EstiloStatus.FEITA -> Triple(cores.primaryContainer, cores.onPrimaryContainer, Res.string.status_feita)
        EstiloStatus.TROCADA -> Triple(cores.errorContainer, cores.onErrorContainer, Res.string.status_trocada)
        EstiloStatus.AGORA -> Triple(cores.onSurface, cores.surface, Res.string.status_agora)
        EstiloStatus.DEPOIS -> Triple(cores.surfaceVariant, cores.onSurfaceVariant, Res.string.status_depois)
        EstiloStatus.ALERTA -> Triple(cores.errorContainer, cores.onErrorContainer, Res.string.status_alerta)
        EstiloStatus.INFO -> Triple(cores.tertiaryContainer, cores.onTertiaryContainer, Res.string.status_info)
    }
    Surface(modifier, color = fundo, contentColor = corTexto, shape = FormasDieta.pilula) {
        Text(
            texto ?: stringResource(rotulo), style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = Dimens.espacoPequeno, vertical = Dimens.espacoMinimo),
        )
    }
}

@Composable
fun LinhaRefeicao(
    nome: String,
    horario: String,
    energia: String,
    status: EstiloStatus,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth().heightIn(min = Dimens.alvoToque)
            .padding(horizontal = Dimens.paddingCard, vertical = Dimens.espacoPequeno)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(Dimens.espacoMedio),
        verticalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno),
    ) {
        Text(horario, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(nome, style = MaterialTheme.typography.bodyLarge)
        Text(energia, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ChipStatus(status)
    }
}
