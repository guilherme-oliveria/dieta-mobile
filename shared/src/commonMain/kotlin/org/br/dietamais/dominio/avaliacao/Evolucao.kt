package org.br.dietamais.dominio.avaliacao

import org.br.dietamais.data.remoto.dto.AvaliacaoResposta
import org.br.dietamais.data.remoto.dto.PesoResposta
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/** Séries do gráfico de evolução (MOB-23). Calculadas no aparelho a partir do que o sync já trouxe, então funcionam offline. */
enum class SerieEvolucao(val unidade: String, val ler: (AvaliacaoResposta) -> Double?) {
    PESO("kg", { it.pesoKg }),
    PESO_EM_CASA("kg", { null }),
    IMC("kg/m²", { it.imc }),
    PERCENTUAL_GORDURA("%", { it.composicao.percentualGordura }),
    PESO_GORDO("kg", { it.composicao.pesoGordoKg }),
    PESO_MAGRO("kg", { it.composicao.pesoMagroKg }),
    CINTURA("cm", { it.perimetros.cintura }),
    ABDOMEN("cm", { it.perimetros.abdomen }),
    QUADRIL("cm", { it.perimetros.quadril }),
    TORAX("cm", { it.perimetros.torax }),
    PESCOCO("cm", { it.perimetros.pescoco }),
    BRACO("cm", { it.perimetros.braco }),
    ANTEBRACO("cm", { it.perimetros.antebraco }),
    COXA("cm", { it.perimetros.coxa }),
    PANTURRILHA("cm", { it.perimetros.panturrilha }),
}

data class PontoSerie(val instante: Instant, val valor: Double)

enum class IntervaloEvolucao(val duracao: Duration?) { TRES_MESES(90.days), SEIS_MESES(180.days), UM_ANO(365.days), TUDO(null) }

/** Séries em ordem cronológica crescente; as sem nenhum ponto ficam de fora. */
fun construirSeries(avaliacoes: List<AvaliacaoResposta>, pesosEmCasa: List<PesoResposta>): Map<SerieEvolucao, List<PontoSerie>> {
    val ordenadas = avaliacoes.sortedBy { it.dataHora }
    val resultado = LinkedHashMap<SerieEvolucao, List<PontoSerie>>()
    for (serie in SerieEvolucao.entries) {
        val pontos = if (serie == SerieEvolucao.PESO_EM_CASA) pesosEmCasa.sortedBy { it.registradoEm }.map { PontoSerie(it.registradoEm, it.pesoKg) }
        else ordenadas.mapNotNull { a -> serie.ler(a)?.let { PontoSerie(a.dataHora, it) } }
        if (pontos.isNotEmpty()) resultado[serie] = pontos
    }
    return resultado
}

/** Mantém só os pontos dentro do intervalo, contado para trás a partir de `agora`. */
fun recortar(pontos: List<PontoSerie>, intervalo: IntervaloEvolucao, agora: Instant): List<PontoSerie> {
    val duracao = intervalo.duracao ?: return pontos
    val inicio = agora - duracao
    return pontos.filter { it.instante >= inicio }
}

/** Variação entre o primeiro e o último ponto; nula com menos de 2 pontos. */
fun variacaoTotal(pontos: List<PontoSerie>): Double? =
    if (pontos.size < 2) null else kotlin.math.round((pontos.last().valor - pontos.first().valor) * 100) / 100
