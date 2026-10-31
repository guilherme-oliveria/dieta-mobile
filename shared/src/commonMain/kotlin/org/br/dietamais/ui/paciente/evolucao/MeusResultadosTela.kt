package org.br.dietamais.ui.paciente.evolucao

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.br.dietamais.dominio.agenda.formatarData
import org.br.dietamais.dominio.avaliacao.*
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.CardDieta
import org.br.dietamais.ui.comum.ChipStatus
import org.br.dietamais.ui.comum.EstiloStatus
import org.br.dietamais.ui.comum.graficos.*
import org.br.dietamais.ui.nutri.avaliacao.*
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

/** Aba "Meus resultados" do paciente: só o que a nutri compartilhou, em frases simples (sem siglas) e sempre com texto junto da cor. */
@Composable
fun MeusResultadosRota(avaliacoes: AvaliacoesViewModel) {
    val estado by avaliacoes.estado.collectAsState()
    LaunchedEffect(Unit) { avaliacoes.abrirComoPaciente() }
    MeusResultadosTela(meusResultados(estado.itens))
}

private fun estiloSemaforo(s: Semaforo) = when (s) { Semaforo.BOM -> EstiloStatus.FEITA; Semaforo.ATENCAO -> EstiloStatus.INFO; Semaforo.ALERTA -> EstiloStatus.ALERTA }

@Composable
private fun textoSemaforo(s: Semaforo) = stringResource(when (s) {
    Semaforo.BOM -> Res.string.resultados_semaforo_bom
    Semaforo.ATENCAO -> Res.string.resultados_semaforo_atencao
    Semaforo.ALERTA -> Res.string.resultados_semaforo_alerta
})

@Composable
private fun valorIndicador(i: Indicador): String = when (i.tipo) {
    TipoIndicador.PESO_ALTURA -> i.imc?.let { stringResource(Res.string.resultados_valor_imc, i.valor?.let(::textoDecimal).orEmpty(), stringResource(rotuloClassificacao(it))) }.orEmpty()
    TipoIndicador.CINTURA -> stringResource(Res.string.resultados_cintura_valor, i.valor?.let(::textoDecimal).orEmpty(), stringResource(when (i.cintura) {
        org.br.dietamais.dominio.calculos.ClasseCintura.B -> Res.string.resultados_cintura_b
        org.br.dietamais.dominio.calculos.ClasseCintura.A -> Res.string.resultados_cintura_a
        else -> Res.string.resultados_cintura_ma
    }))
    TipoIndicador.GORDURA -> stringResource(Res.string.resultados_gordura_valor, i.valor?.let(::textoDecimal).orEmpty(), i.gordura?.let { stringResource(rotuloClassePercentual(it)) }.orEmpty())
    TipoIndicador.RISCO -> i.risco?.let { stringResource(rotuloRisco(it)) }.orEmpty()
}

@Composable
private fun rotuloIndicador(t: TipoIndicador) = stringResource(when (t) {
    TipoIndicador.PESO_ALTURA -> Res.string.resultados_ind_peso_altura
    TipoIndicador.CINTURA -> Res.string.resultados_ind_cintura
    TipoIndicador.GORDURA -> Res.string.resultados_ind_gordura
    TipoIndicador.RISCO -> Res.string.resultados_ind_risco
})

@Composable
private fun rotuloParte(p: ParteCorpo) = stringResource(when (p) {
    ParteCorpo.GORDURA -> Res.string.avaliacao_rosca_gordura
    ParteCorpo.MASSA_MAGRA -> Res.string.avaliacao_rosca_magro
    ParteCorpo.MUSCULO -> Res.string.avaliacao_rosca_musculo
    ParteCorpo.OSSO -> Res.string.avaliacao_rosca_osso
    ParteCorpo.RESIDUAL -> Res.string.avaliacao_rosca_residual
})

@Composable
fun MeusResultadosTela(resultados: MeusResultados?, modifier: Modifier = Modifier) {
    if (resultados == null) {
        Text(stringResource(Res.string.resultados_vazio), style = MaterialTheme.typography.bodyLarge, modifier = modifier.padding(vertical = Dimens.espacoMedio))
        return
    }
    val data = formatarData(resultados.data.toLocalDateTime(TimeZone.currentSystemDefault()).date)
    Column(modifier.verticalScroll(rememberScrollState()).padding(vertical = Dimens.espacoMedio), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
        Text(stringResource(Res.string.resultados_data, data), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        resultados.textoResumo?.let {
            CardDieta(Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.resultados_conversa), style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }
        resultados.indicadores.forEach { i ->
            CardDieta(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(rotuloIndicador(i.tipo), style = MaterialTheme.typography.titleSmall)
                    ChipStatus(estiloSemaforo(i.semaforo), texto = textoSemaforo(i.semaforo))
                }
                Text(valorIndicador(i), style = MaterialTheme.typography.bodyLarge)
            }
        }
        if (resultados.composicao.isNotEmpty()) CardDieta(Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.resultados_composicao), style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
            val itens = resultados.composicao.map { (p, kg) -> ItemGrafico(rotuloParte(p), kg) }
            GraficoComTabela(itens.map { it.rotulo to "${textoDecimal(it.valor)} kg" }) {
                GraficoRosca(itens, centro = resultados.percentualGordura?.let { "${textoDecimal(it)}%" })
            }
        }
        resultados.meta?.let { m ->
            CardDieta(Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.resultados_meta_titulo), style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
                Text(stringResource(Res.string.resultados_meta_linha, textoDecimal(m.percentualDesejado), textoDecimal(m.pesoMetaKg)), style = MaterialTheme.typography.bodyLarge)
                Text(if (m.faltamKg > 0) stringResource(Res.string.resultados_meta_faltam, textoDecimal(m.faltamKg)) else stringResource(Res.string.resultados_meta_atingida), style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (resultados.seriePeso.size >= 2) CardDieta(Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.resultados_evolucao), style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
            GraficoLinhas(
                listOf(SerieLinha(stringResource(Res.string.resultados_serie_peso), resultados.seriePeso)), "kg",
                faixa = resultados.pesoSaudavel?.let { FaixaSombreada(stringResource(Res.string.resultados_faixa_saudavel), it.start, it.endInclusive) },
            )
        }
    }
}
