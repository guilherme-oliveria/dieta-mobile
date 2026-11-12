package org.br.dietamais.ui.paciente.hoje

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import org.br.dietamais.data.remoto.dto.StatusCheckin
import org.br.dietamais.dominio.rotina.*
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.nutri.agenda.nomeDiaSemana
import org.br.dietamais.ui.nutri.dieta.formatar
import org.br.dietamais.ui.paciente.dieta.EstadoDietaPaciente
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource
import kotlin.uuid.Uuid

class AcoesHoje(val aoMudarDia: (LocalDate) -> Unit, val aoMarcar: (Uuid, StatusCheckin) -> Unit, val aoTrocar: (Uuid) -> Unit, val aoBeberAgua: (Int) -> Unit, val aoDesfazer: () -> Unit = {}, val aoDesfazerAgua: () -> Unit = {})

/** Aba Hoje: dia, próxima refeição com Comi/Troquei/Pulei, demais refeições e água (MOB-14, 15, 17). */
@Composable
fun HojeTela(estado: EstadoDietaPaciente, acoes: AcoesHoje, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Dimens.paddingCard),
        verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
        Text(stringResource(Res.string.aba_hoje), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        if (estado.erro != null) Text(stringResource(Res.string.pac_erro_geral), color = MaterialTheme.colorScheme.error,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
        when {
            !estado.carregado -> EstadoCarregando()
            estado.dieta == null -> EstadoVazio(mensagem = stringResource(Res.string.pac_sem_dieta))
            else -> {
                if (estado.podeDesfazer) AvisoDesfazer(stringResource(Res.string.pac_checkin_registrado), stringResource(Res.string.pac_desfazer), acoes.aoDesfazer)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                    estado.dias.forEach { d ->
                        val extenso = descricaoDia(d)
                        ChipEscolha(d == estado.dia, { acoes.aoMudarDia(d) }, { Text(rotuloDia(d, estado.hoje)) },
                            Modifier.semantics { contentDescription = extenso })
                    }
                }
                }
                val proxima = estado.proxima
                if (proxima != null) CardProximaRefeicao(proxima.nome, proxima.horario.toString().take(5),
                    stringResource(Res.string.pac_dieta_kcal_refeicao, formatar(proxima.totais.kcal)),
                    proxima.itens.joinToString(", ") { it.textoLivre ?: it.nomeAlimento.orEmpty() },
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
                CardAgua(estado, acoes)
            }
        }
    }
}

@Composable
private fun rotuloDia(d: LocalDate, hoje: LocalDate?): String = when (tipoDia(d, hoje)) {
    TipoDia.HOJE -> stringResource(Res.string.pac_dia_hoje)
    TipoDia.ONTEM -> stringResource(Res.string.pac_dia_ontem)
    TipoDia.OUTRO -> stringResource(Res.string.pac_dia_rotulo, nomeDiaSemana(d.dayOfWeek), diaMes(d))
}

/** Data por extenso para o leitor de tela: "segunda-feira, 9 de novembro". */
@Composable
private fun descricaoDia(d: LocalDate): String = stringResource(Res.string.pac_dia_extenso,
    stringResource(when (d.dayOfWeek) {
        DayOfWeek.MONDAY -> Res.string.pac_dia_longo_seg; DayOfWeek.TUESDAY -> Res.string.pac_dia_longo_ter
        DayOfWeek.WEDNESDAY -> Res.string.pac_dia_longo_qua; DayOfWeek.THURSDAY -> Res.string.pac_dia_longo_qui
        DayOfWeek.FRIDAY -> Res.string.pac_dia_longo_sex; DayOfWeek.SATURDAY -> Res.string.pac_dia_longo_sab
        DayOfWeek.SUNDAY -> Res.string.pac_dia_longo_dom
    }), d.day.toString(), stringResource(when (d.month.ordinal) {
        0 -> Res.string.pac_mes_1; 1 -> Res.string.pac_mes_2; 2 -> Res.string.pac_mes_3; 3 -> Res.string.pac_mes_4
        4 -> Res.string.pac_mes_5; 5 -> Res.string.pac_mes_6; 6 -> Res.string.pac_mes_7; 7 -> Res.string.pac_mes_8
        8 -> Res.string.pac_mes_9; 9 -> Res.string.pac_mes_10; 10 -> Res.string.pac_mes_11; else -> Res.string.pac_mes_12
    }))

/** Cartão de água: três valores rápidos, valor livre e desfazer do último lançamento ainda não enviado (MOB-UX-05). */
@Composable
private fun CardAgua(estado: EstadoDietaPaciente, acoes: AcoesHoje) {
    var livre by remember { mutableStateOf(false) }
    var texto by remember { mutableStateOf("") }
    var tentou by remember { mutableStateOf(false) }
    val valor = lerMlAgua(texto)
    CardDieta(Modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.pac_agua_titulo), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(Res.string.pac_agua_progresso, estado.aguaMl.toString(), estado.metaAguaMl.toString()), style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
            listOf(200, 300, 500).forEach { ml -> BotaoTexto(stringResource(Res.string.pac_agua_mais, ml.toString()), { acoes.aoBeberAgua(ml) }) }
            BotaoTexto(stringResource(Res.string.pac_agua_outro), { livre = !livre }, selecionado = livre)
            BotaoTexto(stringResource(Res.string.pac_agua_desfazer), acoes.aoDesfazerAgua, habilitado = estado.podeDesfazerAgua)
        }
        if (livre) {
            CampoNumero(stringResource(Res.string.pac_agua_outro_rotulo), texto, { texto = it.filter(Char::isDigit).take(5); tentou = false },
                Modifier.fillMaxWidth(), erro = if (tentou && valor == null) stringResource(Res.string.pac_agua_faixa_invalida, AGUA_MIN_ML.toString(), AGUA_MAX_ML.toString()) else null)
            BotaoPrincipal(stringResource(Res.string.pac_agua_outro_adicionar), {
                if (valor == null) tentou = true else { acoes.aoBeberAgua(valor); texto = ""; livre = false }
            })
        }
    }
}
