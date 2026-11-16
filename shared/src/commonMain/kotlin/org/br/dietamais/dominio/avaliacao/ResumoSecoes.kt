package org.br.dietamais.dominio.avaliacao

/** Quantas medidas de uma seção já foram preenchidas (ART-P1-03: o resumo de cada seção da avaliação). */
data class ProgressoSecao(val secao: SecaoAvaliacao, val preenchidas: Int, val total: Int) {
    val completa get() = total > 0 && preenchidas == total
    val vazia get() = preenchidas == 0
}

/** Conta só os campos com número válido; [medidas] limita o que existe na seção (por exemplo, dobras do protocolo escolhido). */
fun progressoDasSecoes(valores: Map<MedidaAvaliacao, String>, secoes: List<SecaoAvaliacao> = SecaoAvaliacao.entries,
    medidas: (SecaoAvaliacao) -> List<MedidaAvaliacao> = { s -> MedidaAvaliacao.entries.filter { it.secao == s } }): List<ProgressoSecao> =
    secoes.map { s ->
        val lista = medidas(s)
        ProgressoSecao(s, lista.count { m -> valores[m]?.trim()?.replace(',', '.')?.toDoubleOrNull() != null }, lista.size)
    }
