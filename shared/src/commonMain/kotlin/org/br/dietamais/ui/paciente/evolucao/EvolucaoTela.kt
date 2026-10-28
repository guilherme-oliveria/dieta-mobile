package org.br.dietamais.ui.paciente.evolucao

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import org.br.dietamais.dominio.avaliacao.*
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.Instant

data class AcoesEvolucao(val aoSerie: (SerieEvolucao) -> Unit = {}, val aoIntervalo: (IntervaloEvolucao) -> Unit = {}, val aoRegistrarPeso: () -> Unit = {},
    val aoMudarPeso: (String) -> Unit = {}, val aoSalvarPeso: () -> Unit = {}, val aoFecharPeso: () -> Unit = {})

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun EvolucaoTela(estado: EstadoEvolucao, acoes: AcoesEvolucao, agora: Instant = Clock.System.now(), modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = Dimens.espacoMedio), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
        estado.erro?.takeIf { !estado.registrandoPeso }?.let { Text(textoMensagemEvolucao(it), color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }) }
        estado.aviso?.let { Text(textoMensagemEvolucao(it), color = MaterialTheme.colorScheme.primary, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
        if (estado.podeRegistrarPeso) BotaoPrincipal(stringResource(Res.string.evolucao_registrar_peso), acoes.aoRegistrarPeso)
        val serie = estado.serie
        when {
            !estado.carregado -> EstadoCarregando()
            serie == null -> EstadoVazio(mensagem = stringResource(Res.string.evolucao_vazia))
            else -> {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno), verticalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                    estado.disponiveis.forEach { s -> FilterChip(s == serie, { acoes.aoSerie(s) }, label = { Text(stringResource(rotuloSerie(s))) }) }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                    IntervaloEvolucao.entries.forEach { i -> FilterChip(i == estado.intervalo, { acoes.aoIntervalo(i) }, label = { Text(stringResource(rotuloIntervalo(i))) }) }
                }
                val pontos = recortar(estado.pontos, estado.intervalo, agora)
                val rotulo = stringResource(rotuloSerie(serie))
                CardDieta(Modifier.fillMaxWidth()) {
                    if (pontos.isEmpty()) Text(stringResource(Res.string.evolucao_sem_pontos))
                    else {
                        GraficoLinha(pontos, rotulo, serie.unidade)
                        Text(stringResource(Res.string.evolucao_ultimo, textoDecimal(pontos.last().valor), serie.unidade), style = MaterialTheme.typography.titleSmall)
                        variacaoTotal(pontos)?.let { Text(stringResource(Res.string.evolucao_variacao, textoVariacaoEvolucao(it), serie.unidade), style = MaterialTheme.typography.bodyMedium) }
                    }
                }
            }
        }
    }
    if (estado.registrandoPeso) RegistrarPesoSheet(estado, acoes)
}

private fun textoVariacaoEvolucao(delta: Double) = if (delta > 0) "+" + textoDecimal(delta) else textoDecimal(delta)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun RegistrarPesoSheet(estado: EstadoEvolucao, acoes: AcoesEvolucao) {
    ModalBottomSheet(onDismissRequest = acoes.aoFecharPeso) {
        Column(Modifier.fillMaxWidth().padding(Dimens.margemCompacta).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
            Text(stringResource(Res.string.evolucao_registrar_peso), style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            CampoNumero(stringResource(Res.string.evolucao_peso_rotulo), estado.pesoTexto, acoes.aoMudarPeso,
                erro = estado.erro?.let { textoMensagemEvolucao(it) })
            BotaoPrincipal(stringResource(Res.string.evolucao_peso_salvar), acoes.aoSalvarPeso, Modifier.fillMaxWidth())
            BotaoTexto(stringResource(Res.string.avaliacao_cancelar), acoes.aoFecharPeso, Modifier.fillMaxWidth())
        }
    }
}
