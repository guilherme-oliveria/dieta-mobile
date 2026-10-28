package org.br.dietamais.ui.paciente.hoje

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.nutri.dieta.formatar
import org.br.dietamais.ui.paciente.dieta.EstadoDietaPaciente
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

/** Aba Hoje: próxima refeição em destaque e as demais do dia (MOB-14). Check-in entra na MOB-17. */
@Composable
fun HojeTela(estado: EstadoDietaPaciente, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Dimens.paddingCard),
        verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
        Text(stringResource(Res.string.aba_hoje), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        when {
            !estado.carregado -> EstadoCarregando()
            estado.dieta == null -> EstadoVazio(mensagem = stringResource(Res.string.pac_sem_dieta))
            else -> {
                val proxima = estado.proxima
                if (proxima != null) CardProximaRefeicao(proxima.nome, proxima.horario.toString().take(5),
                    stringResource(Res.string.pac_dieta_kcal_refeicao, formatar(proxima.totais.kcal)),
                    proxima.itens.joinToString(", ") { it.nomeAlimento }, {}, {}, {}, Modifier.fillMaxWidth(), habilitado = false)
                else Text(stringResource(Res.string.pac_hoje_dia_acabou), style = MaterialTheme.typography.bodyMedium)
                estado.refeicoes.forEach { r ->
                    LinhaRefeicao(r.nome, r.horario.toString().take(5), stringResource(Res.string.pac_dieta_kcal_refeicao, formatar(r.totais.kcal)),
                        when { r.id == proxima?.id -> EstiloStatus.AGORA; proxima != null && r.horario > proxima.horario -> EstiloStatus.DEPOIS; else -> EstiloStatus.INFO })
                }
            }
        }
    }
}
