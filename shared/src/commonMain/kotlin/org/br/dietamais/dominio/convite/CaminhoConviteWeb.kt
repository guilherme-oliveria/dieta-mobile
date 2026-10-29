package org.br.dietamais.dominio.convite

private val caminhoConvite = Regex("/convites?/([A-HJ-NP-Z2-9]{8})/?", RegexOption.IGNORE_CASE)

/** Código do convite quando o navegador abriu `/convite/{codigo}` (o link que a nutri compartilha); outro caminho dá nulo. */
fun codigoDoCaminhoWeb(caminho: String): String? =
    if (caminho.length > 64) null else caminhoConvite.matchEntire(caminho)?.groupValues?.get(1)?.uppercase()
