package org.br.dietamais.ui.comum.graficos

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.br.dietamais.dominio.graficos.*
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.theme.*
import org.jetbrains.compose.resources.stringResource

/** Cores de série, em ordem. O laranja de alerta fica de fora: só vale para avisos (docs/design/componentes.md). */
object PaletaGraficos {
    val serie: List<Color> = listOf(SerieVerde, SerieAzul, SerieAmbar, SerieRoxo, SerieTurquesa, SerieCinza)
    fun cor(indice: Int): Color = serie[indice.mod(serie.size)]
}

/** Item de gráfico de rosca e de barras. [cor] nulo usa a paleta pela posição. */
data class ItemGrafico(val rotulo: String, val valor: Double, val cor: Color? = null)

private fun Double.texto(): String {
    val arredondado = kotlin.math.round(this * 100) / 100
    val inteiro = arredondado.toLong()
    return if (arredondado == inteiro.toDouble()) inteiro.toString() else arredondado.toString().replace('.', ',')
}

/** Progresso 0→1 de uma animação curta de entrada (600 ms). */
@Composable
private fun progressoEntrada(): Float {
    val progresso = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progresso.animateTo(1f, tween(600)) }
    return progresso.value
}

/** Legenda com bolinha de cor, rótulo e valor. */
@Composable
private fun Legenda(itens: List<Triple<String, String, Color>>, modifier: Modifier = Modifier, empilhada: Boolean = false) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Dimens.espacoMinimo)) {
        itens.forEach { (rotulo, valor, cor) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                Canvas(Modifier.size(10.dp)) { drawCircle(cor) }
                if (empilhada) Column(Modifier.weight(1f)) {
                    Text(rotulo, style = MaterialTheme.typography.bodySmall)
                    Text(valor, style = MaterialTheme.typography.labelLarge)
                } else {
                    Text(rotulo, style = MaterialTheme.typography.bodyMedium)
                    Text(valor, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

/** Alterna entre o gráfico e uma tabela de linhas (rótulo, valor): é o "ver como tabela" para leitores de tela e para quem prefere números. */
@Composable
fun GraficoComTabela(linhas: List<Pair<String, String>>, modifier: Modifier = Modifier, grafico: @Composable () -> Unit) {
    var tabela by remember { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
        if (tabela) Column(verticalArrangement = Arrangement.spacedBy(Dimens.espacoMinimo)) {
            linhas.forEach { (rotulo, valor) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(rotulo, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium); Text(valor, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                }
            }
        } else grafico()
        TextButton(onClick = { tabela = !tabela }) {
            Text(stringResource(if (tabela) Res.string.grafico_ocultar_tabela else Res.string.grafico_ver_tabela))
        }
    }
}

/**
 * Rosca (pizza com furo) com texto no centro e, opcionalmente, um anel externo de faixa recomendada ([anelMeta], em %).
 * A descrição de acessibilidade lista cada fatia com o percentual.
 */
@Composable
fun GraficoRosca(itens: List<ItemGrafico>, modifier: Modifier = Modifier, centro: String? = null, anelMeta: List<ItemGrafico>? = null, tamanho: androidx.compose.ui.unit.Dp = 180.dp,
    valoresLegenda: List<String>? = null, descricaoAcessivel: String? = null) {
    val fatias = fatiasRosca(itens.map { it.rotulo to it.valor })
    val descricao = descricaoAcessivel ?: if (fatias.isEmpty()) stringResource(Res.string.grafico_sem_dados)
    else fatias.joinToString("; ") { "${it.rotulo}: ${it.percentual.texto()}%" }
    val metaFatias = anelMeta?.let { fatiasRosca(it.map { i -> i.rotulo to i.valor }) }.orEmpty()
    val progresso = progressoEntrada()
    val fundo = MaterialTheme.colorScheme.outlineVariant
    val corTexto = MaterialTheme.typography.titleMedium
    Row(modifier.semantics { contentDescription = descricao }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
        Box(Modifier.size(tamanho), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val espessura = size.minDimension * 0.16f
                val folga = if (metaFatias.isNotEmpty()) espessura * 1.5f else espessura / 2
                val lado = size.minDimension - 2 * folga
                val topo = Offset((size.width - lado) / 2, (size.height - lado) / 2)
                if (fatias.isEmpty()) drawArc(fundo, 0f, 360f, false, topo, Size(lado, lado), style = Stroke(espessura))
                fatias.forEach { f ->
                    drawArc(itens[f.indice].cor ?: PaletaGraficos.cor(f.indice), f.inicioGraus, f.varreduraGraus * progresso, false, topo, Size(lado, lado), style = Stroke(espessura))
                }
                if (metaFatias.isNotEmpty()) {
                    val externo = size.minDimension - espessura * 0.5f
                    val topoExterno = Offset((size.width - externo) / 2, (size.height - externo) / 2)
                    metaFatias.forEach { f ->
                        drawArc((anelMeta!![f.indice].cor ?: PaletaGraficos.cor(f.indice)).copy(alpha = 0.35f), f.inicioGraus, f.varreduraGraus * progresso, false, topoExterno, Size(externo, externo), style = Stroke(espessura * 0.4f))
                    }
                }
            }
            if (centro != null) Text(centro, style = corTexto)
        }
        Legenda(itens.mapIndexed { i, item -> Triple(item.rotulo, valoresLegenda?.getOrNull(i) ?: "${fatias.firstOrNull { it.indice == i }?.percentual?.texto() ?: "0"}%", item.cor ?: PaletaGraficos.cor(i)) }, Modifier.weight(1f), empilhada = valoresLegenda != null)
    }
}

/** Barras horizontais com valor ao lado e, se houver, uma marca de [meta]. */
@Composable
fun GraficoBarras(itens: List<ItemGrafico>, modifier: Modifier = Modifier, unidade: String = "", meta: Double? = null) {
    val maximo = (itens.map { it.valor } + listOfNotNull(meta)).filter { it.isFinite() }.maxOrNull()?.takeIf { it > 0 } ?: 1.0
    val progresso = progressoEntrada()
    val descricao = itens.joinToString("; ") { "${it.rotulo}: ${it.valor.texto()} $unidade".trim() } + (meta?.let { "; ${stringResource(Res.string.grafico_meta)}: ${it.texto()} $unidade" } ?: "")
    val trilho = MaterialTheme.colorScheme.surfaceVariant
    val corMeta = MaterialTheme.colorScheme.onSurface
    Column(modifier.semantics { contentDescription = descricao }, verticalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
        itens.forEachIndexed { i, item ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(item.rotulo, style = MaterialTheme.typography.bodyMedium); Text("${item.valor.texto()} $unidade".trim(), style = MaterialTheme.typography.labelLarge)
                }
                Canvas(Modifier.fillMaxWidth().height(12.dp)) {
                    val raio = CornerRadius(size.height / 2)
                    drawRoundRect(trilho, size = size, cornerRadius = raio)
                    val largura = (item.valor / maximo).coerceIn(0.0, 1.0).toFloat() * size.width * progresso
                    if (largura > 0f) drawRoundRect(item.cor ?: PaletaGraficos.cor(i), size = Size(largura, size.height), cornerRadius = raio)
                    if (meta != null) { val x = (meta / maximo).coerceIn(0.0, 1.0).toFloat() * size.width; drawLine(corMeta, Offset(x, 0f), Offset(x, size.height), 2.dp.toPx()) }
                }
            }
        }
    }
}

/** Faixas da régua de IMC (OMS) com as cores de faixa do padrão; [rotulos] são, em ordem, abaixo, normal, sobrepeso e obesidade. */
fun trechosReguaImc(rotulos: List<String>): List<TrechoRegua> = listOf(
    TrechoRegua(SegmentoFaixa(rotulos[0], 10.0, 18.5), FaixaAbaixo), TrechoRegua(SegmentoFaixa(rotulos[1], 18.5, 25.0), FaixaNormal),
    TrechoRegua(SegmentoFaixa(rotulos[2], 25.0, 30.0), FaixaSobrepeso), TrechoRegua(SegmentoFaixa(rotulos[3], 30.0, 40.0), FaixaObesidade),
)

/** Trecho colorido de [ReguaFaixas]. */
data class TrechoRegua(val segmento: SegmentoFaixa, val cor: Color)

/** Régua de faixas (IMC, exames) com um marcador no valor. A faixa do valor vira texto ao lado, sem depender de cor. */
@Composable
fun ReguaFaixas(trechos: List<TrechoRegua>, valor: Double?, unidade: String, modifier: Modifier = Modifier) {
    val segmentos = trechos.map { it.segmento }
    val atual = valor?.let { segmentoDoValor(it, segmentos) }
    val descricao = if (valor == null) stringResource(Res.string.grafico_sem_dados)
    else "${valor.texto()} $unidade" + (atual?.let { ": ${it.rotulo}" } ?: "") + ". " + segmentos.joinToString(", ") { "${it.rotulo} de ${it.de.texto()} a ${it.ate.texto()}" }
    val larguras = largurasRegua(segmentos)
    val marcador = MaterialTheme.colorScheme.onSurface
    val progresso = progressoEntrada()
    Column(modifier.semantics { contentDescription = descricao }, verticalArrangement = Arrangement.spacedBy(Dimens.espacoMinimo)) {
        if (valor != null) Text("${valor.texto()} $unidade" + (atual?.let { " · ${it.rotulo}" } ?: ""), style = MaterialTheme.typography.titleMedium)
        Canvas(Modifier.fillMaxWidth().height(28.dp)) {
            var x = 0f
            val barraTopo = size.height * 0.35f; val barraAltura = size.height * 0.4f
            trechos.forEachIndexed { i, t ->
                val w = larguras[i] * size.width
                drawRect(t.cor, Offset(x, barraTopo), Size(w, barraAltura)); x += w
            }
            if (valor != null) {
                val px = posicaoNaRegua(valor, segmentos) * size.width * progresso
                drawLine(marcador, Offset(px, 0f), Offset(px, size.height), 3.dp.toPx(), StrokeCap.Round)
                drawCircle(marcador, 5.dp.toPx(), Offset(px, 4.dp.toPx()))
            }
        }
        Row(Modifier.fillMaxWidth()) {
            segmentos.forEachIndexed { i, s -> Text(s.rotulo, Modifier.weight(larguras[i].coerceAtLeast(0.05f)), style = MaterialTheme.typography.labelSmall) }
        }
    }
}

/** Medidor em semicírculo (ex.: % de gordura) com uma faixa ideal destacada e o valor no centro. */
@Composable
fun Medidor(valor: Double?, minimo: Double, maximo: Double, rotulo: String, unidade: String, modifier: Modifier = Modifier, faixaIdeal: ClosedFloatingPointRange<Double>? = null, corValor: Color = VerdeDieta) {
    val descricao = if (valor == null) "$rotulo: ${stringResource(Res.string.grafico_sem_dados)}" else "$rotulo: ${valor.texto()} $unidade, de ${minimo.texto()} a ${maximo.texto()}" +
        (faixaIdeal?.let { ", faixa ideal ${it.start.texto()} a ${it.endInclusive.texto()}" } ?: "")
    val progresso = progressoEntrada()
    val trilho = MaterialTheme.colorScheme.outlineVariant
    Column(modifier.semantics { contentDescription = descricao }, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.width(200.dp).height(112.dp), contentAlignment = Alignment.BottomCenter) {
            Canvas(Modifier.fillMaxSize()) {
                val espessura = 18.dp.toPx(); val lado = size.width - espessura; val topo = Offset(espessura / 2, espessura / 2)
                drawArc(trilho, 180f, 180f, false, topo, Size(lado, lado), style = Stroke(espessura, cap = StrokeCap.Round))
                if (faixaIdeal != null) {
                    val a = fracaoNaFaixa(faixaIdeal.start, minimo, maximo); val b = fracaoNaFaixa(faixaIdeal.endInclusive, minimo, maximo)
                    drawArc(corValor.copy(alpha = 0.3f), 180f + 180f * a, 180f * (b - a), false, topo, Size(lado, lado), style = Stroke(espessura))
                }
                if (valor != null) drawArc(corValor, 180f, 180f * fracaoNaFaixa(valor, minimo, maximo) * progresso, false, topo, Size(lado, lado), style = Stroke(espessura / 2, cap = StrokeCap.Round))
            }
            Text(valor?.let { "${it.texto()} $unidade" } ?: "–", style = MaterialTheme.typography.titleLarge)
        }
        Text(rotulo, style = MaterialTheme.typography.labelMedium)
    }
}

