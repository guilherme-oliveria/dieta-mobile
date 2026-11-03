package org.br.dietamais.ui.comum.graficos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.br.dietamais.dominio.graficos.*
import org.br.dietamais.ui.theme.*

@Preview(name = "Gráficos compacto", widthDp = TamanhosPreview.compacto, heightDp = TamanhosPreview.altura)
@Preview(name = "Gráficos médio", widthDp = TamanhosPreview.medio, heightDp = TamanhosPreview.altura)
@Preview(name = "Gráficos expandido", widthDp = TamanhosPreview.expandido, heightDp = TamanhosPreview.altura)
@Composable
private fun GraficosPreview() = DietaTheme { GaleriaGraficos() }

/** Amostra com dados falsos para previews e conferência visual (DES-08). */
@Composable
internal fun GaleriaGraficos() {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(Dimens.margemCompacta), verticalArrangement = Arrangement.spacedBy(Dimens.margemAmpla)) {
            val macros = listOf(ItemGrafico("Carboidrato", 52.0), ItemGrafico("Proteína", 20.0), ItemGrafico("Gordura", 28.0))
            GraficoComTabela(macros.map { it.rotulo to "${it.valor.toInt()}%" }) { GraficoRosca(macros, centro = "2509 kcal", anelMeta = listOf(ItemGrafico("Carboidrato", 55.0), ItemGrafico("Proteína", 20.0), ItemGrafico("Gordura", 25.0))) }
            GraficoBarras(listOf(ItemGrafico("Desjejum", 420.0), ItemGrafico("Almoço", 800.0), ItemGrafico("Jantar", 650.0)), unidade = "kcal", meta = 700.0)
            ReguaFaixas(
                listOf(TrechoRegua(SegmentoFaixa("Baixo", 10.0, 18.5), InfoDieta), TrechoRegua(SegmentoFaixa("Normal", 18.5, 25.0), VerdeDieta), TrechoRegua(SegmentoFaixa("Sobrepeso", 25.0, 30.0), AlertaDieta.copy(alpha = 0.6f)), TrechoRegua(SegmentoFaixa("Obesidade", 30.0, 40.0), AlertaDieta)),
                27.5, "kg/m²",
            )
            Medidor(26.5, 0.0, 50.0, "% de gordura", "%", faixaIdeal = 14.0..25.0)
            GraficoLinhas(listOf(SerieLinha("Peso", listOf(0.0 to 104.0, 30.0 to 102.5, 60.0 to 100.8)), SerieLinha("Peso magro", listOf(0.0 to 74.0, 30.0 to 75.4, 60.0 to 76.1))), "kg", faixa = FaixaSombreada("Peso teórico", 68.9, 92.8))
            CurvaCrescimento(
                listOf("P3" to listOf(PontoCurva(0, 2.5), PontoCurva(12, 7.5), PontoCurva(24, 9.5)), "P50" to listOf(PontoCurva(0, 3.3), PontoCurva(12, 9.6), PontoCurva(24, 12.2)), "P97" to listOf(PontoCurva(0, 4.4), PontoCurva(12, 12.0), PontoCurva(24, 15.3))),
                PontoCurva(15, 10.4), "kg",
            )
            Text("Dados fictícios", style = MaterialTheme.typography.labelSmall)
        }
    }
}
