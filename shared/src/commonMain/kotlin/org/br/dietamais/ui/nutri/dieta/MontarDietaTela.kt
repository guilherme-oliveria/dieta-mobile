package org.br.dietamais.ui.nutri.dieta

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.window.core.layout.WindowSizeClass
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.dominio.dieta.TotaisCent
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource
import kotlin.uuid.Uuid

/** Ações do montador, agrupadas para a tela ficar sem estado (testável e com preview). */
class AcoesMontador(
    val aoVoltar: () -> Unit = {}, val aoTitulo: (String) -> Unit = {}, val aoOrientacao: (String) -> Unit = {},
    val aoAdicionarRefeicao: () -> Unit = {}, val aoRemoverRefeicao: (Uuid) -> Unit = {}, val aoMoverRefeicao: (Uuid, Int) -> Unit = { _, _ -> },
    val aoNomeRefeicao: (Uuid, String) -> Unit = { _, _ -> }, val aoHorario: (Uuid, String) -> Unit = { _, _ -> },
    val aoAbrirSeletor: (Uuid?) -> Unit = {}, val aoBusca: (String) -> Unit = {}, val aoGrupoFiltro: (Uuid?) -> Unit = {},
    val aoAdicionarItem: (Uuid, Uuid) -> Unit = { _, _ -> }, val aoRemoverItem: (Uuid, Uuid) -> Unit = { _, _ -> },
    val aoPorcoes: (Uuid, Uuid, String) -> Unit = { _, _, _ -> }, val aoSalvar: () -> Unit = {}, val aoPublicar: () -> Unit = {},
    val aoConfirmarPublicacao: () -> Unit = {}, val aoCancelarPublicacao: () -> Unit = {}, val aoDuplicar: () -> Unit = {},
)

@Composable
fun MontarDietaTela(estado: EstadoDieta, acoes: AcoesMontador, classe: WindowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass) {
    val rascunho = estado.rascunho ?: return
    val expandido = classe.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
    Surface(Modifier.fillMaxSize().testTag("montar-dieta"), color = MaterialTheme.colorScheme.background) {
        if (expandido) Row(Modifier.fillMaxSize().padding(Dimens.margemCompacta), horizontalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
            Column(Modifier.weight(2f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) { Corpo(estado, rascunho, acoes) }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) { Resumo(estado, rascunho, acoes) }
        }
        else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Dimens.margemCompacta), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
            Resumo(estado, rascunho, acoes); Corpo(estado, rascunho, acoes)
        }
    }
    if (estado.confirmarPublicar) AlertDialog(onDismissRequest = acoes.aoCancelarPublicacao,
        title = { Text(stringResource(Res.string.dieta_publicar_titulo)) },
        text = { Text(stringResource(Res.string.dieta_publicar_resumo, formatar(estado.totais.kcalReais), rascunho.refeicoes.size)) },
        confirmButton = { BotaoPrincipal(stringResource(Res.string.dieta_publicar), acoes.aoConfirmarPublicacao, carregando = estado.enviando) },
        dismissButton = { BotaoTexto(stringResource(Res.string.dieta_cancelar), acoes.aoCancelarPublicacao) })
}

