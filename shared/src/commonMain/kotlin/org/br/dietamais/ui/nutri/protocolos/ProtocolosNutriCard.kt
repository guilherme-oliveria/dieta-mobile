package org.br.dietamais.ui.nutri.protocolos

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import org.br.dietamais.dominio.calculos.ProtocoloDobras
import org.br.dietamais.dominio.calculos.ProtocoloMassa
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

data class AcoesProtocolosNutri(val aoDobras: (ProtocoloDobras) -> Unit = {}, val aoMassa: (ProtocoloMassa) -> Unit = {}, val aoSalvar: () -> Unit = {}, val aoRecarregar: () -> Unit = {})

/** "Meus protocolos" na conta da nutri (MOB-53): protocolo de dobras e cálculo da massa que já vêm marcados nas próximas avaliações. */
@Composable
fun ProtocolosNutriCard(estado: EstadoProtocolosNutri, acoes: AcoesProtocolosNutri, modifier: Modifier = Modifier) {
    CardDieta(modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.protocolos_titulo), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        Text(stringResource(Res.string.protocolos_explicacao), style = MaterialTheme.typography.bodySmall)
        val habilitado = !estado.enviando && estado.carregado
        Escolha(stringResource(Res.string.avaliacao_protocolo_dobras),
            listOf(ProtocoloDobras.TRES to stringResource(Res.string.avaliacao_protocolo_tres), ProtocoloDobras.SETE to stringResource(Res.string.avaliacao_protocolo_sete)),
            estado.dobras, acoes.aoDobras, habilitado)
        Escolha(stringResource(Res.string.avaliacao_protocolo_massa),
            listOf(ProtocoloMassa.MAGRO to stringResource(Res.string.avaliacao_protocolo_magro), ProtocoloMassa.MUSCULAR to stringResource(Res.string.avaliacao_protocolo_muscular)),
            estado.massa, acoes.aoMassa, habilitado)
        estado.erro?.let { Text(textoMensagemProtocolos(it), color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }) }
        estado.aviso?.let { Text(textoMensagemProtocolos(it), color = MaterialTheme.colorScheme.primary, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
        if (!estado.carregado && !estado.enviando) BotaoContornado(stringResource(Res.string.protocolos_tentar_de_novo), acoes.aoRecarregar, Modifier.fillMaxWidth())
        else BotaoPrincipal(stringResource(Res.string.protocolos_salvar), acoes.aoSalvar, Modifier.fillMaxWidth(), habilitado = habilitado && estado.alterado, carregando = estado.enviando)
    }
}

@Composable
private fun <T> Escolha(titulo: String, opcoes: List<Pair<T, String>>, atual: T, aoEscolher: (T) -> Unit, habilitado: Boolean) {
    Text(titulo, style = MaterialTheme.typography.labelLarge)
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
        opcoes.forEach { (valor, rotulo) ->
            FilterChip(selected = valor == atual, onClick = { aoEscolher(valor) }, label = { Text(rotulo) }, enabled = habilitado, modifier = Modifier.heightIn(min = Dimens.alvoToque))
        }
    }
}

@Composable
fun textoMensagemProtocolos(m: MensagemProtocolos): String = stringResource(when (m) {
    MensagemProtocolos.SEM_CONEXAO -> Res.string.protocolos_msg_sem_conexao
    MensagemProtocolos.CONFLITO -> Res.string.protocolos_msg_conflito
    MensagemProtocolos.ASSINATURA_INATIVA -> Res.string.protocolos_msg_assinatura
    MensagemProtocolos.ERRO -> Res.string.protocolos_msg_erro
    MensagemProtocolos.SALVO -> Res.string.protocolos_msg_salvo
})
