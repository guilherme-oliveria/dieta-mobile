package org.br.dietamais.ui.nutri.avaliacao

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import org.br.dietamais.dominio.avaliacao.*
import org.br.dietamais.dominio.calculos.ProtocoloDobras
import org.br.dietamais.dominio.calculos.ProtocoloMassa
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.comum.graficos.GraficoComTabela
import org.br.dietamais.ui.comum.graficos.GraficoRosca
import org.br.dietamais.ui.comum.graficos.ItemGrafico
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock

data class AcoesAvaliacao(val aoMedida: (MedidaAvaliacao, String) -> Unit = { _, _ -> }, val aoParecer: (String) -> Unit = {},
    val aoCompartilhar: (Boolean) -> Unit = {}, val aoJustificativa: (String) -> Unit = {}, val aoSalvar: () -> Unit = {}, val aoFechar: () -> Unit = {},
    val aoProximo: () -> Unit = {}, val aoVoltarPasso: () -> Unit = {}, val aoRepetirMedidas: () -> Unit = {},
    val aoProtocoloDobras: (ProtocoloDobras) -> Unit = {}, val aoProtocoloMassa: (ProtocoloMassa) -> Unit = {},
    val aoMostrarMedidas: (Boolean) -> Unit = {}, val aoMostrarComposicao: (Boolean) -> Unit = {}, val aoMostrarResumo: (Boolean) -> Unit = {})

/**
 * Avaliação em passos (MOB-50): Medidas, Composição e Resumo, com os resultados recalculando a cada campo (prévia local, offline).
 * Na janela larga os resultados ficam num painel à direita; no celular, em cartão abaixo do passo. O passo "Necessidades" entra no MOB-52.
 */
@Composable
fun AvaliacaoTela(estado: EstadoAvaliacoes, acoes: AcoesAvaliacao, rolavel: Boolean = true,
    classeJanela: WindowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass, previa: PreviaAvaliacao? = null) {
    val form = estado.formulario ?: return
    val resultados = previa ?: previaDe(estado, form, Clock.System.now())
    val largo = classeJanela.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            if (largo) Row(Modifier.widthIn(max = 1100.dp).fillMaxWidth().rolavelSe(rolavel).padding(Dimens.margemCompacta), horizontalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) { PassoAtual(estado, form, acoes, resultados, painelAoLado = true) }
                Column(Modifier.width(360.dp)) { PainelResultados(resultados, form) }
            } else Column(Modifier.widthIn(max = Dimens.larguraLeitura).fillMaxWidth().rolavelSe(rolavel).padding(Dimens.margemCompacta),
                verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) { PassoAtual(estado, form, acoes, resultados, painelAoLado = false) }
        }
    }
}

@Composable
private fun PassoAtual(estado: EstadoAvaliacoes, form: FormularioAvaliacao, acoes: AcoesAvaliacao, previa: PreviaAvaliacao, painelAoLado: Boolean) {
    val habilitado = !estado.enviando
    val passos = PassoAvaliacao.entries
    Text(stringResource(if (form.id == null) Res.string.avaliacao_nova else Res.string.avaliacao_editar), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
    Text(stringResource(Res.string.avaliacao_passo_titulo, form.passo.ordinal + 1, passos.size, stringResource(rotuloPasso(form.passo))), style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
    LinearProgressIndicator(progress = { (form.passo.ordinal + 1f) / passos.size }, Modifier.fillMaxWidth())
    MensagensAvaliacao(estado)
    when (form.passo) {
        PassoAvaliacao.MEDIDAS -> PassoMedidas(estado, form, acoes, habilitado)
        PassoAvaliacao.COMPOSICAO -> PassoComposicao(estado, form, acoes, habilitado)
        PassoAvaliacao.RESUMO -> PassoResumo(estado, form, acoes, habilitado)
    }
    if (!painelAoLado && form.passo != PassoAvaliacao.RESUMO) PainelResultados(previa, form)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno), verticalAlignment = Alignment.CenterVertically) {
        if (form.passo != PassoAvaliacao.MEDIDAS) BotaoContornado(stringResource(Res.string.avaliacao_voltar_passo), acoes.aoVoltarPasso, habilitado = habilitado)
        if (form.passo == PassoAvaliacao.RESUMO) BotaoPrincipal(stringResource(Res.string.avaliacao_salvar), acoes.aoSalvar, Modifier.weight(1f), habilitado = habilitado, carregando = estado.enviando)
        else BotaoPrincipal(stringResource(Res.string.avaliacao_continuar), acoes.aoProximo, Modifier.weight(1f), habilitado = habilitado)
    }
    BotaoTexto(stringResource(Res.string.avaliacao_cancelar), acoes.aoFechar, Modifier.fillMaxWidth(), habilitado = !estado.enviando)
}