@Composable
private fun ColumnScope.Resumo(estado: EstadoDieta, r: RascunhoDieta, a: AcoesMontador) {
    BotaoTexto(stringResource(Res.string.pacientes_voltar), a.aoVoltar)
    Text(stringResource(if (r.editavel) Res.string.dieta_montar else Res.string.dieta_ver), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
    Text(stringResource(textoStatus(r.status)), style = MaterialTheme.typography.labelLarge)
    if (estado.semConexao) FaixaOffline()
    estado.erro?.let { Text(textoMensagemDieta(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
    estado.aviso?.let { Text(textoMensagemDieta(it), style = MaterialTheme.typography.bodySmall, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
    CardDieta(Modifier.fillMaxWidth().testTag("totais-dieta")) {
        Text(stringResource(Res.string.dieta_totais), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(Res.string.dieta_kcal, formatar(estado.totais.kcalReais)), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(Res.string.dieta_macros, formatar(estado.totais.carboidratosReais), formatar(estado.totais.proteinasReais), formatar(estado.totais.gordurasReais)), style = MaterialTheme.typography.bodyMedium)
    }
    val habilitado = !estado.semConexao && !estado.enviando && !estado.carregando
    if (r.editavel) {
        BotaoPrincipal(stringResource(Res.string.dieta_salvar), a.aoSalvar, Modifier.fillMaxWidth(), habilitado = habilitado && r.alterado, carregando = estado.enviando)
        BotaoContornado(stringResource(Res.string.dieta_publicar), a.aoPublicar, Modifier.fillMaxWidth(), habilitado = habilitado && r.versao != null && !r.alterado)
        if (r.versao == null || r.alterado) Text(stringResource(Res.string.dieta_salvar_antes), style = MaterialTheme.typography.bodySmall)
    } else BotaoContornado(stringResource(Res.string.dieta_duplicar), a.aoDuplicar, Modifier.fillMaxWidth(), habilitado = habilitado)
}

@Composable
private fun ColumnScope.Corpo(estado: EstadoDieta, r: RascunhoDieta, a: AcoesMontador) {
    val editavel = r.editavel
    CampoTexto(stringResource(Res.string.dieta_titulo), r.titulo, a.aoTitulo, Modifier.fillMaxWidth(), habilitado = editavel,
        erro = estado.errosCampos["titulo"]?.let { textoMensagemDieta(it) })
    r.refeicoes.forEachIndexed { indice, ref ->
        CardDieta(Modifier.fillMaxWidth().testTag("refeicao-$indice")) {
            CampoTexto(stringResource(Res.string.dieta_refeicao_nome), ref.nome, { a.aoNomeRefeicao(ref.chave, it) }, Modifier.fillMaxWidth(), habilitado = editavel,
                erro = estado.errosCampos["refeicao:${ref.chave}"]?.let { textoMensagemDieta(it) })
            CampoTexto(stringResource(Res.string.dieta_refeicao_horario), ref.horario, { a.aoHorario(ref.chave, it) }, Modifier.fillMaxWidth(), habilitado = editavel,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), erro = estado.errosCampos["horario:${ref.chave}"]?.let { textoMensagemDieta(it) })
            ref.itens.forEach { item ->
                val alimento = estado.alimentos.firstOrNull { it.id == item.alimentoId }
                Text(alimento?.nome ?: stringResource(Res.string.dieta_alimento_desconhecido), style = MaterialTheme.typography.titleSmall)
                alimento?.let { Text(it.medidaCaseira, style = MaterialTheme.typography.bodySmall) }
                CampoTexto(stringResource(Res.string.dieta_porcoes), item.porcoes, { a.aoPorcoes(ref.chave, item.chave, it) }, Modifier.fillMaxWidth(), habilitado = editavel,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), erro = estado.errosCampos["porcoes:${item.chave}"]?.let { textoMensagemDieta(it) })
                if (editavel) BotaoTexto(stringResource(Res.string.dieta_remover_item), { a.aoRemoverItem(ref.chave, item.chave) })
            }
            val total = estado.totaisRefeicao[ref.chave] ?: TotaisCent()
            Text(stringResource(Res.string.dieta_kcal_refeicao, formatar(total.kcalReais)), style = MaterialTheme.typography.labelLarge)
            if (editavel) FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                BotaoTexto(stringResource(Res.string.dieta_adicionar_alimento), { a.aoAbrirSeletor(ref.chave) })
                BotaoTexto(stringResource(Res.string.dieta_subir), { a.aoMoverRefeicao(ref.chave, -1) }, habilitado = indice > 0)
                BotaoTexto(stringResource(Res.string.dieta_descer), { a.aoMoverRefeicao(ref.chave, 1) }, habilitado = indice < r.refeicoes.lastIndex)
                BotaoTexto(stringResource(Res.string.dieta_remover_refeicao), { a.aoRemoverRefeicao(ref.chave) })
            }
            if (estado.seletorRefeicao == ref.chave) SeletorAlimento(estado, ref.chave, a)
        }
    }
    if (editavel) BotaoContornado(stringResource(Res.string.dieta_adicionar_refeicao), a.aoAdicionarRefeicao, Modifier.fillMaxWidth(), habilitado = r.refeicoes.size < MontadorDietaViewModel.LIMITE_REFEICOES)
    CampoTexto(stringResource(Res.string.dieta_orientacao), r.orientacao, a.aoOrientacao, Modifier.fillMaxWidth(), habilitado = editavel, singleLine = false)
}

@Composable
private fun SeletorAlimento(estado: EstadoDieta, refeicao: Uuid, a: AcoesMontador) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
        CampoTexto(stringResource(Res.string.alimentos_busca), estado.busca, a.aoBusca, Modifier.fillMaxWidth())
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
            FilterChip(estado.grupoFiltro == null, { a.aoGrupoFiltro(null) }, label = { Text(stringResource(Res.string.alimentos_todos_grupos)) }, modifier = Modifier.heightIn(min = Dimens.alvoToque))
            estado.grupos.forEach { g -> FilterChip(estado.grupoFiltro == g.id, { a.aoGrupoFiltro(g.id) }, label = { Text(g.nome) }, modifier = Modifier.heightIn(min = Dimens.alvoToque)) }
        }
        val opcoes = estado.alimentos.filter { it.ativo && (estado.grupoFiltro == null || it.grupoId == estado.grupoFiltro) && it.nome.contains(estado.busca.trim(), ignoreCase = true) }
        if (opcoes.isEmpty()) Text(stringResource(Res.string.alimentos_vazio), style = MaterialTheme.typography.bodySmall)
        opcoes.take(30).forEach { al ->
            BotaoTexto("${al.nome} · ${al.medidaCaseira}", { a.aoAdicionarItem(refeicao, al.id) }, Modifier.fillMaxWidth())
        }
        BotaoTexto(stringResource(Res.string.dieta_cancelar), { a.aoAbrirSeletor(null) })
    }
}

