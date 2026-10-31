package org.br.dietamais.ui.nutri.avaliacao

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import org.br.dietamais.data.remoto.dto.AvaliacaoResposta
import org.br.dietamais.dominio.avaliacao.MedidaAvaliacao
import org.br.dietamais.dominio.avaliacao.SecaoAvaliacao
import org.br.dietamais.dominio.avaliacao.textoDecimal
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

/** Seções que têm ao menos uma medida preenchida; as básicas já aparecem no cartão. */
fun secoesComMedidas(avaliacao: AvaliacaoResposta): List<Pair<SecaoAvaliacao, List<Pair<MedidaAvaliacao, Double>>>> =
    listOf(SecaoAvaliacao.PERIMETROS, SecaoAvaliacao.DOBRAS, SecaoAvaliacao.COMPOSICAO).mapNotNull { secao ->
        val medidas = MedidaAvaliacao.entries.filter { it.secao == secao }.mapNotNull { m -> m.ler(avaliacao)?.let { m to it } }
        if (medidas.isEmpty()) null else secao to medidas
    }

/** Perímetros, dobras, composição e parecer (P0 do relatório): o paciente vê tudo o que a nutri compartilhou; o backend já filtra o resto. */
@Composable
fun DetalhesAvaliacao(avaliacao: AvaliacaoResposta, modifier: Modifier = Modifier) {
    val secoes = secoesComMedidas(avaliacao)
    val resultados = avaliacao.resultados
    Column(modifier, verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Dimens.espacoPequeno)) {
        if (secoes.isEmpty() && avaliacao.parecer.isNullOrBlank() && resultados?.somatorioDobrasMm == null) {
            Text(stringResource(Res.string.avaliacao_detalhe_sem_medidas), style = MaterialTheme.typography.bodySmall)
        }
        secoes.forEach { (secao, medidas) ->
            Text(stringResource(rotuloSecao(secao)), style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
            medidas.forEach { (medida, valor) ->
                Text(stringResource(Res.string.avaliacao_detalhe_linha, stringResource(rotuloMedida(medida)), textoDecimal(valor)), style = MaterialTheme.typography.bodyMedium)
            }
            if (secao == SecaoAvaliacao.DOBRAS) resultados?.somatorioDobrasMm?.let {
                Text(stringResource(Res.string.avaliacao_detalhe_dobras_soma, textoDecimal(it)), style = MaterialTheme.typography.bodyMedium)
            }
        }
        avaliacao.parecer?.takeIf { it.isNotBlank() }?.let {
            Text(stringResource(Res.string.avaliacao_parecer), style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
            Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = Dimens.espacoPequeno))
        }
    }
}
