package org.br.dietamais.ui.nutri.dieta

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.window.core.layout.WindowSizeClass
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.dominio.dieta.TotaisPorcoes
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource
import kotlin.uuid.Uuid

/** Ações do planejador agrupadas para a tela ficar sem estado (testável e com preview). */
class AcoesPlanejador(
    val aoVoltar: () -> Unit = {}, val aoTitulo: (String) -> Unit = {}, val aoOrientacao: (String) -> Unit = {}, val aoRodape: (String) -> Unit = {},
    val aoMetas: (MetasDieta?) -> Unit = {}, val aoSelecionar: (Uuid) -> Unit = {}, val aoAdicionarRefeicao: () -> Unit = {},
    val aoAdicionarAlternativa: (Uuid) -> Unit = {}, val aoRemoverRefeicao: (Uuid) -> Unit = {},
    val aoNome: (Uuid, String) -> Unit = { _, _ -> }, val aoHorario: (Uuid, String) -> Unit = { _, _ -> },
    val aoAdicionarGrupo: (Uuid, Uuid) -> Unit = { _, _ -> }, val aoAdicionarAVontade: (Uuid, Uuid) -> Unit = { _, _ -> },
    val aoAdicionarTexto: (Uuid, String) -> Unit = { _, _ -> }, val aoEscolherPorcao: (Uuid, Int) -> Unit = { _, _ -> },
    val aoAjustar: (Uuid, Uuid, Int) -> Unit = { _, _, _ -> }, val aoRemoverItem: (Uuid, Uuid) -> Unit = { _, _ -> }, val aoSalvar: () -> Unit = {},
)

/**
 * Planejador por porções (artboards `A-Planejador-Porcoes` e `T-Planejador-Porcoes`, `docs/design/padrao-ui.md`): metas e total ao vivo
 * ao lado (ou no topo no celular), refeições em abas, quantidade em passos de meia porção e "à vontade" sem kcal.
 */
@Composable
fun PlanejadorPorcoesTela(estado: EstadoPlanejador, acoes: AcoesPlanejador, classe: WindowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass) {
    val rascunho = estado.rascunho ?: return
    val expandido = classe.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
    Surface(Modifier.fillMaxSize().testTag("planejador-porcoes"), color = MaterialTheme.colorScheme.background) {
        if (expandido) Row(Modifier.fillMaxSize().padding(Dimens.margemCompacta), horizontalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
            Column(Modifier.weight(2f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) { Refeicoes(estado, rascunho, acoes) }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) { Resumo(estado, rascunho, acoes) }
        }
        else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Dimens.margemCompacta), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
            Resumo(estado, rascunho, acoes); Refeicoes(estado, rascunho, acoes)
        }
    }
}

