package org.br.dietamais.dominio.dieta

import org.br.dietamais.data.remoto.dto.AdequacaoNutrienteResposta

enum class ClasseDri { ABAIXO, ADEQUADO, ACIMA, OUTRA }

/** Uma linha da lista de micronutrientes (MOB-65, ART-P4-04): nome legível, percentual da meta DRI e classe. */
data class LinhaDri(val codigo: String, val nome: String, val percentual: Double, val classe: ClasseDri, val valor: String? = null)

fun classeDri(texto: String) = ClasseDri.entries.firstOrNull { it.name == texto.trim().uppercase() && it != ClasseDri.OUTRA } ?: ClasseDri.OUTRA

/** "calcio" → "Cálcio"; "vitamina_b12" → "Vitamina B12". Códigos desconhecidos viram texto com a primeira letra maiúscula. */
fun nomeDoNutriente(codigo: String): String {
    val conhecido = NOMES_DRI[codigo]
    if (conhecido != null) return conhecido
    val partes = codigo.split('_').filter { it.isNotEmpty() }
    if (partes.isEmpty()) return codigo
    return partes.mapIndexed { i, p -> if (p.length <= 3 && p.any { it.isDigit() }) p.uppercase() else if (i == 0) p.replaceFirstChar { it.uppercase() } else p }.joinToString(" ")
}

private val NOMES_DRI = mapOf(
    "fibras" to "Fibras", "sodio" to "Sódio", "potassio" to "Potássio", "calcio" to "Cálcio", "cobre" to "Cobre", "iodo" to "Iodo", "magnesio" to "Magnésio",
    "manganes" to "Manganês", "fosforo" to "Fósforo", "selenio" to "Selênio", "zinco" to "Zinco", "ferro" to "Ferro", "agua" to "Água",
    "vitamina_a" to "Vitamina A", "vitamina_c" to "Vitamina C", "vitamina_d" to "Vitamina D", "vitamina_e" to "Vitamina E",
)

/** Linhas ordenadas: primeiro o que foge da meta (abaixo e acima), depois o adequado; dentro de cada grupo, pelo nome. */
fun linhasDri(adequacao: List<AdequacaoNutrienteResposta>?): List<LinhaDri> =
    adequacao.orEmpty().map { LinhaDri(it.nutriente, it.nome?.takeIf { n -> n.isNotBlank() } ?: nomeDoNutriente(it.nutriente), it.percentual, classeDri(it.classe), textoValorDri(it)) }
        .sortedWith(compareBy<LinhaDri> { if (it.classe == ClasseDri.ADEQUADO) 1 else 0 }.thenBy { it.nome })

/** "8 mg" (ART-P4-04): o total na unidade da meta, sem o "/dia"; sem unidade do servidor não mostra valor (só o percentual). */
fun textoValorDri(a: AdequacaoNutrienteResposta): String? {
    val unidade = a.unidade?.substringBefore('/')?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return org.br.dietamais.dominio.avaliacao.textoDecimal(a.total) + " " + unidade
}