/** Uma série de [GraficoLinhas]. Pontos têm x numérico (ex.: dias desde a primeira avaliação) e y. */
data class SerieLinha(val rotulo: String, val pontos: List<Pair<Double, Double>>, val cor: Color? = null)

/** Faixa de referência sombreada entre [de] e [ate] (ex.: peso teórico mínimo e máximo). */
data class FaixaSombreada(val rotulo: String, val de: Double, val ate: Double)

/** Gráfico de linhas com várias séries, faixa sombreada e legenda. Cada ponto é desenhado; a tabela cobre o toque/leitor de tela. */
@Composable
fun GraficoLinhas(series: List<SerieLinha>, unidade: String, modifier: Modifier = Modifier, faixa: FaixaSombreada? = null, altura: androidx.compose.ui.unit.Dp = 180.dp) {
    val todos = series.flatMap { it.pontos }
    val (y0, y1) = limitesEixo(todos.map { it.second } + listOfNotNull(faixa?.de, faixa?.ate))
    val x0 = todos.minOfOrNull { it.first } ?: 0.0; val x1 = todos.maxOfOrNull { it.first } ?: 1.0
    val progresso = progressoEntrada()
    val grade = MaterialTheme.colorScheme.outlineVariant
    val descricao = if (todos.isEmpty()) stringResource(Res.string.grafico_sem_dados) else series.joinToString("; ") { s ->
        "${s.rotulo}: ${s.pontos.firstOrNull()?.second?.texto().orEmpty()} a ${s.pontos.lastOrNull()?.second?.texto().orEmpty()} $unidade"
    } + (faixa?.let { "; ${it.rotulo} de ${it.de.texto()} a ${it.ate.texto()}" } ?: "")
    Column(modifier.semantics { contentDescription = descricao }, verticalArrangement = Arrangement.spacedBy(Dimens.espacoMinimo)) {
        Text("${y1.texto()} $unidade", style = MaterialTheme.typography.labelSmall)
        Canvas(Modifier.fillMaxWidth().height(altura)) {
            val margem = 8.dp.toPx(); val w = size.width - 2 * margem; val h = size.height - 2 * margem
            fun ponto(p: Pair<Double, Double>) = Offset(
                margem + (if (x1 == x0) 0.5f else ((p.first - x0) / (x1 - x0)).toFloat()) * w,
                margem + (1f - ((p.second - y0) / (y1 - y0)).toFloat()) * h,
            )
            drawLine(grade, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx()); drawLine(grade, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx())
            if (faixa != null) {
                val topo = margem + (1f - ((faixa.ate - y0) / (y1 - y0)).toFloat()) * h; val base = margem + (1f - ((faixa.de - y0) / (y1 - y0)).toFloat()) * h
                drawRect(VerdeDieta.copy(alpha = 0.12f), Offset(0f, topo), Size(size.width, base - topo))
            }
            series.forEachIndexed { i, s ->
                val cor = s.cor ?: PaletaGraficos.cor(i)
                val visiveis = s.pontos.sortedBy { it.first }.map { ponto(it) }
                if (visiveis.size > 1) {
                    val caminho = Path(); visiveis.forEachIndexed { k, o -> if (k == 0) caminho.moveTo(o.x, o.y) else caminho.lineTo(o.x, o.y) }
                    drawPath(caminho, cor, alpha = progresso, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
                }
                visiveis.forEach { drawCircle(cor, 4.dp.toPx() * progresso, it) }
            }
        }
        Text("${y0.texto()} $unidade", style = MaterialTheme.typography.labelSmall)
        if (series.size > 1 || faixa != null) Legenda(series.mapIndexed { i, s -> Triple(s.rotulo, "", s.cor ?: PaletaGraficos.cor(i)) } + listOfNotNull(faixa?.let { Triple(it.rotulo, "", VerdeDieta.copy(alpha = 0.3f)) }))
    }
}

/** Curva de crescimento (OMS): linhas de percentil/escore Z (do mais baixo ao mais alto) e o ponto da criança. */
@Composable
fun CurvaCrescimento(curvas: List<Pair<String, List<PontoCurva>>>, ponto: PontoCurva?, unidade: String, modifier: Modifier = Modifier, altura: androidx.compose.ui.unit.Dp = 220.dp) {
    val todos = curvas.flatMap { it.second } + listOfNotNull(ponto)
    val (y0, y1) = limitesEixo(todos.map { it.valor })
    val x0 = todos.minOfOrNull { it.mes } ?: 0; val x1 = todos.maxOfOrNull { it.mes } ?: 1
    val progresso = progressoEntrada()
    val grade = MaterialTheme.colorScheme.outlineVariant
    val destaque = AlertaDieta
    val posicao = ponto?.let { p -> curvaAbaixo(p.valor, p.mes, curvas.map { it.second }).let { i -> if (i < 0) "abaixo de ${curvas.firstOrNull()?.first.orEmpty()}" else "a partir de ${curvas[i].first}" } }
    val descricao = if (todos.isEmpty()) stringResource(Res.string.grafico_sem_dados) else
        (ponto?.let { "Ponto: ${it.valor.texto()} $unidade aos ${it.mes} meses, $posicao. " } ?: "") + "Curvas: " + curvas.joinToString(", ") { it.first }
    Column(modifier.semantics { contentDescription = descricao }, verticalArrangement = Arrangement.spacedBy(Dimens.espacoMinimo)) {
        Canvas(Modifier.fillMaxWidth().height(altura)) {
            val margem = 8.dp.toPx(); val w = size.width - 2 * margem; val h = size.height - 2 * margem
            fun xy(mes: Int, valor: Double) = Offset(margem + (if (x1 == x0) 0.5f else (mes - x0).toFloat() / (x1 - x0)) * w, margem + (1f - ((valor - y0) / (y1 - y0)).toFloat()) * h)
            drawLine(grade, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
            val meio = curvas.size / 2
            curvas.forEachIndexed { i, (_, pts) ->
                val ordenados = pts.sortedBy { it.mes }
                if (ordenados.size > 1) {
                    val caminho = Path(); ordenados.forEachIndexed { k, p -> val o = xy(p.mes, p.valor); if (k == 0) caminho.moveTo(o.x, o.y) else caminho.lineTo(o.x, o.y) }
                    drawPath(caminho, if (i == meio) VerdeDieta else VerdeDieta.copy(alpha = 0.4f), alpha = progresso, style = Stroke(if (i == meio) 2.5.dp.toPx() else 1.dp.toPx()))
                }
            }
            if (ponto != null) { val o = xy(ponto.mes, ponto.valor); drawCircle(Color.White, 7.dp.toPx(), o); drawCircle(destaque, 5.dp.toPx(), o) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("$x0 m", style = MaterialTheme.typography.labelSmall); Text("$x1 m", style = MaterialTheme.typography.labelSmall)
        }
    }
}
