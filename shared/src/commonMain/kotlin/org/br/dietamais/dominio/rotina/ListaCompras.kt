package org.br.dietamais.dominio.rotina

import org.br.dietamais.data.remoto.dto.DietaSnapshotResposta
import kotlin.math.round
import kotlin.uuid.Uuid

data class ItemCompra(val alimentoId: Uuid, val nome: String, val gramas: Double, val porcoes: Double, val medidaCaseira: String)
/** [nome] nulo quando o snapshot não traz o nome do grupo (snapshots antigos): esses itens ficam juntos. */
data class GrupoCompras(val nome: String?, val itens: List<ItemCompra>)

/**
 * RN-ROT-04: soma, por alimento de referência, as gramas e porções de todas as refeições da dieta em uso para [dias] dias.
 * Grupos em ordem alfabética (sem nome por último); itens por nome.
 */
fun gerarListaCompras(dieta: DietaSnapshotResposta, dias: Int = 7): List<GrupoCompras> {
    require(dias in 1..60)
    data class Acumulado(var nome: String, var grupo: String?, var gramas: Double, var porcoes: Double, var medida: String)
    val soma = LinkedHashMap<Uuid, Acumulado>()
    // Item à vontade e texto livre não têm quantidade (RN-DIE-12) e ficam fora; item por grupo entra pelo grupo. Alternativa não é comprada por padrão (RN-DIE-11).
    dieta.refeicoes.filterNot { it.ehAlternativa }.flatMap { it.itens }.forEach { i ->
        val chave = i.alimentoId ?: i.grupoId ?: return@forEach
        val porcoes = i.porcoes ?: return@forEach
        val a = soma.getOrPut(chave) { Acumulado(i.nomeAlimento.orEmpty(), i.grupoNome, 0.0, 0.0, i.medidaCaseira.orEmpty()) }
        a.gramas += i.gramas ?: 0.0; a.porcoes += porcoes
    }
    return soma.map { (id, a) -> a.grupo to ItemCompra(id, a.nome, arredondar(a.gramas * dias), arredondar(a.porcoes * dias), a.medida) }
        .groupBy({ it.first }, { it.second })
        .map { (grupo, itens) -> GrupoCompras(grupo, itens.sortedBy { it.nome.lowercase() }) }
        .sortedWith(compareBy({ it.nome == null }, { it.nome?.lowercase() }))
}

private fun arredondar(v: Double) = round(v * 100 + 1e-6) / 100

/** Peso em texto pt-BR: abaixo de 1 kg em gramas, depois em kg (ex.: "850 g", "1,4 kg"). */
fun textoPeso(gramas: Double): String {
    if (gramas < 1000) return "${formatarDecimal(gramas, 1)} g"
    return "${formatarDecimal(gramas / 1000, 2)} kg"
}

private fun formatarDecimal(v: Double, casas: Int): String {
    val fator = if (casas == 1) 10 else 100
    val inteiro = round(v * fator + 1e-6).toLong()
    val parteInteira = inteiro / fator; val fracao = inteiro % fator
    if (fracao == 0L) return parteInteira.toString()
    val txt = fracao.toString().padStart(casas, '0').trimEnd('0')
    return "$parteInteira,$txt"
}

/** Texto para compartilhar (WhatsApp, notas): um item por linha, marcados com [x]. */
fun textoListaCompras(grupos: List<GrupoCompras>, marcados: Set<Uuid> = emptySet(), titulo: String): String = buildString {
    appendLine(titulo)
    grupos.forEach { g ->
        g.nome?.let { appendLine(); appendLine(it) }
        g.itens.forEach { appendLine("${if (it.alimentoId in marcados) "[x]" else "[ ]"} ${it.nome}: ${textoPeso(it.gramas)}") }
    }
}.trimEnd()