@Composable
private fun ColumnScope.Resumo(estado: EstadoPlanejador, r: RascunhoPlano, a: AcoesPlanejador) {
    BotaoTexto(stringResource(Res.string.plan_voltar), a.aoVoltar)
    Text(stringResource(Res.string.plan_titulo), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
    if (estado.semConexao) FaixaOffline()
    estado.erro?.let { Text(textoMensagemPlanejador(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
    estado.aviso?.let { Text(textoMensagemPlanejador(it), style = MaterialTheme.typography.bodySmall, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
    CampoTexto(stringResource(Res.string.plan_campo_titulo), r.titulo, a.aoTitulo, Modifier.fillMaxWidth(), habilitado = r.editavel)
    Metas(r, a)
    CardDieta(Modifier.fillMaxWidth().testTag("resultado-vivo")) {
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
            Text(stringResource(Res.string.plan_resultado_vivo), style = MaterialTheme.typography.titleSmall)
            AssistChip({}, label = { Text(stringResource(Res.string.plan_previa)) })
        }
        val dia = estado.totalDia; val saldo = estado.saldo; val metas = r.metas
        if (metas != null && saldo != null) {
            val pct = if (metas.vetKcal > 0) (dia.energia / metas.vetKcal * 100).toInt() else 0
            Text(stringResource(Res.string.plan_dia_de_meta, formatar(dia.energia), formatar(metas.vetKcal), pct.toString()), style = MaterialTheme.typography.titleMedium)
            Macro(stringResource(Res.string.plan_macro_carbo), dia.carboidratos, dia.carboidratos + saldo.carboidrato)
            Macro(stringResource(Res.string.plan_macro_proteina), dia.proteinas, dia.proteinas + saldo.proteina)
            Macro(stringResource(Res.string.plan_macro_lipidio), dia.gorduras, dia.gorduras + saldo.lipidio)
        } else {
            Text(stringResource(Res.string.plan_dia_sem_meta, formatar(dia.energia)), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(Res.string.plan_macro_sem_meta, stringResource(Res.string.plan_macro_carbo), formatar(dia.carboidratos)), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(Res.string.plan_macro_sem_meta, stringResource(Res.string.plan_macro_proteina), formatar(dia.proteinas)), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(Res.string.plan_macro_sem_meta, stringResource(Res.string.plan_macro_lipidio), formatar(dia.gorduras)), style = MaterialTheme.typography.bodyMedium)
        }
        Text(stringResource(Res.string.plan_previa_aviso), style = MaterialTheme.typography.bodySmall)
        estado.referencia?.let { Text(stringResource(Res.string.plan_fonte, it.fonte), style = MaterialTheme.typography.bodySmall) }
    }
    if (r.editavel) BotaoPrincipal(stringResource(Res.string.plan_salvar), a.aoSalvar, Modifier.fillMaxWidth(),
        habilitado = r.alterado && !estado.semConexao && !estado.carregando, carregando = estado.enviando)
}

/** Meta em gramas e a palavra da faixa (90% a 110% = dentro); a barra nunca é o único sinal. */
@Composable
private fun Macro(nome: String, total: Double, meta: Double) {
    val razao = if (meta > 0) total / meta else 0.0
    val faixa = stringResource(when { razao < 0.9 -> Res.string.plan_macro_abaixo; razao > 1.1 -> Res.string.plan_macro_acima; else -> Res.string.plan_macro_dentro })
    val texto = stringResource(Res.string.plan_macro_linha, nome, formatar(total), formatar(meta), faixa)
    Column(Modifier.semantics(mergeDescendants = true) { contentDescription = texto }) {
        Text(texto, style = MaterialTheme.typography.bodyMedium)
        LinearProgressIndicator(progress = { razao.coerceIn(0.0, 1.0).toFloat() }, Modifier.fillMaxWidth(),
            color = if (razao > 1.1) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun Metas(r: RascunhoPlano, a: AcoesPlanejador) {
    var vet by remember(r.dietaId) { mutableStateOf(r.metas?.vetKcal?.paraTexto().orEmpty()) }
    var ch by remember(r.dietaId) { mutableStateOf(r.metas?.carboidratoPct?.paraTexto().orEmpty()) }
    var pt by remember(r.dietaId) { mutableStateOf(r.metas?.proteinaPct?.paraTexto().orEmpty()) }
    var lip by remember(r.dietaId) { mutableStateOf(r.metas?.lipidioPct?.paraTexto().orEmpty()) }
    fun enviar(v: String, c: String, p: String, l: String) {
        fun n(t: String) = t.trim().replace(',', '.').toDoubleOrNull()
        val vn = n(v); val cn = n(c); val pn = n(p); val ln = n(l)
        a.aoMetas(if (vn != null && cn != null && pn != null && ln != null) MetasDieta(vn, cn, pn, ln) else null)
    }
    val teclado = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    CardDieta(Modifier.fillMaxWidth().testTag("metas-dieta")) {
        Text(stringResource(Res.string.plan_metas), style = MaterialTheme.typography.titleSmall)
        CampoTexto(stringResource(Res.string.plan_meta_vet), vet, { vet = it.take(7); enviar(vet, ch, pt, lip) }, Modifier.fillMaxWidth(), habilitado = r.editavel, keyboardOptions = teclado)
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
            CampoTexto(stringResource(Res.string.plan_meta_carbo), ch, { ch = it.take(5); enviar(vet, ch, pt, lip) }, Modifier.weight(1f), habilitado = r.editavel, keyboardOptions = teclado)
            CampoTexto(stringResource(Res.string.plan_meta_proteina), pt, { pt = it.take(5); enviar(vet, ch, pt, lip) }, Modifier.weight(1f), habilitado = r.editavel, keyboardOptions = teclado)
            CampoTexto(stringResource(Res.string.plan_meta_lipidio), lip, { lip = it.take(5); enviar(vet, ch, pt, lip) }, Modifier.weight(1f), habilitado = r.editavel, keyboardOptions = teclado)
        }
    }
}

@Composable
private fun ColumnScope.Refeicoes(estado: EstadoPlanejador, r: RascunhoPlano, a: AcoesPlanejador) {
    Text(stringResource(Res.string.plan_refeicoes), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
        r.refeicoes.forEach { ref ->
            val nome = ref.nome.ifBlank { ref.horario }
            FilterChip(estado.refeicaoAtiva == ref.id, { a.aoSelecionar(ref.id) }, label = { Text(if (ref.alternativa) stringResource(Res.string.plan_alternativa_de, nome) else nome) },
                modifier = Modifier.heightIn(min = Dimens.alvoToque))
        }
        if (r.editavel) BotaoTexto(stringResource(Res.string.plan_adicionar_refeicao), a.aoAdicionarRefeicao, habilitado = r.refeicoes.size < PlanejadorPorcoesViewModel.LIMITE_REFEICOES)
    }
    r.refeicoes.firstOrNull { it.id == estado.refeicaoAtiva }?.let { RefeicaoAtiva(estado, r, it, a) }
    CampoTexto(stringResource(Res.string.plan_orientacao), r.orientacao, a.aoOrientacao, Modifier.fillMaxWidth(), habilitado = r.editavel, singleLine = false)
    CampoTexto(stringResource(Res.string.plan_rodape), r.rodape, a.aoRodape, Modifier.fillMaxWidth(), habilitado = r.editavel, singleLine = false)
}

@Composable
private fun RefeicaoAtiva(estado: EstadoPlanejador, r: RascunhoPlano, ref: RefeicaoPlano, a: AcoesPlanejador) {
    val grupos = estado.referencia?.grupos.orEmpty()
    var seletor by remember(ref.id) { mutableStateOf(false) }
    var texto by remember(ref.id) { mutableStateOf("") }
    CardDieta(Modifier.fillMaxWidth().testTag("refeicao-ativa")) {
        if (ref.alternativa) {
            Text(stringResource(Res.string.plan_fora_do_total), style = MaterialTheme.typography.labelLarge)
            estado.diferencas[ref.id]?.let { Text(stringResource(Res.string.plan_alternativa_diferenca, formatar(it.kcal)), style = MaterialTheme.typography.bodyMedium) }
        }
        CampoTexto(stringResource(Res.string.plan_refeicao_nome), ref.nome, { a.aoNome(ref.id, it) }, Modifier.fillMaxWidth(), habilitado = r.editavel)
        CampoHora(stringResource(Res.string.plan_refeicao_horario), ref.horario, { a.aoHorario(ref.id, it) }, Modifier.fillMaxWidth(), habilitado = r.editavel)
        ref.itens.forEach { item ->
            when (item) {
                is ItemPlano.DoGrupo -> {
                    val grupo = grupos.firstOrNull { it.grupoId == item.grupoId }
                    val escolhida = grupo?.opcoes?.firstOrNull { it.opcao == r.escolhas[item.grupoId] }
                    Text(grupo?.nome.orEmpty(), style = MaterialTheme.typography.titleSmall)
                    escolhida?.let { Text(stringResource(Res.string.plan_porcao_do_grupo, formatar(it.kcal), descricaoPorcao(it)), style = MaterialTheme.typography.bodySmall) }
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                        BotaoContornado("−", { a.aoAjustar(ref.id, item.chave, -1) }, habilitado = r.editavel && item.porcoes > PlanejadorPorcoesViewModel.PASSO,
                            modifier = Modifier.semantics { contentDescription = "menos" })
                        Text(stringResource(Res.string.plan_quantidade, item.porcoes.paraTexto()), style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(androidx.compose.ui.Alignment.CenterVertically))
                        BotaoContornado("+", { a.aoAjustar(ref.id, item.chave, 1) }, habilitado = r.editavel && item.porcoes < PlanejadorPorcoesViewModel.MAXIMO,
                            modifier = Modifier.semantics { contentDescription = "mais" })
                        if (r.editavel) BotaoTexto(stringResource(Res.string.plan_remover_item), { a.aoRemoverItem(ref.id, item.chave) })
                    }
                    if (r.editavel && (grupo?.opcoes?.size ?: 0) > 1) FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                        grupo!!.opcoes.forEach { o ->
                            FilterChip(o.opcao == r.escolhas[item.grupoId], { a.aoEscolherPorcao(item.grupoId, o.opcao) },
                                label = { Text(stringResource(Res.string.plan_porcao_opcao, o.opcao.toString(), formatar(o.kcal), descricaoPorcao(o))) }, modifier = Modifier.heightIn(min = Dimens.alvoToque))
                        }
                    }
                }
                is ItemPlano.AVontade -> {
                    Text(stringResource(Res.string.plan_a_vontade, grupos.firstOrNull { it.grupoId == item.grupoId }?.nome.orEmpty()), style = MaterialTheme.typography.titleSmall)
                    if (r.editavel) BotaoTexto(stringResource(Res.string.plan_remover_item), { a.aoRemoverItem(ref.id, item.chave) })
                }
                is ItemPlano.TextoLivre -> {
                    Text(item.texto, style = MaterialTheme.typography.titleSmall)
                    if (r.editavel) BotaoTexto(stringResource(Res.string.plan_remover_item), { a.aoRemoverItem(ref.id, item.chave) })
                }
            }
        }
        estado.totaisRefeicao[ref.id]?.let { Text(stringResource(Res.string.plan_refeicao_total, formatar(it.energia), formatar(it.carboidratos)), style = MaterialTheme.typography.labelLarge) }
        if (r.editavel) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                BotaoTexto(stringResource(Res.string.plan_adicionar_grupo), { seletor = !seletor })
                if (!ref.alternativa) BotaoTexto(stringResource(Res.string.plan_adicionar_alternativa), { a.aoAdicionarAlternativa(ref.id) })
                BotaoTexto(stringResource(Res.string.plan_remover_refeicao), { a.aoRemoverRefeicao(ref.id) })
            }
            if (seletor) Column(verticalArrangement = Arrangement.spacedBy(Dimens.espacoMinimo)) {
                grupos.forEach { g ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                        BotaoTexto(g.nome, { a.aoAdicionarGrupo(ref.id, g.grupoId) }, Modifier.weight(1f))
                        BotaoTexto(stringResource(Res.string.plan_adicionar_a_vontade), { a.aoAdicionarAVontade(ref.id, g.grupoId) })
                    }
                }
                BotaoTexto(stringResource(Res.string.plan_fechar_seletor), { seletor = false })
            }
            CampoTexto(stringResource(Res.string.plan_texto_livre), texto, { texto = it.take(200) }, Modifier.fillMaxWidth())
            BotaoTexto(stringResource(Res.string.plan_texto_adicionar), { a.aoAdicionarTexto(ref.id, texto); texto = "" }, habilitado = texto.isNotBlank())
        }
    }
}

private fun descricaoPorcao(o: OpcaoPorcaoResposta): String = o.medidaCaseira
    ?: listOfNotNull(o.quantidade?.paraTexto(), o.unidade).joinToString(" ")

@Composable
fun textoMensagemPlanejador(m: MensagemPlanejador): String = stringResource(when (m) {
    MensagemPlanejador.TITULO_OBRIGATORIO -> Res.string.plan_erro_titulo
    MensagemPlanejador.NOME_REFEICAO_OBRIGATORIO -> Res.string.plan_erro_nome_refeicao
    MensagemPlanejador.HORARIO_INVALIDO -> Res.string.plan_erro_horario
    MensagemPlanejador.METAS_INVALIDAS -> Res.string.plan_erro_metas
    MensagemPlanejador.DIETA_VAZIA -> Res.string.plan_erro_vazia
    MensagemPlanejador.LIMITE_EXCEDIDO -> Res.string.plan_erro_limite
    MensagemPlanejador.SEM_CONEXAO -> Res.string.plan_erro_sem_conexao
    MensagemPlanejador.CONFLITO -> Res.string.plan_erro_conflito
    MensagemPlanejador.ASSINATURA_INATIVA -> Res.string.plan_erro_assinatura
    MensagemPlanejador.NAO_EDITAVEL -> Res.string.plan_erro_nao_editavel
    MensagemPlanejador.NAO_ENCONTRADA -> Res.string.plan_erro_nao_encontrada
    MensagemPlanejador.TAXA_EXCEDIDA -> Res.string.plan_erro_taxa
    MensagemPlanejador.ERRO -> Res.string.plan_erro_generico
    MensagemPlanejador.SALVA -> Res.string.plan_salva
    MensagemPlanejador.SO_NO_MONTADOR -> Res.string.plan_so_no_montador
    MensagemPlanejador.SEM_PORCOES -> Res.string.plan_sem_porcoes
})
