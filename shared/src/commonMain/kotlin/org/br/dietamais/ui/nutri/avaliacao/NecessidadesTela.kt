package org.br.dietamais.ui.nutri.avaliacao

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import org.br.dietamais.data.remoto.dto.Sexo
import org.br.dietamais.dominio.avaliacao.*
import org.br.dietamais.dominio.calculos.*
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.comum.graficos.GraficoComTabela
import org.br.dietamais.ui.comum.graficos.GraficoRosca
import org.br.dietamais.ui.comum.graficos.ItemGrafico
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Passo "Necessidades" (MOB-52): método do GET, nível de atividade, objetivo, protocolos das metas; o resultado aparece logo abaixo. */
@Composable
internal fun PassoNecessidades(sexo: Sexo?, escolhas: EscolhasEnergia, previa: PreviaEnergia, aoMudar: (EscolhasEnergia) -> Unit, habilitado: Boolean) {
    Opcoes(Res.string.necessidades_metodo, listOf(MetodoEnergia.FAO_1985, MetodoEnergia.FAO_2004, MetodoEnergia.IOM_2005, MetodoEnergia.RDI_2005), escolhas.metodo,
        { aoMudar(escolhas.copy(metodo = it)) }, habilitado, ::rotuloMetodo)
    if (escolhas.metodo == MetodoEnergia.MET) Text(stringResource(Res.string.necessidades_met_mantido), style = MaterialTheme.typography.bodySmall)
    OpcoesOpcionais(Res.string.necessidades_nivel, NivelAtividade.entries, escolhas.nivel, { aoMudar(escolhas.copy(nivel = it)) }, habilitado, ::rotuloNivel)
    Opcoes(Res.string.necessidades_objetivo, ObjetivoPeso.entries, escolhas.objetivo, { aoMudar(escolhas.copy(objetivo = it)) }, habilitado, ::rotuloObjetivo)
    if (escolhas.objetivo != ObjetivoPeso.MANUTENCAO) CampoNumero(stringResource(Res.string.necessidades_kg_por_mes), escolhas.kgPorMes,
        { aoMudar(escolhas.copy(kgPorMes = it.take(6))) }, habilitado = habilitado)
    if (sexo != Sexo.MASCULINO) Opcoes(Res.string.necessidades_condicao, CondicaoEnergia.entries, escolhas.condicao, { aoMudar(escolhas.copy(condicao = it)) }, habilitado, ::rotuloCondicao)
    Text(stringResource(Res.string.necessidades_protocolos), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
    Opcoes(Res.string.necessidades_carboidrato, ProtocoloCarboidrato.entries, escolhas.carboidrato, { aoMudar(escolhas.copy(carboidrato = it)) }, habilitado, ::rotuloCarboidrato)
    Opcoes(Res.string.necessidades_proteina, ProtocoloProteina.entries, escolhas.proteina, { aoMudar(escolhas.copy(proteina = it)) }, habilitado, ::rotuloProteina)
    Opcoes(Res.string.necessidades_lipidio, ProtocoloLipidio.entries, escolhas.lipidio, { aoMudar(escolhas.copy(lipidio = it)) }, habilitado, ::rotuloLipidio)
    Row(Modifier.fillMaxWidth().heightIn(min = Dimens.alvoToque), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
        Text(stringResource(Res.string.necessidades_dislipidemico), Modifier.weight(1f))
        Switch(escolhas.dislipidemico, { aoMudar(escolhas.copy(dislipidemico = it)) }, enabled = habilitado)
    }
    ResultadoNecessidades(previa, escolhas)
}

/** Resultado ao vivo: GEB, GET, VET, kcal/kg, as metas de macro em rosca e em lista, e o que falta para calcular (RN-CAL-04). */
@Composable
fun ResultadoNecessidades(previa: PreviaEnergia, escolhas: EscolhasEnergia, modifier: Modifier = Modifier) {
    CardDieta(modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }) {
        Text(stringResource(Res.string.necessidades_titulo_resultado), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        previa.gebKcal?.let { Text(stringResource(Res.string.necessidades_geb, textoDecimal(it)), style = MaterialTheme.typography.bodyMedium) }
        previa.getKcal?.let { Text(stringResource(Res.string.necessidades_get, textoDecimal(it)), style = MaterialTheme.typography.bodyMedium) }
        previa.vetKcal?.let { Text(stringResource(Res.string.necessidades_vet, textoDecimal(it)), style = MaterialTheme.typography.titleSmall) }
        previa.kcalPorKg?.let { Text(stringResource(Res.string.necessidades_kcal_kg, textoDecimal(it)), style = MaterialTheme.typography.bodySmall) }
        val metas = listOfNotNull(
            previa.carboidrato?.let { Res.string.necessidades_carboidrato to it }, previa.proteina?.let { Res.string.necessidades_proteina to it },
            previa.lipidio?.let { Res.string.necessidades_lipidio to it })
        if (metas.isNotEmpty() && previa.vetKcal != null) {
            val nomes = metas.map { stringResource(it.first) }
            val linhas = metas.mapIndexed { i, (_, m) -> nomes[i] to textoFaixa(m) }
            GraficoComTabela(linhas) {
                GraficoRosca(metas.mapIndexed { i, (_, m) -> ItemGrafico(nomes[i], (m.percentualMinimo + (m.percentualMaximo ?: m.percentualMinimo)) / 2) },
                    centro = stringResource(Res.string.necessidades_rosca_centro, textoDecimal(previa.vetKcal)))
            }
            Text(stringResource(Res.string.necessidades_rosca_nota), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            metas.forEachIndexed { i, (_, m) -> Text(textoMeta(nomes[i], m), style = MaterialTheme.typography.bodyMedium) }
        }
        previa.colesterolMaximoMg?.let { Text(stringResource(Res.string.necessidades_colesterol, textoDecimal(it)), style = MaterialTheme.typography.bodyMedium) }
        previa.faltas.forEach { Text(stringResource(textoFaltaEnergia(it)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Text(stringResource(Res.string.avaliacao_previa_nota), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun textoMeta(nome: String, m: MetaMacro): String =
    if (m.percentualMaximo == null) stringResource(Res.string.necessidades_meta_minimo, nome, textoDecimal(m.percentualMinimo), textoDecimal(m.gramasMinimo))
    else stringResource(Res.string.necessidades_meta_percentual, nome, textoIntervalo(m.percentualMinimo, m.percentualMaximo), textoIntervalo(m.gramasMinimo, m.gramasMaximo))

private fun textoFaixa(m: MetaMacro): String = if (m.percentualMaximo == null) "${textoDecimal(m.percentualMinimo)}%+" else "${textoIntervalo(m.percentualMinimo, m.percentualMaximo)}%"
internal fun textoIntervalo(min: Double, max: Double?): String = if (max == null || max == min) textoDecimal(min) else "${textoDecimal(min)} a ${textoDecimal(max)}"

@Composable
private fun <T> Opcoes(titulo: StringResource, opcoes: List<T>, atual: T, aoEscolher: (T) -> Unit, habilitado: Boolean, rotulo: (T) -> StringResource) {
    Text(stringResource(titulo), style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
        opcoes.forEach { valor ->
            FilterChip(selected = valor == atual, onClick = { aoEscolher(valor) }, label = { Text(stringResource(rotulo(valor))) }, enabled = habilitado,
                modifier = Modifier.heightIn(min = Dimens.alvoToque))
        }
    }
}

/** Como [Opcoes], mas tocar na opção já escolhida limpa a escolha (o nível de atividade começa vazio). */
@Composable
private fun <T> OpcoesOpcionais(titulo: StringResource, opcoes: List<T>, atual: T?, aoEscolher: (T?) -> Unit, habilitado: Boolean, rotulo: (T) -> StringResource) {
    Text(stringResource(titulo), style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
        opcoes.forEach { valor ->
            FilterChip(selected = valor == atual, onClick = { aoEscolher(if (valor == atual) null else valor) }, label = { Text(stringResource(rotulo(valor))) }, enabled = habilitado,
                modifier = Modifier.heightIn(min = Dimens.alvoToque))
        }
    }
}

fun rotuloMetodo(m: MetodoEnergia): StringResource = when (m) {
    MetodoEnergia.FAO_1985 -> Res.string.necessidades_metodo_fao_1985; MetodoEnergia.FAO_2004 -> Res.string.necessidades_metodo_fao_2004
    MetodoEnergia.IOM_2005 -> Res.string.necessidades_metodo_iom_2005; MetodoEnergia.RDI_2005 -> Res.string.necessidades_metodo_rdi_2005
    MetodoEnergia.MET -> Res.string.necessidades_metodo_met
}
fun rotuloNivel(n: NivelAtividade): StringResource = when (n) {
    NivelAtividade.SEDENTARIO -> Res.string.necessidades_nivel_sedentario; NivelAtividade.LEVE -> Res.string.necessidades_nivel_leve
    NivelAtividade.MODERADO -> Res.string.necessidades_nivel_moderado; NivelAtividade.INTENSO -> Res.string.necessidades_nivel_intenso
}
fun rotuloObjetivo(o: ObjetivoPeso): StringResource = when (o) {
    ObjetivoPeso.MANUTENCAO -> Res.string.necessidades_objetivo_manutencao; ObjetivoPeso.GANHO -> Res.string.necessidades_objetivo_ganho; ObjetivoPeso.PERDA -> Res.string.necessidades_objetivo_perda
}
fun rotuloCondicao(c: CondicaoEnergia): StringResource = when (c) {
    CondicaoEnergia.NENHUMA -> Res.string.necessidades_condicao_nenhuma; CondicaoEnergia.GESTANTE -> Res.string.necessidades_condicao_gestante
    CondicaoEnergia.LACTANTE_ATE_6_MESES -> Res.string.necessidades_condicao_lactante_6; CondicaoEnergia.LACTANTE_7_A_12_MESES -> Res.string.necessidades_condicao_lactante_12
}
fun rotuloCarboidrato(p: ProtocoloCarboidrato): StringResource = when (p) {
    ProtocoloCarboidrato.FAIXA_DRI -> Res.string.necessidades_carbo_faixa_dri; ProtocoloCarboidrato.MINIMO_RDA -> Res.string.necessidades_carbo_minimo_rda
    ProtocoloCarboidrato.FAIXA_ESPORTIVA -> Res.string.necessidades_carbo_faixa_esportiva; ProtocoloCarboidrato.POR_KG -> Res.string.necessidades_carbo_por_kg
}
fun rotuloProteina(p: ProtocoloProteina): StringResource = when (p) {
    ProtocoloProteina.FAIXA_DRI -> Res.string.necessidades_ptn_faixa_dri; ProtocoloProteina.RDA -> Res.string.necessidades_ptn_rda
    ProtocoloProteina.MINIMO_15 -> Res.string.necessidades_ptn_minimo_15; ProtocoloProteina.ENDURANCE -> Res.string.necessidades_ptn_endurance
    ProtocoloProteina.HIPERTROFIA -> Res.string.necessidades_ptn_hipertrofia
}
fun rotuloLipidio(p: ProtocoloLipidio): StringResource = when (p) {
    ProtocoloLipidio.MINIMO_20 -> Res.string.necessidades_lip_minimo_20; ProtocoloLipidio.MINIMO_25 -> Res.string.necessidades_lip_minimo_25
}
fun textoFaltaEnergia(f: FaltaEnergia): StringResource = when (f) {
    FaltaEnergia.PESO_ESTATURA -> Res.string.necessidades_falta_peso_estatura; FaltaEnergia.SEXO -> Res.string.necessidades_falta_sexo
    FaltaEnergia.NASCIMENTO -> Res.string.necessidades_falta_nascimento; FaltaEnergia.FAIXA_ETARIA -> Res.string.necessidades_falta_faixa
    FaltaEnergia.NIVEL_ATIVIDADE -> Res.string.necessidades_falta_nivel; FaltaEnergia.METODO_MET -> Res.string.necessidades_falta_met
}
