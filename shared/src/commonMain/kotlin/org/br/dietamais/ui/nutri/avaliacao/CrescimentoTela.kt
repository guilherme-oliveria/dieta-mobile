package org.br.dietamais.ui.nutri.avaliacao

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import org.br.dietamais.data.remoto.dto.AvaliacaoResposta
import org.br.dietamais.dominio.avaliacao.*
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.CardDieta
import org.br.dietamais.ui.comum.graficos.GraficoComTabela
import org.br.dietamais.ui.comum.graficos.CurvaCrescimentoConfirmada
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

/** Lê somente os snapshots já autorizados e armazenados, inclusive offline. */
@Composable
fun CrescimentoTela(avaliacoes: List<AvaliacaoResposta>, modifier: Modifier = Modifier, rolavel: Boolean = true) {
    val disponiveis = avaliacoes.filter { a -> indicadoresCrescimento.any { crescimento(listOf(a), a.id, it) != null } }.sortedByDescending { it.dataHora }
    var id by remember { mutableStateOf(disponiveis.firstOrNull()?.id) }
    val selecionada = disponiveis.firstOrNull { it.id == id } ?: disponiveis.firstOrNull()
    var indicador by remember { mutableStateOf("PESO_IDADE") }
    val indicadores = indicadoresCrescimento.filter { selecionada != null && crescimento(listOf(selecionada),selecionada.id,it) != null }
    val escolhido = indicador.takeIf { it in indicadores } ?: indicadores.firstOrNull()
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = Dimens.larguraLeitura).fillMaxWidth()
            .then(if (rolavel) Modifier.verticalScroll(rememberScrollState()) else Modifier)
            .padding(Dimens.espacoMedio), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
            Text(stringResource(Res.string.crescimento_titulo), style=MaterialTheme.typography.titleLarge, modifier=Modifier.semantics { heading() })
            if (selecionada == null || escolhido == null) {
                Text(stringResource(Res.string.crescimento_vazio)); return@Column
            }
            FlowRow(horizontalArrangement=Arrangement.spacedBy(Dimens.espacoPequeno)) {
                indicadores.forEach { codigo -> FilterChip(codigo == escolhido, { indicador=codigo }, label={ Text(rotuloCrescimento(codigo)) }) }
            }
            var datas by remember { mutableStateOf(false) }
            Text(textoData(selecionada.dataHora),style=MaterialTheme.typography.bodyMedium)
            TextButton({ datas = !datas }) { Text(stringResource(Res.string.crescimento_escolher)) }
            if (datas) disponiveis.forEach { a ->
                TextButton({ id=a.id; datas=false }) { Text(textoData(a.dataHora)) }
            }
            val serie = crescimento(avaliacoes,selecionada.id,escolhido) ?: return@Column
            val unidade = serie.selecionado.curva.unidade.orEmpty()
            val meses = stringResource(Res.string.crescimento_meses)
            val medida = stringResource(Res.string.crescimento_medida)
            val linhas = serie.pontos.map { p ->
                textoData(p.data) to "${p.idadeMeses} $meses · ${textoDecimal(p.valor)} $unidade"
            } + serie.referencias.flatMap { r ->
                r.cortes.mapIndexed { i,v -> "${r.idadeMeses} $meses" to "${percentisCrescimento[i]}: ${textoDecimal(v)} $unidade" }
            }
            CardDieta(Modifier.fillMaxWidth()) {
                Text(rotuloCrescimento(escolhido),style=MaterialTheme.typography.titleMedium)
                Text(stringResource(Res.string.crescimento_ponto, serie.selecionado.idadeMeses, textoDecimal(serie.selecionado.valor), unidade))
                GraficoComTabela(linhas) { CurvaCrescimentoConfirmada(serie, unidade, meses, medida) }
                Text(stringResource(Res.string.crescimento_legenda),style=MaterialTheme.typography.bodySmall)
                if (serie.referencias.isEmpty()) Text(stringResource(Res.string.crescimento_sem_cortes))
                Text(stringResource(Res.string.crescimento_limites),style=MaterialTheme.typography.bodySmall)
            }
            CardDieta(Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.crescimento_palavras),style=MaterialTheme.typography.titleMedium)
                serie.selecionado.curva.percentil?.let { Text(textoIntervaloInfantil(stringResource(Res.string.inf_percentil),it)) }
                    ?: Text(stringResource(Res.string.inf_sem_resultado))
                Text(stringResource(Res.string.crescimento_intervalo),style=MaterialTheme.typography.bodySmall)
                serie.fonte?.let { Text(it,style=MaterialTheme.typography.bodySmall) }
                serie.versao?.let { Text(stringResource(Res.string.crescimento_versao,it),style=MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable
private fun rotuloCrescimento(codigo: String) = stringResource(when(codigo) {
    "PESO_IDADE" -> Res.string.inf_peso_idade
    "ESTATURA_IDADE" -> Res.string.inf_estatura_idade
    else -> Res.string.inf_imc_idade
})
