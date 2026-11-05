package org.br.dietamais.ui.nutri.agenda

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.br.dietamais.data.remoto.dto.ComparecimentoConsulta
import org.br.dietamais.data.remoto.dto.FormaPagamento
import org.br.dietamais.dominio.agenda.ErroFinanceiro
import org.br.dietamais.dominio.agenda.formatarReais
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

data class AcoesFinanceiro(val aoComparecimento: (ComparecimentoConsulta) -> Unit = {}, val aoValor: (String) -> Unit = {}, val aoForma: (FormaPagamento?) -> Unit = {},
    val aoPago: (Boolean) -> Unit = {}, val aoSalvar: () -> Unit = {}, val aoFechar: () -> Unit = {})

private val comparecimentos = listOf(ComparecimentoConsulta.PENDENTE, ComparecimentoConsulta.COMPARECEU, ComparecimentoConsulta.FALTOU)
private val formas = listOf(FormaPagamento.PIX, FormaPagamento.DINHEIRO, FormaPagamento.CARTAO, FormaPagamento.CONVENIO, FormaPagamento.OUTRO)

@Composable
fun textoMensagemFinanceiro(mensagem: MensagemFinanceiro): String = stringResource(when (mensagem) {
    MensagemFinanceiro.SEM_CONEXAO -> Res.string.financeiro_msg_sem_conexao
    MensagemFinanceiro.CONFLITO -> Res.string.financeiro_msg_conflito
    MensagemFinanceiro.ASSINATURA_INATIVA -> Res.string.agenda_msg_assinatura
    MensagemFinanceiro.VINCULO_ENCERRADO -> Res.string.financeiro_msg_vinculo
    MensagemFinanceiro.NAO_ENCONTRADA -> Res.string.agenda_msg_nao_encontrada
    MensagemFinanceiro.CANCELADA -> Res.string.agenda_msg_ja_cancelada
    MensagemFinanceiro.TAXA_EXCEDIDA -> Res.string.agenda_msg_taxa
    MensagemFinanceiro.DADOS_INVALIDOS -> Res.string.financeiro_msg_invalido
    MensagemFinanceiro.ERRO -> Res.string.agenda_msg_erro
    MensagemFinanceiro.SALVO -> Res.string.financeiro_msg_salvo
})

@Composable
private fun rotuloComparecimento(c: ComparecimentoConsulta) = stringResource(when (c) {
    ComparecimentoConsulta.PENDENTE, ComparecimentoConsulta.OUTRO -> Res.string.financeiro_comp_pendente
    ComparecimentoConsulta.COMPARECEU -> Res.string.financeiro_comp_compareceu
    ComparecimentoConsulta.FALTOU -> Res.string.financeiro_comp_faltou
})

@Composable
private fun rotuloForma(f: FormaPagamento) = stringResource(when (f) {
    FormaPagamento.PIX -> Res.string.financeiro_forma_pix
    FormaPagamento.DINHEIRO -> Res.string.financeiro_forma_dinheiro
    FormaPagamento.CARTAO -> Res.string.financeiro_forma_cartao
    FormaPagamento.CONVENIO -> Res.string.financeiro_forma_convenio
    FormaPagamento.OUTRO -> Res.string.financeiro_forma_outro
})

@Composable
private fun textoErroFinanceiro(e: ErroFinanceiro) = stringResource(when (e) {
    ErroFinanceiro.VALOR_INVALIDO -> Res.string.financeiro_erro_valor
    ErroFinanceiro.PAGO_SEM_VALOR -> Res.string.financeiro_erro_pago_valor
    ErroFinanceiro.PAGO_SEM_FORMA -> Res.string.financeiro_erro_pago_forma
    ErroFinanceiro.PAGO_SEM_COMPARECIMENTO -> Res.string.financeiro_erro_pago_comparecimento
})

