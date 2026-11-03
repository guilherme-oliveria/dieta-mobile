package org.br.dietamais.ui.paciente.evolucao

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.br.dietamais.dominio.avaliacao.PontoSerie
import org.br.dietamais.dominio.avaliacao.textoDecimal
import org.br.dietamais.ui.nutri.avaliacao.textoData
import org.br.dietamais.ui.theme.Dimens

/** Posição de cada ponto no quadro [0,1]×[0,1] (y para cima). Um valor constante fica no meio; um único ponto fica no centro. */
fun normalizar(pontos: List<PontoSerie>): List<Pair<Float, Float>> {
    if (pontos.isEmpty()) return emptyList()
    val t0 = pontos.first().instante.toEpochMilliseconds(); val t1 = pontos.last().instante.toEpochMilliseconds()
    val minimo = pontos.minOf { it.valor }; val maximo = pontos.maxOf { it.valor }
    return pontos.map { p ->
        val x = if (t1 == t0) 0.5f else ((p.instante.toEpochMilliseconds() - t0).toDouble() / (t1 - t0)).toFloat()
        val y = if (maximo == minimo) 0.5f else ((p.valor - minimo) / (maximo - minimo)).toFloat()
        x to y
    }
}

/** Gráfico de linha desenhado em Canvas, com mínimo/máximo e datas nas pontas. A descrição para leitores de tela resume o período. */
@Composable
fun GraficoLinha(pontos: List<PontoSerie>, rotulo: String, unidade: String, modifier: Modifier = Modifier) {
    val cor = MaterialTheme.colorScheme.primary
    val grade = MaterialTheme.colorScheme.outlineVariant
    val minimo = pontos.minOfOrNull { it.valor }; val maximo = pontos.maxOfOrNull { it.valor }
    val descricao = if (pontos.isEmpty()) rotulo else
        "$rotulo: ${textoDecimal(pontos.first().valor)} a ${textoDecimal(pontos.last().valor)} $unidade, ${textoData(pontos.first().instante)} a ${textoData(pontos.last().instante)}"
    Column(modifier.semantics { contentDescription = descricao }, verticalArrangement = Arrangement.spacedBy(Dimens.espacoMinimo)) {
        if (maximo != null) Text("${textoDecimal(maximo)} $unidade", style = MaterialTheme.typography.labelSmall)
        val normalizados = normalizar(pontos)
        Canvas(Modifier.fillMaxWidth().height(180.dp)) {
            val margem = 8.dp.toPx()
            val largura = size.width - 2 * margem; val altura = size.height - 2 * margem
            fun ponto(p: Pair<Float, Float>) = Offset(margem + p.first * largura, margem + (1f - p.second) * altura)
            drawLine(grade, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
            drawLine(grade, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx())
            if (normalizados.size > 1) {
                val caminho = Path()
                normalizados.forEachIndexed { i, p -> val o = ponto(p); if (i == 0) caminho.moveTo(o.x, o.y) else caminho.lineTo(o.x, o.y) }
                drawPath(caminho, cor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
            }
            normalizados.forEach { drawCircle(cor, radius = 4.dp.toPx(), center = ponto(it)) }
        }
        if (minimo != null) Text("${textoDecimal(minimo)} $unidade", style = MaterialTheme.typography.labelSmall)
        if (pontos.isNotEmpty()) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(textoData(pontos.first().instante), style = MaterialTheme.typography.labelSmall)
            if (pontos.size > 1) Text(textoData(pontos.last().instante), style = MaterialTheme.typography.labelSmall)
        }
    }
}
