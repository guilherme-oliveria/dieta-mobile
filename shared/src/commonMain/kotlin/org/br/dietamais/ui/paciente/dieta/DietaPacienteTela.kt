package org.br.dietamais.ui.paciente.dieta

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.nutri.dieta.formatar
import org.br.dietamais.ui.theme.Dimens
import org.br.dietamais.ui.paciente.substituicao.*
import org.br.dietamais.dominio.rotina.substitutosDoItem
import org.br.dietamais.data.remoto.dto.*
import org.jetbrains.compose.resources.stringResource
import kotlin.uuid.Uuid

/** Aba Dieta: refeições detalhadas, orientações e escolha da dieta em uso quando há mais de uma nutri (MOB-14). */
@Composable
fun DietaPacienteTela(estado: EstadoDietaPaciente, aoAbrirRefeicao: (Uuid?) -> Unit, aoEscolher: (Uuid) -> Unit, modifier: Modifier = Modifier,
    pdf: @Composable (EstadoDietaPaciente) -> Unit = {}, aoAbrirLista: () -> Unit = {},
    acoesLista: AcoesListaPaciente = AcoesListaPaciente(), aoTrocar: (Uuid, Uuid) -> Unit = { _, _ -> },
    aoMarcar: (Uuid, StatusCheckin) -> Unit = { _, _ -> }, aoTrocarRefeicao: (Uuid) -> Unit = {}) {
    if (estado.listaAberta) { ListaSubstituicaoTela(estado, acoesLista); return }
    val refeicao = estado.refeicoes.firstOrNull { it.id == estado.refeicaoAberta }
    if (refeicao != null && estado.dieta != null) {
        RefeicaoDietaPaciente(estado, refeicao, { aoAbrirRefeicao(null) }, aoTrocar, aoMarcar, aoTrocarRefeicao)
        return
    }
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        BarraTopo(stringResource(Res.string.aba_dieta), null, "", subtitulo = estado.dieta?.titulo)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(Dimens.paddingCard),
            verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
            if (estado.semConexao) FaixaOffline()
            estado.erro?.let { Text(stringResource(textoErro(it)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            val dieta = estado.dieta
            when {
                !estado.carregado -> EstadoCarregando()
                dieta == null -> EstadoVazio(mensagem = stringResource(Res.string.pac_sem_dieta))
                else -> {
                    Text(stringResource(Res.string.pac_dieta_nutri, dieta.nutricionista.nome, dieta.nutricionista.crn), style = MaterialTheme.typography.bodySmall)
                    MacrosPaciente(dieta.totais, dia = true)
                    pdf(estado)
                    BotaoContornado(stringResource(Res.string.pac_lista_ver), aoAbrirLista, Modifier.fillMaxWidth())
                    if (estado.alternativas.isNotEmpty()) CardDieta(Modifier.fillMaxWidth()) {
                        Text(stringResource(Res.string.pac_dieta_escolher), style = MaterialTheme.typography.titleMedium)
                        estado.alternativas.forEach { s ->
                            Text("${s.nutricionista.nome} · ${s.titulo}", style = MaterialTheme.typography.bodyMedium)
                            if (s.dietaId == dieta.dietaId) Text(stringResource(Res.string.pac_dieta_em_uso), style = MaterialTheme.typography.labelLarge)
                            else BotaoTexto(stringResource(Res.string.pac_dieta_usar), { aoEscolher(s.dietaId) }, habilitado = !estado.semConexao && !estado.enviando)
                        }
                    }
                    Column {
                        estado.refeicoes.forEach { r ->
                            val rotuloAbrir = stringResource(Res.string.pac_dieta_ver)
                            Row(Modifier.fillMaxWidth().heightIn(min = Dimens.alvoToque + Dimens.espacoPequeno)
                                .clickable(onClickLabel = rotuloAbrir, role = androidx.compose.ui.semantics.Role.Button) { aoAbrirRefeicao(r.id) }.padding(vertical = Dimens.espacoPequeno),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
                                Text(stringResource(Res.string.refeicao_nome_horario, r.nome, r.horario.toString().take(5)), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                                Text(stringResource(Res.string.pac_dieta_kcal_refeicao, formatar(r.totais.kcal)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                ChipStatus(when (estado.checkins[r.id]) {
                                    StatusCheckin.FEITO -> EstiloStatus.FEITA
                                    StatusCheckin.TROCADO -> EstiloStatus.TROCADA
                                    StatusCheckin.PULADO -> EstiloStatus.ALERTA
                                    else -> if (r.id == estado.proxima?.id) EstiloStatus.AGORA else EstiloStatus.DEPOIS
                                })
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                    dieta.orientacao?.takeIf { it.isNotBlank() }?.let {
                        Text(stringResource(Res.string.pac_dieta_orientacoes), style = MaterialTheme.typography.titleMedium)
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun RefeicaoDietaPaciente(estado: EstadoDietaPaciente, r: RefeicaoResposta, aoVoltar: () -> Unit,
    aoTrocar: (Uuid, Uuid) -> Unit, aoMarcar: (Uuid, StatusCheckin) -> Unit, aoTrocarRefeicao: (Uuid) -> Unit) {
    val dieta = estado.dieta ?: return
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        BarraTopo(stringResource(Res.string.refeicao_nome_horario, r.nome, r.horario.toString().take(5)), aoVoltar, stringResource(Res.string.pac_lista_voltar),
            subtitulo = stringResource(Res.string.pac_kcal, numeroPaciente(r.totais.kcal)))
        Column(Modifier.weight(1f).fillMaxWidth().padding(Dimens.paddingCard), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
            if (estado.semConexao) FaixaOffline()
            estado.erro?.let { Text(stringResource(textoErro(it)), color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }) }
            val status = when (estado.checkins[r.id]) {
                StatusCheckin.FEITO -> Res.string.status_feita
                StatusCheckin.TROCADO -> Res.string.status_trocada
                StatusCheckin.PULADO -> Res.string.pac_status_pulada
                else -> null
            }
            status?.let { Text(stringResource(Res.string.pac_registro_status, stringResource(it)),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
                MacrosPaciente(r.totais, dia = false)
                r.itens.forEach { i ->
                    CardDieta(Modifier.fillMaxWidth()) {
                        Text(i.textoLivre ?: i.nomeAlimento.orEmpty(), style = MaterialTheme.typography.titleSmall)
                        val porcoes = i.porcoes
                        if (i.aVontade == true) Text(stringResource(Res.string.pac_item_livre), style = MaterialTheme.typography.bodySmall)
                        else if (porcoes != null) {
                            val medida = i.medidaCaseira?.takeIf { it.isNotBlank() } ?: stringResource(Res.string.pac_item_medida_ausente)
                            Text(stringResource(if (porcoes == 1.0) Res.string.pac_item_porcao_medida else Res.string.pac_item_porcoes_medida,
                                numeroPaciente(porcoes), medida), style = MaterialTheme.typography.bodySmall)
                        } else i.medidaCaseira?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        if (i.aVontade != true && i.textoLivre == null && porcoes != null) i.grupoNome?.let { grupo ->
                            Text(stringResource(Res.string.pac_item_substituicao, grupo), style = MaterialTheme.typography.bodySmall)
                        }
                        if (i.textoLivre == null || i.aVontade == true) Text(stringResource(Res.string.pac_kcal, numeroPaciente(if (i.aVontade == true) 0.0 else i.totais.kcal)), style = MaterialTheme.typography.labelLarge)
                        if (substitutosDoItem(i, dieta.listaSubstituicao).isNotEmpty()) BotaoContornado(stringResource(Res.string.pac_item_trocar), { aoTrocar(r.id, i.id) })
                        i.observacao?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                BotaoPrincipal(stringResource(Res.string.pac_comi), { aoMarcar(r.id, StatusCheckin.FEITO) }, habilitado = estado.dia != null)
                BotaoContornado(stringResource(Res.string.pac_troquei), { aoTrocarRefeicao(r.id) }, habilitado = estado.dia != null && r.itens.any { substitutosDoItem(it, dieta.listaSubstituicao).isNotEmpty() })
                BotaoContornado(stringResource(Res.string.pac_pulei), { aoMarcar(r.id, StatusCheckin.PULADO) }, habilitado = estado.dia != null)
            }
        }
    }
}

private fun textoErro(m: MensagemDietaPaciente) = when (m) {
    MensagemDietaPaciente.SEM_CONEXAO -> Res.string.pac_erro_sem_conexao
    MensagemDietaPaciente.CONFLITO -> Res.string.pac_erro_conflito
    MensagemDietaPaciente.ERRO -> Res.string.pac_erro_geral
}
