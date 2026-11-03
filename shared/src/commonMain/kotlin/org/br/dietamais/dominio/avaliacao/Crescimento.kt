package org.br.dietamais.dominio.avaliacao

import org.br.dietamais.data.remoto.dto.AvaliacaoResposta
import org.br.dietamais.data.remoto.dto.CurvaCrescimentoAvaliacaoDto
import kotlin.time.Instant
import kotlin.uuid.Uuid

/** RN-CAL-08: rótulos dos oito cortes congelados pelo contrato BE-54/55b, sem interpolação. */
val percentisCrescimento = listOf("P0,1", "P3", "P10", "P15", "P50", "P85", "P97", "P99")
val indicadoresCrescimento = listOf("PESO_IDADE", "ESTATURA_IDADE", "IMC_IDADE")

data class MedidaCrescimento(val avaliacaoId: Uuid, val data: Instant, val idadeMeses: Int, val valor: Double,
    val curva: CurvaCrescimentoAvaliacaoDto)
data class ReferenciaCrescimento(val idadeMeses: Int, val cortes: List<Double>)
data class SerieCrescimento(val selecionado: MedidaCrescimento, val pontos: List<MedidaCrescimento>,
    val referencias: List<ReferenciaCrescimento>, val fonte: String?, val versao: String?, val sexo: String?)

/** Apenas projeção de snapshots. Não busca catálogo atual nem deduz classificação a partir do desenho. */
fun crescimento(avaliacoes: List<AvaliacaoResposta>, selecionada: Uuid, indicador: String): SerieCrescimento? {
    if (indicador !in indicadoresCrescimento) return null
    val base = avaliacoes.firstOrNull { it.id == selecionada } ?: return null
    val snapshot = base.resultadosInfantis ?: return null
    val curva = snapshot.curvas?.firstOrNull { it.indicador == indicador } ?: return null
    fun medida(a: AvaliacaoResposta): MedidaCrescimento? {
        val r = a.resultadosInfantis ?: return null
        val c = r.curvas?.firstOrNull { it.indicador == indicador } ?: return null
        val idade = r.idadeMeses?.takeIf { it in 0..215 } ?: return null
        val valor = c.valor?.takeIf { it.isFinite() && it > 0 } ?: return null
        return MedidaCrescimento(a.id, a.dataHora, idade, valor, c)
    }
    val ponto = medida(base) ?: return null
    // Metadados ausentes não identificam uma referência comum: mostrar apenas a avaliação selecionada.
    val identificada = snapshot.sexoReferencia != null && curva.fonte != null && curva.versaoReferencia != null
    val pontos = avaliacoes.filter { a ->
        val r = a.resultadosInfantis
        val c = r?.curvas?.firstOrNull { it.indicador == indicador }
        a.pacienteId == base.pacienteId && a.vinculoId == base.vinculoId &&
            (a.id == base.id || identificada && r?.sexoReferencia == snapshot.sexoReferencia &&
                c?.fonte == curva.fonte && c?.versaoReferencia == curva.versaoReferencia && c?.unidade == curva.unidade)
    }.mapNotNull(::medida).distinctBy { it.avaliacaoId }.sortedWith(compareBy({ it.idadeMeses }, { it.data }))
    val referencias = pontos.mapNotNull { p ->
        val mes = p.curva.idadeMesesReferencia?.takeIf { it in 0..215 } ?: return@mapNotNull null
        val cortes = p.curva.cortesPercentis ?: return@mapNotNull null
        if (cortes.size != percentisCrescimento.size || cortes.any { !it.isFinite() || it <= 0 } ||
            cortes.zipWithNext().any { (a,b) -> a >= b }) return@mapNotNull null
        ReferenciaCrescimento(mes, cortes)
    }.groupBy { it.idadeMeses }.mapNotNull { (_, linhas) ->
        linhas.distinct().singleOrNull() // cortes divergentes no mesmo mês não viram uma curva inventada
    }.sortedBy { it.idadeMeses }
    return SerieCrescimento(ponto, pontos, referencias, curva.fonte, curva.versaoReferencia, snapshot.sexoReferencia)
}
