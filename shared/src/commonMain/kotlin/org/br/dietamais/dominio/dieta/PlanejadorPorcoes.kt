package org.br.dietamais.dominio.dieta

import org.br.dietamais.data.remoto.dto.MetasDieta
import org.br.dietamais.data.remoto.dto.OpcaoPorcaoResposta
import org.br.dietamais.data.remoto.dto.SaldoMetasResposta
import kotlin.math.floor

/** Item do planejador: a porção escolhida do grupo e a quantidade de porções. Item à vontade e texto livre não entram na conta. */
data class ItemPorcao(val opcao: OpcaoPorcaoResposta, val porcoes: Double)

/** Total da refeição ou do dia, com 2 casas (RN-CAL-07). `micronutrientes` não inclui energia, carboidrato, proteína e lipídio. */
data class TotaisPorcoes(val energia: Double, val carboidratos: Double, val proteinas: Double, val gorduras: Double,
    val micronutrientes: Map<String, Double>)

/**
 * RN-CAL-07, mesma conta do backend (`CalculadoraTotais.porGrupo`): soma porções × perfil da porção do grupo sem arredondar e só o total
 * arredonda a 2 casas (meio para cima). Os vetores `contrato/calculos/dieta-porcoes.json` valem nos dois lados. É só feedback ao vivo:
 * quem decide e grava o total é o backend.
 */
object PlanejadorPorcoes {
    private val MACROS = setOf("energia", "gli", "pro", "lip")

    fun somar(itens: List<ItemPorcao>): TotaisPorcoes {
        var energia = 0.0; var gli = 0.0; var pro = 0.0; var lip = 0.0
        val micros = LinkedHashMap<String, Double>()
        for (i in itens) {
            energia += (i.opcao.nutrientes["energia"] ?: i.opcao.kcal) * i.porcoes
            gli += (i.opcao.nutrientes["gli"] ?: 0.0) * i.porcoes
            pro += (i.opcao.nutrientes["pro"] ?: 0.0) * i.porcoes
            lip += (i.opcao.nutrientes["lip"] ?: 0.0) * i.porcoes
            // RN-DIE-13: só soma quem informa o nutriente; falta de dado nunca vira zero.
            for ((cod, v) in i.opcao.nutrientes) if (cod !in MACROS) micros[cod] = (micros[cod] ?: 0.0) + v * i.porcoes
        }
        return TotaisPorcoes(arredondar(energia), arredondar(gli), arredondar(pro), arredondar(lip), micros.mapValues { arredondar(it.value) })
    }

    /** Meta menos o total do dia; carboidrato e proteína a 4 kcal/g, lipídio a 9 kcal/g sobre o VET. Negativo = passou da meta. */
    fun saldo(metas: MetasDieta, dia: TotaisPorcoes) = SaldoMetasResposta(
        kcal = arredondar(metas.vetKcal - dia.energia),
        carboidrato = arredondar(gramasMeta(metas, metas.carboidratoPct, 4.0) - dia.carboidratos),
        proteina = arredondar(gramasMeta(metas, metas.proteinaPct, 4.0) - dia.proteinas),
        lipidio = arredondar(gramasMeta(metas, metas.lipidioPct, 9.0) - dia.gorduras),
    )

    private fun gramasMeta(m: MetasDieta, pct: Double, kcalPorGrama: Double) = m.vetKcal * pct / (100.0 * kcalPorGrama)

    /** 2 casas, meio para cima, com folga para o erro de ponto flutuante da soma. */
    fun arredondar(v: Double): Double = floor(v * 100 + 0.5 + 1e-9) / 100
}
