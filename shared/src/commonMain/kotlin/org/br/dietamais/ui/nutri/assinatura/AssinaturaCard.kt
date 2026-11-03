package org.br.dietamais.ui.nutri.assinatura

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import org.br.dietamais.data.remoto.dto.StatusAssinatura
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.nutri.avaliacao.textoData
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToLong

class AcoesAssinatura(val aoAssinar: (String) -> Unit, val aoUrlAberta: () -> Unit, val aoRecarregar: () -> Unit)

/** Cartão "Minha assinatura" da aba Conta (MOB-32). Compra só no navegador externo (RN-ASS-06). */
@Composable
fun AssinaturaCard(estado: EstadoAssinatura, acoes: AcoesAssinatura, modifier: Modifier = Modifier) {
    val navegador = LocalUriHandler.current
    LaunchedEffect(estado.urlCheckout) { estado.urlCheckout?.let { navegador.openUri(it); acoes.aoUrlAberta() } }
    CardDieta(modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.assinatura_titulo), style = MaterialTheme.typography.titleMedium)
        val a = estado.assinatura
        if (a == null) {
            if (estado.enviando) EstadoCarregando() else BotaoTexto(stringResource(Res.string.sync_atualizar), acoes.aoRecarregar, habilitado = !estado.semConexao)
        } else {
            Text(stringResource(Res.string.assinatura_plano, a.plano.nome), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(rotuloStatus(a.status)), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(Res.string.painel_plano_uso, a.pacientesOcupados, a.plano.limitePacientes), style = MaterialTheme.typography.bodyMedium)
            a.fimPeriodoEm?.let { Text(stringResource(if (a.status == StatusAssinatura.TESTE) Res.string.assinatura_teste_ate else Res.string.assinatura_periodo_ate, textoData(it)), style = MaterialTheme.typography.bodySmall) }
            if (estado.emAtraso) Text(stringResource(Res.string.assinatura_atraso, a.carenciaAte?.let { textoData(it) } ?: "—"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            if (estado.somenteLeitura) Text(stringResource(Res.string.assinatura_somente_leitura), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            estado.erro?.let { Text(stringResource(textoErro(it)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            if (a.status != StatusAssinatura.ATIVA) {
                if (a.pagamentoDisponivel && a.planosDisponiveis.isNotEmpty()) {
                    Text(stringResource(Res.string.assinatura_escolha), style = MaterialTheme.typography.titleSmall)
                    a.planosDisponiveis.forEach { p ->
                        BotaoPrincipal(stringResource(Res.string.assinatura_assinar_plano, p.nome, p.limitePacientes, preco(p.precoMensal)), { acoes.aoAssinar(p.codigo) },
                            habilitado = !estado.semConexao && !estado.enviando, carregando = estado.enviando)
                    }
                    Text(stringResource(Res.string.assinatura_navegador), style = MaterialTheme.typography.bodySmall)
                } else Text(stringResource(Res.string.assinatura_pagamento_indisponivel), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

internal fun preco(valor: Double): String {
    val centavos = (valor * 100).roundToLong()
    return "R$ ${centavos / 100},${(centavos % 100).toString().padStart(2, '0')}"
}

private fun rotuloStatus(s: StatusAssinatura): StringResource = when (s) {
    StatusAssinatura.TESTE -> Res.string.assinatura_status_teste
    StatusAssinatura.ATIVA -> Res.string.assinatura_status_ativa
    StatusAssinatura.EM_ATRASO -> Res.string.assinatura_status_atraso
    StatusAssinatura.CANCELADA -> Res.string.assinatura_status_cancelada
    StatusAssinatura.EXPIRADA -> Res.string.assinatura_status_expirada
    StatusAssinatura.OUTRO -> Res.string.assinatura_status_outro
}

private fun textoErro(m: MensagemAssinatura): StringResource = when (m) {
    MensagemAssinatura.SEM_CONEXAO -> Res.string.perfil_erro_sem_conexao
    MensagemAssinatura.JA_ATIVA -> Res.string.assinatura_erro_ja_ativa
    MensagemAssinatura.PAGAMENTO_INDISPONIVEL -> Res.string.assinatura_pagamento_indisponivel
    MensagemAssinatura.ERRO -> Res.string.perfil_erro_geral
}
