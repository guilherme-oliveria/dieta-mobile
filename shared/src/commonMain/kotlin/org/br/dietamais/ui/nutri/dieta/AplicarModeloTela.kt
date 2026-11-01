package org.br.dietamais.ui.nutri.dieta

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

/**
 * Aplicar modelo ao paciente (artboard `A-Modelo-Aplicar`, MOB-57, RN-MOD-02/03): VET do paciente, energia do modelo, escala e porções por grupo
 * (modelo × paciente, arredondadas para 0,5). Cria só um rascunho; nada é publicado nem enviado ao paciente.
 */
@Composable
fun AplicarModeloTela(estado: EstadoAplicarModelo, nomePaciente: String, aoVoltar: () -> Unit, aoCriar: () -> Unit) {
    val modelo = estado.modelo ?: return
    val habilitado = !estado.semConexao && !estado.enviando && estado.situacao != SituacaoEscala.CARREGANDO
    Surface(Modifier.fillMaxSize().testTag("aplicar-modelo"), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Dimens.margemCompacta), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
            BotaoTexto(stringResource(Res.string.mod_voltar), aoVoltar)
            Text(stringResource(Res.string.mod_aplicar_titulo), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            Text("${modelo.titulo} · $nomePaciente", style = MaterialTheme.typography.bodyMedium)
            if (estado.semConexao) FaixaOffline()
            estado.erro?.let { Text(textoMensagemModelos(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
            if (estado.situacao == SituacaoEscala.CARREGANDO && estado.erro == null) EstadoCarregando()
            else if (estado.situacao != SituacaoEscala.CARREGANDO) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoMedio), verticalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                    Indicador(stringResource(Res.string.mod_aplicar_vet), estado.vetKcal?.let { stringResource(Res.string.mod_aplicar_kcal, formatar(it)) } ?: "—")
                    Indicador(stringResource(Res.string.mod_aplicar_modelo), modelo.kcal?.let { stringResource(Res.string.mod_aplicar_kcal, formatar(it)) } ?: "—")
                    Indicador(stringResource(Res.string.mod_aplicar_escala), estado.fator?.let { "× ${formatar(it)}" } ?: "—")
                }
                Text(stringResource(when (estado.situacao) {
                    SituacaoEscala.CALCULADA -> Res.string.mod_aplicar_arredondadas
                    SituacaoEscala.SEM_AVALIACAO -> Res.string.mod_aplicar_sem_avaliacao
                    SituacaoEscala.SEM_BASE -> Res.string.mod_aplicar_sem_base
                    else -> Res.string.mod_aplicar_fora_faixa
                }), style = MaterialTheme.typography.bodySmall)
                if (estado.linhas.isNotEmpty()) CardDieta(Modifier.fillMaxWidth()) {
                    Text(stringResource(Res.string.mod_aplicar_porcoes_dia), style = MaterialTheme.typography.titleMedium)
                    Linha(stringResource(Res.string.mod_aplicar_grupo), stringResource(Res.string.mod_aplicar_modelo), stringResource(Res.string.mod_aplicar_paciente), MaterialTheme.typography.labelLarge)
                    estado.linhas.forEach { Linha(it.grupo, formatar(it.modelo), formatar(it.paciente), MaterialTheme.typography.bodyMedium) }
                }
                if (modelo.restricoes.isNotEmpty()) {
                    Text(stringResource(Res.string.mod_aplicar_restricoes), style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno), verticalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                        modelo.restricoes.forEach { ChipStatus(EstiloStatus.DEPOIS, texto = textoRestricao(it)) }
                    }
                    Text(stringResource(Res.string.mod_aplicar_restricoes_texto), style = MaterialTheme.typography.bodySmall)
                }
            }
            estado.avisos.forEach { Text(it.mensagem, style = MaterialTheme.typography.bodySmall) }
            Text(stringResource(Res.string.mod_aplicar_rascunho), style = MaterialTheme.typography.bodySmall)
            BotaoPrincipal(stringResource(Res.string.mod_aplicar_criar), aoCriar, habilitado = habilitado)
        }
    }
}

@Composable
private fun Indicador(rotulo: String, valor: String) {
    Column { Text(rotulo, style = MaterialTheme.typography.labelMedium); Text(valor, style = MaterialTheme.typography.titleMedium) }
}

@Composable
private fun Linha(grupo: String, modelo: String, paciente: String, estilo: androidx.compose.ui.text.TextStyle) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
        Text(grupo, style = estilo, modifier = Modifier.weight(2f))
        Text(modelo, style = estilo, modifier = Modifier.weight(1f))
        Text(paciente, style = estilo, modifier = Modifier.weight(1f))
    }
}
