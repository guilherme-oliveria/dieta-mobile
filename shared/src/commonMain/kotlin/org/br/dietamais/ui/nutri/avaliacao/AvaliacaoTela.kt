package org.br.dietamais.ui.nutri.avaliacao

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import org.br.dietamais.dominio.avaliacao.*
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

data class AcoesAvaliacao(val aoMedida: (MedidaAvaliacao, String) -> Unit = { _, _ -> }, val aoParecer: (String) -> Unit = {},
    val aoCompartilhar: (Boolean) -> Unit = {}, val aoJustificativa: (String) -> Unit = {}, val aoSalvar: () -> Unit = {}, val aoFechar: () -> Unit = {})

/** Formulário em seções (RN-AVA-01): básicas, perímetros, dobras e composição; IMC calculado em tempo real como prévia. */
@Composable
fun AvaliacaoTela(estado: EstadoAvaliacoes, acoes: AcoesAvaliacao, rolavel: Boolean = true) {
    val form = estado.formulario ?: return
    val habilitado = !estado.enviando
    val previa = previaImc(form.valores)
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = Dimens.larguraLeitura).fillMaxWidth().rolavelSe(rolavel).padding(Dimens.margemCompacta),
                verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
                Text(stringResource(if (form.id == null) Res.string.avaliacao_nova else Res.string.avaliacao_editar),
                    style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
                MensagensAvaliacao(estado)
                SecaoAvaliacao.entries.forEach { secao ->
                    Text(stringResource(rotuloSecao(secao)), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                    MedidaAvaliacao.entries.filter { it.secao == secao }.forEach { medida ->
                        CampoNumero(stringResource(rotuloMedida(medida)), form.valores[medida].orEmpty(), { acoes.aoMedida(medida, it) },
                            erro = estado.errosMedidas[medida]?.let { textoErroMedida(it) }, habilitado = habilitado)
                    }
                    if (secao == SecaoAvaliacao.BASICAS && previa != null) {
                        Text(stringResource(Res.string.avaliacao_imc_previa, textoDecimal(previa.first), stringResource(rotuloClassificacao(previa.second))),
                            style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    }
                }
                CampoTexto(stringResource(Res.string.avaliacao_parecer), form.parecer, acoes.aoParecer, singleLine = false, habilitado = habilitado)
                Row(Modifier.fillMaxWidth().heightIn(min = Dimens.alvoToque).toggleable(form.compartilhada, enabled = habilitado, role = Role.Switch, onValueChange = acoes.aoCompartilhar),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
                    Text(stringResource(Res.string.avaliacao_compartilhar), Modifier.weight(1f))
                    Switch(form.compartilhada, onCheckedChange = null, enabled = habilitado)
                }
                if (form.exigeJustificativa) CampoTexto(stringResource(Res.string.avaliacao_justificativa), form.justificativa, acoes.aoJustificativa, singleLine = false,
                    erro = if (estado.justificativaFaltando) stringResource(Res.string.avaliacao_erro_justificativa) else null, habilitado = habilitado)
                BotaoPrincipal(stringResource(Res.string.avaliacao_salvar), acoes.aoSalvar, Modifier.fillMaxWidth(), habilitado = habilitado, carregando = estado.enviando)
                BotaoTexto(stringResource(Res.string.avaliacao_cancelar), acoes.aoFechar, Modifier.fillMaxWidth(), habilitado = !estado.enviando)
            }
        }
    }
}

@Composable
internal fun MensagensAvaliacao(estado: EstadoAvaliacoes) {
    estado.erro?.let { Text(textoMensagemAvaliacao(it), color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }) }
    estado.aviso?.let { Text(textoMensagemAvaliacao(it), color = MaterialTheme.colorScheme.primary, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
}
