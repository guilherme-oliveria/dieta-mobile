package org.br.dietamais.ui.paciente.dieta

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import org.br.dietamais.data.remoto.dto.TotaisNutricionaisResposta
import org.br.dietamais.dominio.rotina.macrosPaciente
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.CardDieta
import org.br.dietamais.ui.comum.graficos.*
import org.br.dietamais.ui.nutri.dieta.formatar
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

internal fun numeroPaciente(n: Double) = formatar(n).trimEnd('0').trimEnd(',')

/** Sempre três macros e sem anel de meta. O gráfico representa o planejamento publicado. */
@Composable
internal fun MacrosPaciente(totais: TotaisNutricionaisResposta, dia: Boolean) {
    val titulo = stringResource(if (dia) Res.string.pac_macros_dia else Res.string.pac_macros_refeicao)
    val rotulos = listOf(stringResource(Res.string.plan_macro_carbo), stringResource(Res.string.plan_macro_proteina), stringResource(Res.string.plan_macro_lipidio))
    val macros = macrosPaciente(totais)
    val fonteAmpliada = LocalDensity.current.fontScale > 1.3f
    CardDieta(Modifier.fillMaxWidth()) {
        Text(titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        if (dia && fonteAmpliada) Text(stringResource(Res.string.pac_kcal, numeroPaciente(totais.kcal)), style = MaterialTheme.typography.titleMedium)
        if (macros == null) Text(stringResource(Res.string.pac_macros_sem_dados), style = MaterialTheme.typography.bodyMedium)
        else {
            val valores = macros.map { stringResource(Res.string.pac_macro_valor, numeroPaciente(it.gramas), numeroPaciente(it.percentual)) }
            val linhas = rotulos.zip(valores)
            GraficoComTabela(linhas) {
                GraficoRosca(rotulos.mapIndexed { i, nome -> ItemGrafico(nome, macros[i].energia) }, Modifier.fillMaxWidth(),
                    centro = if (dia && !fonteAmpliada) stringResource(Res.string.pac_kcal, numeroPaciente(totais.kcal)) else null,
                    tamanho = if (dia) Dimens.roscaPacienteDia else Dimens.roscaPacienteRefeicao,
                    valoresLegenda = valores, descricaoAcessivel = linhas.joinToString("; ") { "${it.first}: ${it.second}" })
            }
        }
        Text(stringResource(Res.string.pac_macros_planejado), style = MaterialTheme.typography.bodySmall)
    }
}