/** Lançamento financeiro da consulta (MOB-79, ART-P4): comparecimento, valor, forma e pago. Só a nutri vê. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FinanceiroTela(estado: EstadoFinanceiro, acoes: AcoesFinanceiro) {
    if (!estado.aberto) return
    val f = estado.formulario
    val habilitado = !estado.enviando && !estado.carregando
    Dialog(onDismissRequest = acoes.aoFechar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Column(Modifier.widthIn(max = Dimens.larguraLeitura).fillMaxWidth().verticalScroll(rememberScrollState()).padding(Dimens.margemCompacta),
                    verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
                    Text(stringResource(Res.string.financeiro_titulo), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
                    Text(estado.paciente ?: stringResource(Res.string.agenda_paciente_desconhecido), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(Res.string.financeiro_privado), style = MaterialTheme.typography.bodySmall)
                    if (estado.carregando) Text(stringResource(Res.string.financeiro_carregando), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    estado.erro?.let { Text(textoMensagemFinanceiro(it), color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }) }
                    Text(stringResource(Res.string.financeiro_comparecimento), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                        comparecimentos.forEach { c -> ChipEscolha(f.comparecimento == c, { acoes.aoComparecimento(c) }, label = { Text(rotuloComparecimento(c)) }, enabled = habilitado) }
                    }
                    CampoTexto(stringResource(Res.string.financeiro_valor), f.valor, acoes.aoValor, habilitado = habilitado, unidade = "R$",
                        erro = (estado.erros.firstOrNull { it == ErroFinanceiro.VALOR_INVALIDO } ?: estado.erros.firstOrNull { it == ErroFinanceiro.PAGO_SEM_VALOR })?.let { textoErroFinanceiro(it) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    Text(stringResource(Res.string.financeiro_forma), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                        formas.forEach { x -> ChipEscolha(f.forma == x, { acoes.aoForma(if (f.forma == x) null else x) }, label = { Text(rotuloForma(x)) }, enabled = habilitado) }
                    }
                    if (ErroFinanceiro.PAGO_SEM_FORMA in estado.erros) Text(textoErroFinanceiro(ErroFinanceiro.PAGO_SEM_FORMA), color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                    Row(Modifier.fillMaxWidth().heightIn(min = Dimens.alvoToque).toggleable(f.pago, enabled = habilitado, role = Role.Switch) { acoes.aoPago(it) },
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                        Switch(f.pago, onCheckedChange = null, enabled = habilitado)
                        Text(stringResource(Res.string.financeiro_pago), style = MaterialTheme.typography.bodyLarge)
                    }
                    if (ErroFinanceiro.PAGO_SEM_COMPARECIMENTO in estado.erros) Text(textoErroFinanceiro(ErroFinanceiro.PAGO_SEM_COMPARECIMENTO), color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                        BotaoPrincipal(stringResource(Res.string.financeiro_salvar), acoes.aoSalvar, habilitado = habilitado && !estado.semConexao, carregando = estado.enviando)
                        BotaoTexto(stringResource(Res.string.agenda_form_voltar), acoes.aoFechar, habilitado = !estado.enviando)
                    }
                }
            }
        }
    }
}

/** Resumo do mês (consultas não canceladas): comparecimento, recebido e a receber. */
@Composable
fun ResumoFinanceiroCard(estado: EstadoFinanceiro) {
    estado.aviso?.let { Text(textoMensagemFinanceiro(it), color = MaterialTheme.colorScheme.primary, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
    val r = estado.resumo ?: return
    CardDieta(Modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.financeiro_resumo_titulo), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        Text(stringResource(Res.string.financeiro_resumo_comparecimento, r.consultas, r.compareceram, r.faltaram, r.pendentes), style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(Res.string.financeiro_resumo_recebido, formatarReais(r.recebido)), style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(Res.string.financeiro_resumo_a_receber, formatarReais(r.aReceber)), style = MaterialTheme.typography.bodyLarge)
        r.recebidoPorForma.filterValues { it > 0 }.forEach { (forma, valor) -> Text(rotuloForma(forma) + ": " + formatarReais(valor), style = MaterialTheme.typography.bodySmall) }
    }
}
