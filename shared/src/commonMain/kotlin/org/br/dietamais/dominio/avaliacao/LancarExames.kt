package org.br.dietamais.dominio.avaliacao

import org.br.dietamais.data.remoto.dto.*

/** Faixa do catálogo de exames da avaliação (criança, adolescente, adulto; idoso usa adulto). Sem faixa (ficha sem nascimento) a nutri não lança exame. */
fun faixaDoCatalogo(faixa: FaixaEtariaAvaliacao?): String? = when (faixa) {
    FaixaEtariaAvaliacao.CRIANCA -> "CRIANCA"
    FaixaEtariaAvaliacao.ADOLESCENTE -> "ADOLESCENTE"
    FaixaEtariaAvaliacao.ADULTO, FaixaEtariaAvaliacao.IDOSO -> "ADULTO"
    else -> null
}

/** Texto do campo a partir do exame já gravado: o número com vírgula (pt-BR) ou o texto do exame qualitativo. */
fun textoDoExame(e: ExameResultadoDto): String = e.valor?.let(::textoDecimal) ?: e.texto.orEmpty()

/**
 * Lista que vai para o servidor (RN-EXA-01): só entram campos preenchidos; numérico leva `valor`, qualitativo leva `texto`.
 * Campo numérico que não é número vira erro (código na lista de inválidos) e nada é enviado.
 */
data class ExamesParaEnviar(val lancados: List<ExameLancadoDto>, val invalidos: Set<String>)

fun montarExames(catalogo: List<ExameCatalogoDto>, valores: Map<String, String>, condicoes: Map<String, String>): ExamesParaEnviar {
    val invalidos = LinkedHashSet<String>(); val lancados = ArrayList<ExameLancadoDto>()
    catalogo.forEach { e ->
        val bruto = valores[e.codigo].orEmpty().trim()
        if (bruto.isEmpty()) return@forEach
        val condicao = condicoes[e.codigo]?.takeIf { it in e.condicoes }
        if (e.qualitativo) lancados += ExameLancadoDto(e.codigo, texto = bruto.take(100), condicao = condicao)
        else {
            val numero = lerDecimal(bruto)
            if (numero == null || numero < 0) invalidos += e.codigo else lancados += ExameLancadoDto(e.codigo, valor = numero, condicao = condicao)
        }
    }
    return ExamesParaEnviar(lancados, invalidos)
}

private fun semAcento(s: String) = s.lowercase().map { when (it) { 'á', 'à', 'â', 'ã' -> 'a'; 'é', 'ê' -> 'e'; 'í' -> 'i'; 'ó', 'ô', 'õ' -> 'o'; 'ú', 'ü' -> 'u'; 'ç' -> 'c'; else -> it } }.joinToString("")

/** Busca pelo nome, sem acento nem maiúscula; [secao] nula = todas. */
fun filtrarCatalogo(catalogo: List<ExameCatalogoDto>, busca: String, secao: String?): List<ExameCatalogoDto> {
    val termo = semAcento(busca.trim())
    return catalogo.filter { (secao == null || it.secao == secao) && (termo.isEmpty() || semAcento(it.nome).contains(termo)) }
}
