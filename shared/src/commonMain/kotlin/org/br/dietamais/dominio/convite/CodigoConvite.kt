package org.br.dietamais.dominio.convite

private val codigo = Regex("[A-HJ-NP-Z2-9]{8}")
private val esquema = Regex("dietamais://convites?/([A-HJ-NP-Z2-9]{8})", RegexOption.IGNORE_CASE)
private val web = Regex("https://([^/:@?#]+)(?::443)?/convites?/([A-HJ-NP-Z2-9]{8})", RegexOption.IGNORE_CASE)

fun extrairCodigoConvite(entrada: String, dominiosPermitidos: Set<String> = emptySet()): String? {
    if (entrada.length > 2048) return null
    val texto = entrada.trim()
    texto.uppercase().takeIf(codigo::matches)?.let { return it }
    esquema.matchEntire(texto)?.let { return it.groupValues[1].uppercase() }
    val link = web.matchEntire(texto) ?: return null
    if (dominiosPermitidos.none { it.equals(link.groupValues[1], ignoreCase = true) }) return null
    return link.groupValues[2].uppercase()
}