@Composable
private fun Campos(estado: EstadoAvaliacoes, form: FormularioAvaliacao, acoes: AcoesAvaliacao, habilitado: Boolean, medidas: List<MedidaAvaliacao>) {
    medidas.forEach { medida ->
        CampoNumero(stringResource(rotuloMedida(medida)), form.valores[medida].orEmpty(), { acoes.aoMedida(medida, it) },
            erro = estado.errosMedidas[medida]?.let { textoErroMedida(it) }, habilitado = habilitado)
    }
}

@Composable
private fun PassoMedidas(estado: EstadoAvaliacoes, form: FormularioAvaliacao, acoes: AcoesAvaliacao, habilitado: Boolean) {
    if (form.id == null && estado.itens.isNotEmpty()) BotaoContornado(stringResource(Res.string.avaliacao_repetir_medidas), acoes.aoRepetirMedidas, Modifier.fillMaxWidth(), habilitado = habilitado)
    val dobras = dobrasDoProtocolo(form.protocoloDobras, estado.sexo)
    val previaImc = previaImc(form.valores)
    listOf(SecaoAvaliacao.BASICAS, SecaoAvaliacao.PERIMETROS, SecaoAvaliacao.DOBRAS, SecaoAvaliacao.COMPLEMENTARES).forEach { secao ->
        Text(stringResource(rotuloSecao(secao)), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        if (secao == SecaoAvaliacao.DOBRAS) EscolhaProtocolo(stringResource(Res.string.avaliacao_protocolo_dobras),
            listOf(ProtocoloDobras.TRES to stringResource(Res.string.avaliacao_protocolo_tres), ProtocoloDobras.SETE to stringResource(Res.string.avaliacao_protocolo_sete)),
            form.protocoloDobras, acoes.aoProtocoloDobras, habilitado)
        Campos(estado, form, acoes, habilitado, MedidaAvaliacao.entries.filter { it.secao == secao && (secao != SecaoAvaliacao.DOBRAS || it in dobras) })
        if (secao == SecaoAvaliacao.BASICAS && previaImc != null) {
            Text(stringResource(Res.string.avaliacao_imc_previa, textoDecimal(previaImc.first), stringResource(rotuloClassificacao(previaImc.second))),
                style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
    }
}

@Composable
private fun PassoComposicao(estado: EstadoAvaliacoes, form: FormularioAvaliacao, acoes: AcoesAvaliacao, habilitado: Boolean) {
    EscolhaProtocolo(stringResource(Res.string.avaliacao_protocolo_massa),
        listOf(ProtocoloMassa.MAGRO to stringResource(Res.string.avaliacao_protocolo_magro), ProtocoloMassa.MUSCULAR to stringResource(Res.string.avaliacao_protocolo_muscular)),
        form.protocoloMassa, acoes.aoProtocoloMassa, habilitado)
    Text(stringResource(Res.string.avaliacao_composicao_manual), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
    Campos(estado, form, acoes, habilitado, MedidaAvaliacao.entries.filter { it.secao == SecaoAvaliacao.COMPOSICAO })
}

@Composable
private fun PassoResumo(estado: EstadoAvaliacoes, form: FormularioAvaliacao, acoes: AcoesAvaliacao, habilitado: Boolean) {
    Text(stringResource(Res.string.avaliacao_resumo_parecer_dica), style = MaterialTheme.typography.bodySmall)
    CampoTexto(stringResource(Res.string.avaliacao_parecer), form.parecer, acoes.aoParecer, singleLine = false, habilitado = habilitado)
    Text(stringResource(Res.string.avaliacao_mostrar_ao_paciente), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
    Interruptor(stringResource(Res.string.avaliacao_compartilhar), form.compartilhada, acoes.aoCompartilhar, habilitado)
    if (form.compartilhada) {
        Interruptor(stringResource(Res.string.avaliacao_mostrar_medidas), form.compartilhamento.medidas != false, acoes.aoMostrarMedidas, habilitado)
        Interruptor(stringResource(Res.string.avaliacao_mostrar_composicao), form.compartilhamento.composicao != false, acoes.aoMostrarComposicao, habilitado)
        Interruptor(stringResource(Res.string.avaliacao_mostrar_resumo), form.compartilhamento.resumo != false, acoes.aoMostrarResumo, habilitado)
    }
    if (form.exigeJustificativa) CampoTexto(stringResource(Res.string.avaliacao_justificativa), form.justificativa, acoes.aoJustificativa, singleLine = false,
        erro = if (estado.justificativaFaltando) stringResource(Res.string.avaliacao_erro_justificativa) else null, habilitado = habilitado)
}

@Composable
private fun Interruptor(rotulo: String, ligado: Boolean, aoMudar: (Boolean) -> Unit, habilitado: Boolean) {
    Row(Modifier.fillMaxWidth().heightIn(min = Dimens.alvoToque).toggleable(ligado, enabled = habilitado, role = Role.Switch, onValueChange = aoMudar),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
        Text(rotulo, Modifier.weight(1f))
        Switch(ligado, onCheckedChange = null, enabled = habilitado)
    }
}

@Composable
private fun <T> EscolhaProtocolo(titulo: String, opcoes: List<Pair<T, String>>, atual: T, aoEscolher: (T) -> Unit, habilitado: Boolean) {
    Text(titulo, style = MaterialTheme.typography.labelLarge)
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
        opcoes.forEach { (valor, rotulo) ->
            FilterChip(selected = valor == atual, onClick = { aoEscolher(valor) }, label = { Text(rotulo) }, enabled = habilitado,
                modifier = Modifier.heightIn(min = Dimens.alvoToque))
        }
    }
}

/** Resultados que recalculam a cada campo; cada dado que falta vira uma frase, nunca um zero (RN-CAL-04). */
@Composable
fun PainelResultados(previa: PreviaAvaliacao, form: FormularioAvaliacao, modifier: Modifier = Modifier) {
    CardDieta(modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }) {
        Text(stringResource(Res.string.avaliacao_previa_titulo), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        val r = previa.resultado
        if (r == null) {
            if (previa.faltas.isEmpty()) Text(stringResource(Res.string.avaliacao_previa_vazia), style = MaterialTheme.typography.bodyMedium)
        } else {
            Text(stringResource(Res.string.avaliacao_imc_previa, textoDecimal(r.imc), stringResource(rotuloClassificacao(classificarImc(r.imc)))), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(Res.string.avaliacao_previa_risco, stringResource(rotuloRisco(r.riscoComorbidades))), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(Res.string.avaliacao_previa_peso_teorico, textoDecimal(r.pesoTeoricoMinKg), textoDecimal(r.pesoTeoricoMaxKg)), style = MaterialTheme.typography.bodyMedium)
            r.percentualGordura?.let { pg ->
                Text(stringResource(Res.string.avaliacao_previa_gordura, textoDecimal(pg), r.classificacaoPercentualGordura?.let { stringResource(rotuloClassePercentual(it)) }.orEmpty()),
                    style = MaterialTheme.typography.titleSmall)
            }
            RoscaComposicao(r, form.protocoloMassa)
            r.pesoGordoKg?.let { Text(stringResource(Res.string.avaliacao_previa_peso_linha, stringResource(Res.string.avaliacao_peso_gordo_linha), textoDecimal(it)), style = MaterialTheme.typography.bodyMedium) }
            r.pesoMagroKg?.let { Text(stringResource(Res.string.avaliacao_previa_peso_linha, stringResource(Res.string.avaliacao_peso_magro_linha), textoDecimal(it)), style = MaterialTheme.typography.bodyMedium) }
            r.pesoOsseoKg?.let { Text(stringResource(Res.string.avaliacao_previa_peso_linha, stringResource(Res.string.avaliacao_peso_osseo), textoDecimal(it)), style = MaterialTheme.typography.bodyMedium) }
            r.pesoParaPercentualDesejadoKg?.let { kg ->
                lerDecimal(form.valores[MedidaAvaliacao.PERCENTUAL_DESEJADO].orEmpty())?.let { alvo ->
                    Text(stringResource(Res.string.avaliacao_previa_peso_meta, textoDecimal(alvo), textoDecimal(kg)), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        previa.faltas.forEach { Text(stringResource(textoFalta(it)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Text(stringResource(Res.string.avaliacao_previa_nota), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RoscaComposicao(r: org.br.dietamais.dominio.calculos.ResultadoAvaliacaoAdulto, massa: ProtocoloMassa) {
    val gordo = r.pesoGordoKg ?: return
    val magro = r.pesoMagroKg ?: return
    val osso = r.pesoOsseoKg
    val itens = if (massa == ProtocoloMassa.MUSCULAR && osso != null)
        listOf(ItemGrafico(stringResource(Res.string.avaliacao_rosca_gordura), gordo), ItemGrafico(stringResource(Res.string.avaliacao_rosca_musculo), magro),
            ItemGrafico(stringResource(Res.string.avaliacao_rosca_osso), osso), ItemGrafico(stringResource(Res.string.avaliacao_rosca_residual), r.pesoResidualKg))
    else listOf(ItemGrafico(stringResource(Res.string.avaliacao_rosca_gordura), gordo), ItemGrafico(stringResource(Res.string.avaliacao_rosca_magro), magro))
    GraficoComTabela(itens.map { it.rotulo to "${textoDecimal(it.valor)} kg" }) {
        GraficoRosca(itens, centro = r.percentualGordura?.let { "${textoDecimal(it)}%" })
    }
}

@Composable
internal fun MensagensAvaliacao(estado: EstadoAvaliacoes) {
    estado.erro?.let { Text(textoMensagemAvaliacao(it), color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }) }
    estado.aviso?.let { Text(textoMensagemAvaliacao(it), color = MaterialTheme.colorScheme.primary, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
}
