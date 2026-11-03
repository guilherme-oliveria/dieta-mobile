package org.br.dietamais.ui.comum.anamnese

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import org.br.dietamais.data.remoto.dto.AnamneseDerivada
import org.br.dietamais.dominio.anamnese.AssuntoAnamnese
import org.br.dietamais.dominio.avaliacao.textoDecimal
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.CardDieta
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.StringResource

/** Apresentação do valor do servidor, sem fórmulas nem interpretação clínica no cliente. */
@Composable
fun ResultadosAnamnese(derivados: AnamneseDerivada?, assuntos: Set<AssuntoAnamnese>, modifier: Modifier = Modifier) {
    if (assuntos.isEmpty()) return
    CardDieta(modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.ana_resultados), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        Text(stringResource(if (derivados == null) Res.string.ana_resultados_aguardando else Res.string.ana_resultados_contexto), style = MaterialTheme.typography.bodySmall)
        if (AssuntoAnamnese.PRESSAO in assuntos) LinhaResultado(stringResource(Res.string.ana_pressao), textoPressao(derivados?.pressaoArterial))
        if (AssuntoAnamnese.ALCOOL in assuntos) {
            LinhaResultado(stringResource(Res.string.ana_alcool_etanol), unidade(derivados?.alcool?.gramasEtanolPorSemana, Res.string.ana_valor_g))
            LinhaResultado(stringResource(Res.string.ana_alcool_volume), unidade(derivados?.alcool?.mlPorSemana, Res.string.ana_valor_ml))
            LinhaResultado(stringResource(Res.string.ana_alcool_energia), unidade(derivados?.alcool?.kcalPorSemana, Res.string.ana_valor_kcal))
        }
        if (AssuntoAnamnese.SAL in assuntos) {
            LinhaResultado(stringResource(Res.string.ana_sal_pessoa), unidade(derivados?.salGramasPorPessoaDia?.toDouble(), Res.string.ana_valor_g))
            Text(stringResource(Res.string.ana_sal_referencia), style = MaterialTheme.typography.bodySmall)
        }
        if (AssuntoAnamnese.OLEO in assuntos) {
            LinhaResultado(stringResource(Res.string.ana_oleo_pessoa), unidade(derivados?.oleoMlPorPessoaDia?.toDouble(), Res.string.ana_valor_ml))
            LinhaResultado(stringResource(Res.string.ana_oleo_energia), unidade(derivados?.oleoKcalPorPessoaDia?.toDouble(), Res.string.ana_valor_kcal))
        }
        if (AssuntoAnamnese.ATIVIDADE in assuntos) {
            val dias = derivados?.atividadesDuracaoMinutos.orEmpty()
            if (dias.isEmpty() || derivados?.atividadeDuracaoMinutos != null) LinhaResultado(stringResource(Res.string.ana_duracao), unidade(derivados?.atividadeDuracaoMinutos?.toDouble(), Res.string.ana_valor_min))
            val rotulos = listOf("SEGUNDA" to Res.string.ana_segunda, "TERCA" to Res.string.ana_terca, "QUARTA" to Res.string.ana_quarta,
                "QUINTA" to Res.string.ana_quinta, "SEXTA" to Res.string.ana_sexta, "SABADO" to Res.string.ana_sabado, "DOMINGO" to Res.string.ana_domingo)
            rotulos.forEach { (dia, rotulo) -> dias[dia]?.let { LinhaResultado(stringResource(rotulo), unidade(it.toDouble(), Res.string.ana_valor_min)) } }
        }
    }
}

@Composable
private fun LinhaResultado(rotulo: String, valor: String?) {
    Column(Modifier.fillMaxWidth().padding(top = Dimens.espacoPequeno).semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(Dimens.espacoMinimo)) {
        Text(rotulo, style = MaterialTheme.typography.labelLarge)
        Text(valor ?: stringResource(Res.string.ana_resultado_ausente), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun unidade(valor: Double?, recurso: StringResource): String? = valor?.takeIf { it.isFinite() && it >= 0 }?.let { stringResource(recurso, textoDecimal(it)) }

@Composable
private fun textoPressao(classe: String?): String? = classe?.let {
    stringResource(when (it) {
        "OTIMA" -> Res.string.ana_pressao_otima
        "NORMAL" -> Res.string.ana_pressao_normal
        "LIMITROFE" -> Res.string.ana_pressao_limitrofe
        "HIPERTENSAO_1" -> Res.string.ana_pressao_hipertensao_1
        "HIPERTENSAO_2" -> Res.string.ana_pressao_hipertensao_2
        "HIPERTENSAO_3" -> Res.string.ana_pressao_hipertensao_3
        "HIPERTENSAO_SISTOLICA_ISOLADA" -> Res.string.ana_pressao_isolada
        else -> Res.string.ana_pressao_futura
    })
}
