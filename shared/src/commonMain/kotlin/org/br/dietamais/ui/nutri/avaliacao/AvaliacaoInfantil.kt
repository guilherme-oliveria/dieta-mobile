package org.br.dietamais.ui.nutri.avaliacao

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.dominio.avaliacao.*
import org.br.dietamais.dominio.calculos.ClassePercentualGordura
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock

private fun meses(estado: EstadoAvaliacoes, form: FormularioAvaliacao) =
    idadeAvaliacaoMeses(estado.nascimento, form.dataHora ?: Clock.System.now()) ?: form.idadeMeses ?: form.resultadosInfantis?.idadeMeses

@Composable
internal fun MedidasInfantis(estado: EstadoAvaliacoes, form: FormularioAvaliacao, acoes: AcoesAvaliacao, habilitado: Boolean) {
    val campos = mutableListOf<@Composable () -> Unit>()
    val idade = meses(estado, form)
    val altura = when { idade == null -> Res.string.inf_altura; idade < 24 -> Res.string.inf_comprimento_cm; else -> Res.string.inf_estatura_cm }
    listOf(MedidaAvaliacao.PESO, MedidaAvaliacao.ESTATURA).forEach { medida -> campos += {
        CampoNumero(stringResource(if (medida == MedidaAvaliacao.ESTATURA) altura else rotuloMedida(medida)),
            form.valores[medida].orEmpty(), { acoes.aoMedida(medida, it) }, habilitado = habilitado,
            erro = estado.errosMedidas[medida]?.let { if (medida == MedidaAvaliacao.ESTATURA) stringResource(Res.string.inf_erro_altura) else textoErroMedida(it) })
    } }
    if ((meses(estado, form) ?: 0) < 120) {
        campos += { CampoNumero(stringResource(Res.string.inf_peso_nascer), form.infantil.pesoNascerKg,
            { acoes.aoInfantil(form.infantil.copy(pesoNascerKg = it.take(12))) }, habilitado = habilitado,
            erro = estado.errosInfantis[CampoInfantil.PESO_NASCER]?.let { stringResource(Res.string.inf_erro_nascer) }) }
        campos += { CampoNumero(stringResource(rotuloMedida(MedidaAvaliacao.BRACO)), form.valores[MedidaAvaliacao.BRACO].orEmpty(),
            { acoes.aoMedida(MedidaAvaliacao.BRACO, it) }, habilitado = habilitado, erro = estado.errosMedidas[MedidaAvaliacao.BRACO]?.let { textoErroMedida(it) }) }
    }
    listOf(MedidaAvaliacao.DOBRA_TRICEPS, MedidaAvaliacao.DOBRA_SUBESCAPULAR).forEach { medida -> campos += {
        CampoNumero(stringResource(rotuloMedida(medida)), form.valores[medida].orEmpty(), { acoes.aoMedida(medida, it) }, habilitado = habilitado,
            erro = estado.errosMedidas[medida]?.let { textoErroMedida(it) })
    } }
    GradeInfantil(campos)
    Text(stringResource(Res.string.inf_modo), style = MaterialTheme.typography.titleSmall)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
        listOf("PERCENTIL" to Res.string.inf_percentil, "ESCORE_Z" to Res.string.inf_escore).forEach { (valor, rotulo) ->
            ChipEscolha(selected = form.infantil.modoCurva == valor, onClick = { acoes.aoInfantil(form.infantil.copy(modoCurva = valor)) },
                label = { Text(stringResource(rotulo)) }, enabled = habilitado, modifier = Modifier.heightIn(min = Dimens.alvoToque))
        }
    }
    if (form.infantil.modoCurva !in listOf("PERCENTIL", "ESCORE_Z")) Text(stringResource(Res.string.inf_escolha_preservada))
}

