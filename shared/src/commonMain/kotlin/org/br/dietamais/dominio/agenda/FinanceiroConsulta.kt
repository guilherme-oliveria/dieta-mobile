package org.br.dietamais.dominio.agenda

import org.br.dietamais.data.remoto.dto.AtualizarFinanceiroConsultaRequisicao
import org.br.dietamais.data.remoto.dto.ComparecimentoConsulta
import org.br.dietamais.data.remoto.dto.FinanceiroConsultaResposta
import org.br.dietamais.data.remoto.dto.FormaPagamento
import kotlin.math.roundToLong

/** Erros de feedback do formulário (o backend decide, RN-FIN-01). */
enum class ErroFinanceiro { VALOR_INVALIDO, PAGO_SEM_VALOR, PAGO_SEM_FORMA, PAGO_SEM_COMPARECIMENTO }

data class FormularioFinanceiro(val comparecimento: ComparecimentoConsulta = ComparecimentoConsulta.PENDENTE, val valor: String = "", val forma: FormaPagamento? = null, val pago: Boolean = false)

/** Aceita "150", "150,5" e "150.50"; vazio ou inválido (negativo, mais de 2 casas, acima de 99.999.999,99) vira null. */
fun lerValorReais(texto: String): Double? {
    val limpo = texto.trim().replace(',', '.')
    if (!Regex("""\d{1,8}(\.\d{1,2})?""").matches(limpo)) return null
    return limpo.toDouble()
}

fun erroDeFinanceiro(f: FormularioFinanceiro): Set<ErroFinanceiro> = buildSet {
    val valor = lerValorReais(f.valor)
    if (f.valor.isNotBlank() && valor == null) add(ErroFinanceiro.VALOR_INVALIDO)
    if (f.pago) {
        if (f.valor.isNotBlank() && valor == null) return@buildSet
        if (valor == null) add(ErroFinanceiro.PAGO_SEM_VALOR)
        if (f.forma == null) add(ErroFinanceiro.PAGO_SEM_FORMA)
        if (f.comparecimento == ComparecimentoConsulta.PENDENTE) add(ErroFinanceiro.PAGO_SEM_COMPARECIMENTO)
    }
}

fun requisicaoDeFinanceiro(f: FormularioFinanceiro) = AtualizarFinanceiroConsultaRequisicao(f.comparecimento, lerValorReais(f.valor), f.forma, f.pago)

fun formularioDeFinanceiro(r: FinanceiroConsultaResposta) = FormularioFinanceiro(r.comparecimento.takeIf { it != ComparecimentoConsulta.OUTRO } ?: ComparecimentoConsulta.PENDENTE,
    r.valor?.let { textoDeValor(it) }.orEmpty(), r.formaPagamento, r.pago)

/** "250" ou "150,50": sem separador de milhar, para o campo de edição. */
fun textoDeValor(valor: Double): String {
    val centavos = (valor * 100).roundToLong()
    val inteiro = centavos / 100; val resto = (centavos % 100).toInt()
    return if (resto == 0) "$inteiro" else "$inteiro,${resto.toString().padStart(2, '0')}"
}

/** "R$ 1.234,50". */
fun formatarReais(valor: Double): String {
    val centavos = (valor * 100).roundToLong()
    val inteiro = (centavos / 100).toString().reversed().chunked(3).joinToString(".").reversed()
    return "R$ $inteiro,${(centavos % 100).toString().padStart(2, '0')}"
}
