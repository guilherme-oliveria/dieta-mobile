package org.br.dietamais.ui.comum.graficos

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.br.dietamais.dominio.avaliacao.*
import org.br.dietamais.ui.theme.*

/** Somente pontos publicados: não desenha segmentos em meses sem referência congelada. */
@Composable
fun CurvaCrescimentoConfirmada(serie: SerieCrescimento, unidade: String, meses: String, medida: String, modifier: Modifier=Modifier) {
    val valores=serie.pontos.map { it.valor }+serie.referencias.flatMap { it.cortes }
    val minimo=valores.minOrNull() ?: 0.0
    val maximo=valores.maxOrNull() ?: 1.0
    val margem=((maximo-minimo)*0.1).coerceAtLeast(0.5)
    val y0=minimo-margem; val y1=maximo+margem
    val idades=serie.pontos.map { it.idadeMeses }+serie.referencias.map { it.idadeMeses }
    val x0=(idades.minOrNull() ?: 0)-1
    val x1=(idades.maxOrNull() ?: 1)+1
    val grade=MaterialTheme.colorScheme.outlineVariant
    val fundo=MaterialTheme.colorScheme.surface
    val descricao=serie.pontos.joinToString("; ") { "$medida: ${textoDecimal(it.valor)} $unidade, ${it.idadeMeses} $meses" }
    Column(modifier.semantics { contentDescription=descricao }) {
        Text("${textoDecimal(y1)} $unidade",style=MaterialTheme.typography.labelSmall)
        Canvas(Modifier.fillMaxWidth().height(220.dp)) {
            val borda=12.dp.toPx(); val w=(size.width-2*borda).coerceAtLeast(1f); val h=size.height-2*borda
            fun xy(m:Int,v:Double)=Offset(borda+(m-x0).toFloat()/(x1-x0)*w,borda+(1-(v-y0)/(y1-y0)).toFloat()*h)
            repeat(4) { i -> val y=borda+h*i/3; drawLine(grade,Offset(borda,y),Offset(size.width-borda,y),1.dp.toPx()) }
            serie.referencias.zipWithNext().forEach { (a,b) ->
                if(b.idadeMeses == a.idadeMeses+1) a.cortes.indices.forEach { i ->
                    val mediana=percentisCrescimento[i]=="P50"
                    drawLine(if(mediana) VerdeDieta else SerieCinza,xy(a.idadeMeses,a.cortes[i]),xy(b.idadeMeses,b.cortes[i]),
                        if(mediana) 2.5.dp.toPx() else 1.5.dp.toPx(),
                        pathEffect=if(mediana) null else PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(),3.dp.toPx())))
                }
            }
            serie.referencias.forEach { r -> r.cortes.forEachIndexed { i,v ->
                val o=xy(r.idadeMeses,v)
                val mediana=percentisCrescimento[i]=="P50"
                // Uma marca por mês disponível; sem sugerir cortes para meses ausentes.
                drawLine(if(mediana) VerdeDieta else SerieCinza,Offset(o.x-6.dp.toPx(),o.y),Offset(o.x+6.dp.toPx(),o.y),
                    if(mediana) 2.5.dp.toPx() else 1.5.dp.toPx(), pathEffect=if(mediana) null else PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(),2.dp.toPx())))
            } }
            serie.pontos.forEach { p -> val o=xy(p.idadeMeses,p.valor); val selecionado=p.avaliacaoId==serie.selecionado.avaliacaoId
                drawCircle(fundo,if(selecionado) 8.dp.toPx() else 6.dp.toPx(),o)
                drawCircle(SerieAzul,if(selecionado) 6.dp.toPx() else 4.dp.toPx(),o)
                if(selecionado) drawCircle(SerieAzul,10.dp.toPx(),o,style=Stroke(1.dp.toPx()))
            }
        }
        Text("${textoDecimal(y0)} $unidade",style=MaterialTheme.typography.labelSmall)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Text("${x0.coerceAtLeast(0)} $meses",style=MaterialTheme.typography.labelSmall)
            Text("$x1 $meses",style=MaterialTheme.typography.labelSmall)
        }
    }
}