@Composable
private fun GradeInfantil(campos: List<@Composable () -> Unit>) {
    val fonte = LocalDensity.current.fontScale
    BoxWithConstraints {
        val colunas = if (fonte > 1.3f) 1 else if (maxWidth >= 600.dp) 3 else 2
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
            campos.chunked(colunas).forEach { linha -> Row(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
                linha.forEach { campo -> Box(Modifier.weight(1f)) { campo() } }
                repeat(colunas - linha.size) { Spacer(Modifier.weight(1f)) }
            } }
        }
    }
}

@Composable
internal fun ComposicaoInfantil(estado: EstadoAvaliacoes, form: FormularioAvaliacao, acoes: AcoesAvaliacao, habilitado: Boolean) {
    Text(stringResource(Res.string.inf_composicao_dica))
    val soma = lerDecimal(form.valores[MedidaAvaliacao.DOBRA_TRICEPS].orEmpty())?.let { t ->
        lerDecimal(form.valores[MedidaAvaliacao.DOBRA_SUBESCAPULAR].orEmpty())?.plus(t)
    }
    if ((meses(estado, form) ?: 0) >= 120 && estado.sexo == Sexo.MASCULINO && (soma == null || soma <= 35.0)) {
        Text(stringResource(Res.string.inf_etnia), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(Res.string.inf_etnia_dica), style = MaterialTheme.typography.bodySmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
            listOf(null to Res.string.inf_nao_informada, "BRANCA" to Res.string.inf_branca, "NEGRA" to Res.string.inf_negra).forEach { (valor, rotulo) ->
                ChipEscolha(form.infantil.etniaSlaughter == valor, { acoes.aoInfantil(form.infantil.copy(etniaSlaughter = valor)) },
                    label = { Text(stringResource(rotulo)) }, enabled = habilitado, modifier = Modifier.heightIn(min = Dimens.alvoToque))
            }
        }
        if (form.infantil.etniaSlaughter != null && form.infantil.etniaSlaughter !in listOf("BRANCA", "NEGRA")) Text(stringResource(Res.string.inf_escolha_preservada))
    }
}

@Composable
internal fun NecessidadesInfantis(estado: EstadoAvaliacoes, form: FormularioAvaliacao, acoes: AcoesAvaliacao, habilitado: Boolean) {
    if ((meses(estado, form) ?: 36) >= 36) {
        CampoNumero(stringResource(Res.string.inf_fator), form.infantil.fatorAtividade,
            { acoes.aoInfantil(form.infantil.copy(fatorAtividade = it.take(12))) }, habilitado = habilitado,
            erro = estado.errosInfantis[CampoInfantil.FATOR_ATIVIDADE]?.let { stringResource(Res.string.inf_erro_fator) })
        Text(stringResource(Res.string.inf_fator_dica), style = MaterialTheme.typography.bodySmall)
    } else Text(stringResource(Res.string.inf_menor_tres))
}

