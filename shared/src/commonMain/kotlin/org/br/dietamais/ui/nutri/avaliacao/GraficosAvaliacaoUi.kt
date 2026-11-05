package org.br.dietamais.ui.nutri.avaliacao

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import org.br.dietamais.data.remoto.dto.AvaliacaoResposta
import org.br.dietamais.dominio.avaliacao.*
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.CardDieta
import org.br.dietamais.ui.comum.graficos.GraficoBarrasVerticais
import org.br.dietamais.ui.comum.graficos.GraficoComTabela
import org.br.dietamais.ui.comum.graficos.ItemGrafico
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private fun tituloSerie(serie: SerieGraficoAvaliacao): StringResource = when (serie) {
    SerieGraficoAvaliacao.PESO -> Res.string.grafico_av_peso
    SerieGraficoAvaliacao.PERIMETROS -> Res.string.grafico_av_perimetros
    SerieGraficoAvaliacao.DOBRAS -> Res.string.grafico_av_dobras
    SerieGraficoAvaliacao.COMPOSICAO -> Res.string.grafico_av_composicao
}

private fun rotuloChave(chave: ChaveGrafico): StringResource = when (chave) {
    ChaveGrafico.Peso.MINIMO -> Res.string.grafico_av_peso_minimo
    ChaveGrafico.Peso.ATUAL -> Res.string.grafico_av_peso_atual
    ChaveGrafico.Peso.MAXIMO -> Res.string.grafico_av_peso_maximo
    is ChaveGrafico.Medida -> when (chave.medida) {
        MedidaAvaliacao.PESO_GORDO -> Res.string.grafico_av_peso_gordo
        MedidaAvaliacao.PESO_MAGRO -> Res.string.grafico_av_peso_magro
        else -> rotuloMedida(chave.medida)
    }
}

/**
 * Os 4 gráficos de barras do PDF da avaliação (Peso, Perímetros, Dobras, Composição), cada um com "ver como tabela" (ART-P1-09 e ART-P1-10, graficos.md).
 * Só mostra a série que tem dados; sem nenhuma, não desenha nada.
 */
@Composable
fun GraficosAvaliacao(avaliacao: AvaliacaoResposta, modifier: Modifier = Modifier) {
    val graficos = graficosDaAvaliacao(avaliacao)
    if (graficos.isEmpty()) return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
        graficos.forEach { g ->
            CardDieta {
                Text(stringResource(tituloSerie(g.serie)), style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
                val itens = g.pontos.map { ItemGrafico(stringResource(rotuloChave(it.chave)), it.valor, sigla = (it.chave as? ChaveGrafico.Medida)?.let { c -> siglaMedida(c.medida) }) }
                GraficoComTabela(itens.map { it.rotulo to "${textoDecimal(it.valor)} ${g.serie.unidade}" }) {
                    GraficoBarrasVerticais(itens, unidade = g.serie.unidade)
                }
            }
        }
    }
}
