package org.br.dietamais.ui.paciente.evolucao

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.br.dietamais.data.remoto.dto.ExameResultadoDto
import org.br.dietamais.dominio.agenda.formatarData
import org.br.dietamais.dominio.avaliacao.*
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.comum.graficos.*
import org.br.dietamais.ui.nutri.avaliacao.AvaliacoesViewModel
import org.br.dietamais.ui.theme.*
import org.jetbrains.compose.resources.stringResource

/** Guia "Exames" do paciente (artboard `P-Exames`): só o que a nutri compartilhou, com régua de faixa e o rótulo escrito junto da cor. */
@Composable
fun MeusExamesRota(avaliacoes: AvaliacoesViewModel) {
    val estado by avaliacoes.estado.collectAsState()
    LaunchedEffect(Unit) { avaliacoes.abrirComoPaciente() }
    MeusExamesTela(meusExames(estado.itens))
}

@Composable
fun MeusExamesTela(exames: ExamesDaAvaliacao?) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = Dimens.espacoMedio).testTag("meus-exames"), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
        Text(stringResource(Res.string.exames_titulo), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        if (exames == null) { EstadoVazio(mensagem = stringResource(Res.string.exames_vazio)); return@Column }
        val data = exames.dataHora.toLocalDateTime(TimeZone.currentSystemDefault()).date
        Text(stringResource(Res.string.exames_subtitulo, formatarData(data)), style = MaterialTheme.typography.bodySmall)
        exames.exames.forEach { CartaoExame(it) }
        Text(stringResource(Res.string.exames_aviso), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun CartaoExame(e: ExameResultadoDto) {
    val segmentos = e.faixas?.let(::segmentosDoExame)
    val classe = e.rotulo?.let(::classeDoRotulo) ?: ClasseFaixa.OUTRA
    CardDieta(Modifier.fillMaxWidth().testTag("exame-${e.codigo}")) {
        Text(e.nome ?: e.codigo, style = MaterialTheme.typography.titleMedium)
        e.rotulo?.let { ChipStatus(estiloDaClasse(classe), texto = rotuloLegivel(it)) }
        val valor = e.valor
        when {
            valor != null && segmentos != null -> ReguaFaixas(segmentos.map { TrechoRegua(it, corDaClasse(classeDoRotulo(it.rotulo))) }, valor, e.unidade.orEmpty())
            valor != null -> Text(stringResource(Res.string.exames_valor, textoDecimal(valor), e.unidade.orEmpty()), style = MaterialTheme.typography.titleMedium)
            e.texto != null -> Text(rotuloLegivel(e.texto), style = MaterialTheme.typography.titleMedium)
        }
        if (e.rotulo == null && e.motivo != null) Text(stringResource(Res.string.exames_sem_faixa), style = MaterialTheme.typography.bodySmall)
    }
}

private fun estiloDaClasse(c: ClasseFaixa) = when (c) { ClasseFaixa.NORMAL -> EstiloStatus.FEITA; ClasseFaixa.ALTO -> EstiloStatus.ALERTA; else -> EstiloStatus.INFO }

@Composable
private fun corDaClasse(c: ClasseFaixa): Color = when (c) {
    ClasseFaixa.ABAIXO -> FaixaAbaixo; ClasseFaixa.NORMAL -> FaixaNormal; ClasseFaixa.ATENCAO -> FaixaSobrepeso; ClasseFaixa.ALTO -> FaixaObesidade
    ClasseFaixa.OUTRA -> MaterialTheme.colorScheme.outlineVariant
}