@Composable
fun PainelResultadosInfantis(r: ResultadosInfantisAvaliacaoDto?, modifier: Modifier = Modifier, paciente: Boolean = false, edicao: Boolean = true, previa: Boolean = false) {
    CardDieta(modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }) {
        Text(stringResource(Res.string.inf_resultados), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        if (r == null) {
            Text(stringResource(if (edicao && !paciente) Res.string.inf_pendente else Res.string.inf_ausente))
            Text(stringResource(if (edicao && !paciente) Res.string.inf_pendente_dica else Res.string.inf_ausente_dica), style = MaterialTheme.typography.bodySmall)
        } else {
            Text(stringResource(if (previa) Res.string.inf_previa else Res.string.inf_confirmados), style = MaterialTheme.typography.bodySmall)
            r.idadeTexto?.let { Text(it, style = MaterialTheme.typography.titleSmall) }
            r.classificacaoPesoNascer?.let { Text(classeInfantil(it)) }
            r.curvas.orEmpty().forEach { curva ->
                val titulo = when (curva.indicador) { "PESO_IDADE" -> Res.string.inf_peso_idade; "ESTATURA_IDADE" -> Res.string.inf_estatura_idade; "IMC_IDADE" -> Res.string.inf_imc_idade; else -> null }
                if (titulo != null) {
                    Text(stringResource(titulo), style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
                    curva.classificacao?.let { Text(classeInfantil(it)) }
                    curva.percentil?.let { Text(textoIntervaloInfantil(stringResource(Res.string.inf_percentil), it)) }
                    if (!paciente) curva.escoreZ?.let { Text(textoIntervaloInfantil(stringResource(Res.string.inf_escore), it)) }
                    if (curva.classificacao == null && curva.percentil == null) Text(motivoInfantil(curva.motivo))
                    if (!paciente && curva.fonte != null) Text(stringResource(Res.string.inf_fonte, curva.fonte, curva.versaoReferencia.orEmpty()), style = MaterialTheme.typography.labelSmall)
                }
            }
            if (!r.curvas.isNullOrEmpty()) Text(stringResource(Res.string.inf_intervalos_dica), style = MaterialTheme.typography.bodySmall)
            if (!paciente && r.pesoTeoricoMinKg != null && r.pesoTeoricoMaxKg != null)
                Text(stringResource(Res.string.avaliacao_previa_peso_teorico, textoDecimal(r.pesoTeoricoMinKg), textoDecimal(r.pesoTeoricoMaxKg)))
            if ((r.idadeMeses ?: -1) in 24..119 && !r.frisancho.isNullOrEmpty()) {
                Text(stringResource(Res.string.inf_braco), style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
                r.frisancho.forEach { f ->
                    val titulo = when (f.indicador) { "CB" -> Res.string.inf_cb; "DCT" -> Res.string.inf_dct; "DCSE" -> Res.string.inf_dcse; "AMBC" -> Res.string.inf_ambc; "AGB" -> Res.string.inf_agb; else -> null }
                    if (titulo != null) {
                        if (f.valor != null) Text(stringResource(Res.string.inf_linha_valor, stringResource(titulo), textoDecimal(f.valor), f.unidade.orEmpty())) else Text(stringResource(titulo))
                        f.classificacao?.let { Text(classeInfantil(it)) }
                        f.adequacaoPercentual?.let { Text(stringResource(Res.string.inf_adequacao, textoDecimal(it))) }
                        if (f.classificacao == null) Text(motivoInfantil(f.motivo))
                    }
                }
            }
            r.composicao?.let { c ->
                Text(stringResource(Res.string.inf_composicao), style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
                if (c.percentualGordura == null) Text(motivoInfantil(c.motivosNulos?.get("percentualGordura")))
                else Text(stringResource(Res.string.inf_linha_pct, stringResource(Res.string.inf_gordura), textoDecimal(c.percentualGordura)))
                c.classificacaoPercentualGordura?.let { codigo -> ClassePercentualGordura.entries.firstOrNull { it.name == codigo }?.let { Text(stringResource(rotuloClassePercentual(it))) } }
                c.pesoGordoKg?.let { Text(stringResource(Res.string.inf_linha_valor, stringResource(Res.string.avaliacao_rosca_gordura), textoDecimal(it), stringResource(Res.string.inf_unidade_kg))) }
                c.pesoMagroKg?.let { Text(stringResource(Res.string.inf_linha_valor, stringResource(Res.string.avaliacao_rosca_magro), textoDecimal(it), stringResource(Res.string.inf_unidade_kg))) }
            }
            r.energia?.let { e ->
                Text(stringResource(Res.string.inf_energia), style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
                if (!paciente) {
                    LinhaEnergia(stringResource(Res.string.inf_geb), e.gebIomKcal, e.motivosNulos?.get("energia"))
                    LinhaEnergia(stringResource(Res.string.inf_get_iom), e.getIomKcal, e.motivosNulos?.get("getIomKcal") ?: e.motivosNulos?.get("energia"))
                }
                LinhaEnergia(stringResource(if (paciente) Res.string.inf_energia_paciente else Res.string.inf_get_rdi), e.getRdiKcal, e.motivosNulos?.get("getRdiKcal") ?: e.motivosNulos?.get("energia"))
            }
            if (paciente) Text(stringResource(Res.string.inf_orientacao), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable private fun LinhaEnergia(rotulo: String, valor: Double?, motivo: String?) {
    if (valor == null) { Text(rotulo); Text(motivoInfantil(motivo), style = MaterialTheme.typography.bodySmall) }
    else Text(stringResource(Res.string.inf_linha_valor, rotulo, textoDecimal(valor), stringResource(Res.string.inf_unidade_kcal)))
}

@Composable
fun textoIntervaloInfantil(rotulo: String, intervalo: IntervaloCurvaDto): String {
    val a = intervalo.inferior; val b = intervalo.superior
    return when {
        a != null && b != null && a == b -> stringResource(Res.string.inf_intervalo_exato, rotulo, textoDecimal(a))
        a != null && b != null -> stringResource(Res.string.inf_intervalo_entre, rotulo, textoDecimal(a), textoDecimal(b))
        a == null && b != null -> stringResource(Res.string.inf_intervalo_abaixo, rotulo, textoDecimal(b))
        a != null -> stringResource(Res.string.inf_intervalo_acima, rotulo, textoDecimal(a))
        else -> stringResource(Res.string.inf_sem_resultado)
    }
}

@Composable
private fun motivoInfantil(codigo: String?): String = stringResource(when (codigo) {
    "FALTA_SEXO" -> Res.string.inf_falta_sexo; "FALTA_DOBRAS" -> Res.string.inf_falta_dobras
    "FALTA_ETNIA_SLAUGHTER" -> Res.string.inf_falta_etnia; "FALTA_FATOR_ATIVIDADE" -> Res.string.inf_falta_fator
    "REFERENCIA_INCONSISTENTE_G16" -> Res.string.inf_g16; "REFERENCIA_INCONSISTENTE" -> Res.string.inf_referencia_inconsistente
    "REFERENCIA_INDISPONIVEL", "IDADE_FORA_DA_REFERENCIA", "IDADE_FORA_DA_FORMULA" -> Res.string.inf_sem_referencia
    else -> Res.string.inf_sem_resultado
})

@Composable
private fun classeInfantil(codigo: String): String = stringResource(when (codigo) {
    "MB" -> Res.string.inf_muito_baixo; "B" -> Res.string.inf_baixo; "A", "NO", "ADEQ" -> Res.string.inf_adequado; "AC" -> Res.string.inf_acima
    "MA" -> Res.string.inf_magreza_acentuada; "M" -> Res.string.inf_magreza; "BP" -> Res.string.inf_baixo_peso
    "S", "SP" -> Res.string.inf_sobrepeso; "O", "OB" -> Res.string.inf_obesidade; "OG" -> Res.string.inf_obesidade_grave
    "ABAIXO_P5" -> Res.string.inf_percentil_baixo; "P5_A_P10" -> Res.string.inf_percentil_intermediario; "P10_A_P90" -> Res.string.inf_percentil_normal; "ACIMA_P90" -> Res.string.inf_percentil_alto
    "DG" -> Res.string.inf_deficit_grave; "DM" -> Res.string.inf_deficit_moderado; "DL" -> Res.string.inf_deficit_leve; "EP" -> Res.string.inf_excesso
    "EXTREMAMENTE_BAIXO" -> Res.string.inf_nascer_extremo; "MUITO_BAIXO" -> Res.string.inf_nascer_muito_baixo; "BAIXO" -> Res.string.inf_nascer_baixo
    "INSUFICIENTE" -> Res.string.inf_nascer_insuficiente; "NORMAL" -> Res.string.inf_nascer_normal; "MACROSSOMIA" -> Res.string.inf_nascer_macrossomia
    else -> Res.string.inf_sem_resultado
})
