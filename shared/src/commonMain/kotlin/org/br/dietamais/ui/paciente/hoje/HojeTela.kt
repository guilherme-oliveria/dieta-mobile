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
import kotlinx.datetime.LocalDate
import org.br.dietamais.data.remoto.dto.StatusCheckin
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.nutri.dieta.formatar
import org.br.dietamais.ui.paciente.dieta.EstadoDietaPaciente
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource
import kotlin.uuid.Uuid

class AcoesHoje(val aoMudarDia: (LocalDate) -> Unit, val aoMarcar: (Uuid, StatusCheckin) -> Unit, val aoTrocar: (Uuid) -> Unit, val aoBeberAgua: (Int) -> Unit)

/** Aba Hoje: dia, próxima refeição com Comi/Troquei/Pulei, demais refeições e água (MOB-14, 15, 17). */
@Composable
fun HojeTela(estado: EstadoDietaPaciente, acoes: AcoesHoje, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Dimens.paddingCard),
        verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
        Text(stringResource(Res.string.aba_hoje), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        when {
            !estado.carregado -> EstadoCarregando()
            estado.dieta == null -> EstadoVazio(mensagem = stringResource(Res.string.pac_sem_dieta))
            else -> {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                    estado.dias.forEach { d ->
                        BotaoTexto(rotuloDia(d, estado.hoje), { acoes.aoMudarDia(d) }, selecionado = d == estado.dia)
                    }
                }
                val proxima = estado.proxima
                if (proxima != null) CardProximaRefeicao(proxima.nome, proxima.horario.toString().take(5),
                    stringResource(Res.string.pac_dieta_kcal_refeicao, formatar(proxima.totais.kcal)),
                    proxima.itens.joinToString(", ") { it.nomeAlimento },
                    { acoes.aoMarcar(proxima.id, StatusCheckin.FEITO) }, { acoes.aoTrocar(proxima.id) }, { acoes.aoMarcar(proxima.id, StatusCheckin.PULADO) },
                    Modifier.fillMaxWidth())
                else if (estado.ehHoje) Text(stringResource(Res.string.pac_hoje_dia_acabou), style = MaterialTheme.typography.bodyMedium)
                estado.refeicoes.forEach { r ->
                    val feito = estado.checkins[r.id]
                    val estilo = when {
                        feito == StatusCheckin.FEITO -> EstiloStatus.FEITA
                        feito == StatusCheckin.TROCADO -> EstiloStatus.TROCADA
                        feito == StatusCheckin.PULADO -> EstiloStatus.ALERTA
                        r.id == proxima?.id -> EstiloStatus.AGORA
                        else -> EstiloStatus.DEPOIS
                    }
                    Column {
                        LinhaRefeicao(r.nome, r.horario.toString().take(5), stringResource(Res.string.pac_dieta_kcal_refeicao, formatar(r.totais.kcal)), estilo)
                        // refeições passadas ou de dias anteriores também aceitam o registro (até 3 dias)
                        if (r.id != proxima?.id && feito == null) FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                            BotaoTexto(stringResource(Res.string.pac_comi), { acoes.aoMarcar(r.id, StatusCheckin.FEITO) })
                            BotaoTexto(stringResource(Res.string.pac_troquei), { acoes.aoTrocar(r.id) })
                            BotaoTexto(stringResource(Res.string.pac_pulei), { acoes.aoMarcar(r.id, StatusCheckin.PULADO) })
                        }
                    }
                }
                CardDieta(Modifier.fillMaxWidth()) {
                    Text(stringResource(Res.string.pac_agua_titulo), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(Res.string.pac_agua_progresso, estado.aguaMl.toString(), estado.metaAguaMl.toString()), style = MaterialTheme.typography.bodyMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                        listOf(200, 300, 500).forEach { ml -> BotaoTexto(stringResource(Res.string.pac_agua_mais, ml.toString()), { acoes.aoBeberAgua(ml) }) }
                    }
                }
            }
        }
    }
}

@Composable
private fun rotuloDia(d: LocalDate, hoje: LocalDate?): String = when {
    d == hoje -> stringResource(Res.string.pac_dia_hoje)
    else -> "${d.day.toString().padStart(2, '0')}/${(d.month.ordinal + 1).toString().padStart(2, '0')}"
}
