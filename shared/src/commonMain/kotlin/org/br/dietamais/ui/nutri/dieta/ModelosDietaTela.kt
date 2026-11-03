package org.br.dietamais.ui.nutri.dieta

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import org.br.dietamais.data.remoto.dto.ModeloDietaResposta
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

/** Ações da biblioteca agrupadas para a tela ficar sem estado. [aoUsar] nulo esconde "Usar" (sem paciente em foco). */
class AcoesModelos(
    val aoVoltar: () -> Unit = {}, val aoAba: (AbaModelos) -> Unit = {}, val aoDuplicar: (ModeloDietaResposta) -> Unit = {},
    val aoApagar: (ModeloDietaResposta) -> Unit = {}, val aoConfirmarApagar: () -> Unit = {}, val aoCancelarApagar: () -> Unit = {},
    val aoUsar: ((ModeloDietaResposta) -> Unit)? = null, val aoRecarregar: () -> Unit = {},
)

/**
 * Biblioteca de modelos de dieta (artboard `A-Modelos-Biblioteca`, `docs/design/padrao-ui.md`): abas Do sistema e Meus, restrições em chips,
 * energia e número de refeições, Duplicar e Usar. Modelo do sistema é só leitura (RN-MOD-01).
 */
@Composable
fun ModelosDietaTela(estado: EstadoModelos, acoes: AcoesModelos) {
    val habilitado = !estado.semConexao && !estado.enviando && !estado.carregando
    Surface(Modifier.fillMaxSize().testTag("modelos-dieta"), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Dimens.margemCompacta), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
            BotaoTexto(stringResource(Res.string.mod_voltar), acoes.aoVoltar)
            Text(stringResource(Res.string.mod_titulo), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            Text(stringResource(Res.string.mod_subtitulo), style = MaterialTheme.typography.bodySmall)
            if (estado.semConexao) FaixaOffline()
            estado.erro?.let { Text(textoMensagemModelos(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
            estado.aviso?.let { Text(textoMensagemModelos(it), style = MaterialTheme.typography.bodySmall, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                FilterChip(estado.aba == AbaModelos.SISTEMA, { acoes.aoAba(AbaModelos.SISTEMA) }, { Text(stringResource(Res.string.mod_aba_sistema)) })
                FilterChip(estado.aba == AbaModelos.MEUS, { acoes.aoAba(AbaModelos.MEUS) }, { Text(stringResource(Res.string.mod_aba_meus)) })
            }
            if (estado.aba == AbaModelos.SISTEMA) Text(stringResource(Res.string.mod_somente_leitura), style = MaterialTheme.typography.bodySmall)
            if (estado.carregando) EstadoCarregando()
            else if (estado.visiveis.isEmpty()) EstadoVazio(mensagem = stringResource(if (estado.aba == AbaModelos.MEUS) Res.string.mod_vazio_meus else Res.string.mod_vazio_sistema))
            estado.visiveis.forEach { m -> CartaoModelo(m, habilitado, acoes) }
            if (estado.erro != null && !estado.carregado) BotaoTexto(stringResource(Res.string.sync_atualizar), acoes.aoRecarregar)
        }
    }
    estado.apagar?.let { alvo ->
        AlertDialog(onDismissRequest = acoes.aoCancelarApagar, title = { Text(stringResource(Res.string.mod_apagar_titulo)) },
            text = { Text(stringResource(Res.string.mod_apagar_texto, alvo.titulo)) },
            confirmButton = { BotaoTexto(stringResource(Res.string.mod_apagar), acoes.aoConfirmarApagar) },
            dismissButton = { BotaoTexto(stringResource(Res.string.mod_cancelar), acoes.aoCancelarApagar) })
    }
}

@Composable
private fun CartaoModelo(m: ModeloDietaResposta, habilitado: Boolean, a: AcoesModelos) {
    CardDieta(Modifier.fillMaxWidth().testTag("modelo-${m.id}")) {
        Text(m.titulo, style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno), verticalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
            if (m.doSistema) ChipStatus(EstiloStatus.INFO, texto = stringResource(Res.string.mod_chip_sistema))
            m.restricoes.forEach { ChipStatus(EstiloStatus.DEPOIS, texto = textoRestricao(it)) }
        }
        m.descricao?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        val kcal = m.kcal
        Text(if (kcal != null) stringResource(Res.string.mod_kcal_refeicoes, formatar(kcal), m.refeicoesPrincipais) else stringResource(Res.string.mod_refeicoes, m.refeicoesPrincipais),
            style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
            BotaoTexto(stringResource(Res.string.mod_duplicar), { a.aoDuplicar(m) }, habilitado = habilitado)
            if (!m.doSistema) BotaoTexto(stringResource(Res.string.mod_apagar), { a.aoApagar(m) }, habilitado = habilitado)
            a.aoUsar?.let { usar -> BotaoContornado(stringResource(Res.string.mod_usar), { usar(m) }, habilitado = habilitado) }
        }
    }
}

@Composable
internal fun textoRestricao(codigo: String): String = when (codigo) {
    "CONTEM_LACTOSE" -> stringResource(Res.string.mod_restricao_lactose)
    "CONTEM_GLUTEN" -> stringResource(Res.string.mod_restricao_gluten)
    "ALTO_SODIO" -> stringResource(Res.string.mod_restricao_sodio)
    "ACUCAR_ADICIONADO" -> stringResource(Res.string.mod_restricao_acucar)
    "ALTO_POTASSIO" -> stringResource(Res.string.mod_restricao_potassio)
    else -> codigo
}

@Composable
fun textoMensagemModelos(m: MensagemModelos): String = stringResource(when (m) {
    MensagemModelos.SEM_CONEXAO -> Res.string.mod_erro_sem_conexao
    MensagemModelos.NAO_ENCONTRADA -> Res.string.mod_erro_nao_encontrado
    MensagemModelos.SOMENTE_LEITURA -> Res.string.mod_erro_somente_leitura
    MensagemModelos.CONFLITO -> Res.string.mod_erro_conflito
    MensagemModelos.LIMITE -> Res.string.mod_erro_limite
    MensagemModelos.ASSINATURA_INATIVA -> Res.string.mod_erro_assinatura
    MensagemModelos.TAXA_EXCEDIDA -> Res.string.mod_erro_taxa
    MensagemModelos.ERRO -> Res.string.mod_erro_generico
    MensagemModelos.DUPLICADO -> Res.string.mod_duplicado
    MensagemModelos.APAGADO -> Res.string.mod_apagado
    MensagemModelos.SALVO -> Res.string.mod_salvo
})
