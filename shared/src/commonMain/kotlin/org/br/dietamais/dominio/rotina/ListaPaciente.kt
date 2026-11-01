package org.br.dietamais.dominio.rotina

import org.br.dietamais.data.remoto.dto.*
import kotlin.math.round
import kotlin.uuid.Uuid

data class MacroPaciente(val gramas: Double, val energia: Double, val percentual: Double)

/** As fatias representam energia (4/4/9 kcal por g), não a proporção entre gramas. Sem metas. */
fun macrosPaciente(t: TotaisNutricionaisResposta): List<MacroPaciente>? {
    val gramas = listOf(t.carboidratos, t.proteinas, t.gorduras)
    if (gramas.any { !it.isFinite() || it < 0 }) return null
    val energia = gramas.zip(listOf(4.0, 4.0, 9.0)) { g, fator -> g * fator }
    val soma = energia.sum().takeIf { it.isFinite() && it > 0 } ?: return null
    return gramas.indices.map { MacroPaciente(gramas[it], energia[it], round(energia[it] / soma * 1000) / 10) }
}

data class GrupoListaPaciente(val id: Uuid, val nome: String?, val kcalPorPorcao: Double?, val alimentos: List<AlimentoResposta>)

/** Nunca completa a lista congelada com o catálogo: restrições já foram aplicadas pelo servidor. */
fun gruposListaPaciente(lista: ListaSubstituicaoResposta, refeicoes: List<RefeicaoResposta>): List<GrupoListaPaciente> {
    val itens = refeicoes.flatMap { it.itens }
    return lista.alimentos.filter { it.ativo }.groupBy { it.grupoId }.map { (id, alimentos) ->
        val doGrupo = itens.filter { it.grupoId == id }
        // Item por grupo pode usar outra opção de porção; não informa a energia da porção-base da lista.
        val base = doGrupo.firstOrNull { i -> i.alimentoId != null && alimentos.any { it.id == i.alimentoId } &&
            i.aVontade != true && i.porcoes?.let { it.isFinite() && it > 0 } == true && i.totais.kcal.isFinite() }
        GrupoListaPaciente(id, doGrupo.firstNotNullOfOrNull { it.grupoNome?.takeIf(String::isNotBlank) },
            base?.let { (it.totais.kcal / it.porcoes!!).takeIf { k -> k.isFinite() && k >= 0 } }, alimentos.sortedBy { semAcentosLista(it.nome) })
    }.sortedBy { semAcentosLista(it.nome.orEmpty()) }
}

fun filtrarListaPaciente(grupos: List<GrupoListaPaciente>, busca: String, grupoId: Uuid?): List<GrupoListaPaciente> {
    val termo = semAcentosLista(busca.trim())
    return grupos.filter { grupoId == null || it.id == grupoId }.mapNotNull { g ->
        val alimentos = g.alimentos.filter { termo.isBlank() || semAcentosLista(it.nome).contains(termo) || semAcentosLista(g.nome.orEmpty()).contains(termo) }
        g.copy(alimentos = alimentos).takeIf { alimentos.isNotEmpty() }
    }
}

private fun semAcentosLista(s: String) = s.lowercase().map { when (it) {
    'á', 'à', 'â', 'ã', 'ä' -> 'a'; 'é', 'ê', 'è', 'ë' -> 'e'; 'í', 'ì', 'î', 'ï' -> 'i'
    'ó', 'ô', 'õ', 'ò', 'ö' -> 'o'; 'ú', 'ü', 'ù', 'û' -> 'u'; 'ç' -> 'c'; else -> it
} }.joinToString("")