internal fun formatar(valor: Double): String {
    val centesimos = kotlin.math.round(valor * 100).toLong()
    val inteiro = centesimos / 100; val frac = (centesimos % 100).toString().padStart(2, '0')
    return "$inteiro,$frac"
}

internal fun textoStatus(status: StatusDieta) = when (status) {
    StatusDieta.RASCUNHO -> Res.string.dieta_status_rascunho
    StatusDieta.PUBLICADA -> Res.string.dieta_status_publicada
    StatusDieta.SUBSTITUIDA -> Res.string.dieta_status_substituida
    else -> Res.string.dieta_status_arquivada
}

@Composable
fun textoMensagemDieta(mensagem: MensagemDieta): String = stringResource(when (mensagem) {
    MensagemDieta.TITULO_OBRIGATORIO -> Res.string.dieta_erro_titulo
    MensagemDieta.NOME_REFEICAO_OBRIGATORIO -> Res.string.dieta_erro_nome_refeicao
    MensagemDieta.HORARIO_INVALIDO -> Res.string.dieta_erro_horario
    MensagemDieta.PORCOES_INVALIDAS -> Res.string.dieta_erro_porcoes
    MensagemDieta.LIMITE_EXCEDIDO -> Res.string.dieta_erro_limite
    MensagemDieta.SEM_CONEXAO -> Res.string.pacientes_erro_rede
    MensagemDieta.CONFLITO -> Res.string.dieta_erro_conflito
    MensagemDieta.ASSINATURA_INATIVA -> Res.string.pacientes_erro_assinatura
    MensagemDieta.NAO_ENCONTRADA -> Res.string.lista_nao_encontrada
    MensagemDieta.DIETA_VAZIA -> Res.string.dieta_erro_vazia
    MensagemDieta.NAO_EDITAVEL -> Res.string.dieta_erro_nao_editavel
    MensagemDieta.ALIMENTO_INATIVO -> Res.string.dieta_erro_alimento_inativo
    MensagemDieta.TAXA_EXCEDIDA -> Res.string.auth_erro_taxa
    MensagemDieta.ERRO -> Res.string.erro_padrao
    MensagemDieta.SALVA -> Res.string.dieta_salva
    MensagemDieta.PUBLICADA -> Res.string.dieta_publicada
    MensagemDieta.DUPLICADA -> Res.string.dieta_duplicada
    MensagemDieta.EDITAR_NO_PLANEJADOR -> Res.string.dieta_editar_no_planejador
})
