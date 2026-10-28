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
import org.br.dietamais.data.remoto.dto.AvaliacaoResposta
import org.br.dietamais.dominio.avaliacao.*
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource
import kotlin.uuid.Uuid

data class AcoesHistorico(val aoNova: () -> Unit = {}, val aoEditar: (Uuid) -> Unit = {}, val aoSelecionar: (Uuid) -> Unit = {}, val aoComparar: () -> Unit = {},
    val aoVoltarComparacao: () -> Unit = {}, val aoAtualizar: () -> Unit = {})

/** Histórico em ordem cronológica inversa, com seleção de duas datas para comparar (MOB-22). Paciente: somente leitura. */
@Composable
fun HistoricoAvaliacoesTela(estado: EstadoAvaliacoes, acoes: AcoesHistorico, podeEditar: Boolean = !estado.somenteLeitura) {
    val comparacao = estado.comparacao
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = Dimens.espacoMedio), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
        if (estado.semConexao) FaixaOffline()
        MensagensAvaliacao(estado)
        if (estado.comparando && comparacao != null) {
            ComparacaoAvaliacoes(comparacao.first, comparacao.second, acoes.aoVoltarComparacao)
            return@Column
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno), verticalAlignment = Alignment.CenterVertically) {
            if (podeEditar) BotaoPrincipal(stringResource(Res.string.avaliacao_nova), acoes.aoNova)
            BotaoContornado(stringResource(Res.string.avaliacao_comparar), acoes.aoComparar, habilitado = estado.selecionadas.size == 2)
            if (!estado.somenteLeitura) BotaoTexto(stringResource(Res.string.avaliacao_atualizar), acoes.aoAtualizar, habilitado = !estado.atualizando && !estado.semConexao)
        }
        if (estado.itens.size >= 2) Text(stringResource(Res.string.avaliacao_comparar_dica), style = MaterialTheme.typography.bodySmall)
        when {
            estado.atualizando && estado.itens.isEmpty() -> EstadoCarregando()
            estado.itens.isEmpty() -> EstadoVazio(mensagem = stringResource(if (estado.somenteLeitura) Res.string.avaliacao_paciente_vazia else Res.string.avaliacao_vazia))
            else -> estado.itens.forEach { CartaoAvaliacao(it, it.id in estado.selecionadas, podeEditar, acoes) }
        }
    }
}

@Composable
private fun CartaoAvaliacao(item: AvaliacaoResposta, selecionada: Boolean, editavel: Boolean, acoes: AcoesHistorico) {
    val rotuloSelecionar = stringResource(Res.string.avaliacao_selecionar)
    CardDieta(Modifier.fillMaxWidth().toggleable(selecionada, role = Role.Checkbox, onValueChange = { acoes.aoSelecionar(item.id) })
        .semantics { contentDescription = rotuloSelecionar }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
            Checkbox(selecionada, onCheckedChange = null)
            Column(Modifier.weight(1f)) {
                Text(textoData(item.dataHora), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(Res.string.avaliacao_peso_estatura, textoDecimal(item.pesoKg), textoDecimal(item.estaturaM)), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(Res.string.avaliacao_imc_valor, textoDecimal(item.imc), stringResource(rotuloClassificacao(item.classificacaoImc))), style = MaterialTheme.typography.bodyMedium)
                if (!item.compartilhada) ChipStatus(EstiloStatus.INFO, texto = stringResource(Res.string.avaliacao_privada))
            }
            if (editavel) BotaoTexto(stringResource(Res.string.avaliacao_editar), { acoes.aoEditar(item.id) })
        }
    }
}

@Composable
private fun ComparacaoAvaliacoes(antes: AvaliacaoResposta, depois: AvaliacaoResposta, aoVoltar: () -> Unit) {
    Text("${stringResource(Res.string.avaliacao_comparacao_titulo)}: ${textoData(antes.dataHora)} → ${textoData(depois.dataHora)}",
        style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
    CardDieta(Modifier.fillMaxWidth()) {
        compararAvaliacoes(antes, depois).forEach { v ->
            val rotulo = if (v.medida == null) stringResource(Res.string.avaliacao_imc) else stringResource(rotuloMedida(v.medida))
            Row(Modifier.fillMaxWidth().padding(vertical = Dimens.espacoMinimo), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(rotulo, Modifier.weight(1f))
                Text(stringResource(Res.string.avaliacao_comparacao_linha, textoDecimal(v.antes), textoDecimal(v.depois), textoVariacao(v.delta)))
            }
        }
    }
    BotaoTexto(stringResource(Res.string.avaliacao_comparacao_voltar), aoVoltar)
}
