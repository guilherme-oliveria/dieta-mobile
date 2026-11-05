package org.br.dietamais.data.remoto.dto

import kotlinx.serialization.Serializable
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Serializable(with = ComparecimentoConsultaSerializer::class)
enum class ComparecimentoConsulta { PENDENTE, COMPARECEU, FALTOU, OUTRO }
object ComparecimentoConsultaSerializer : EnumComOutro<ComparecimentoConsulta>("ComparecimentoConsulta", ComparecimentoConsulta.entries.toTypedArray(), ComparecimentoConsulta.OUTRO)

@Serializable(with = FormaPagamentoSerializer::class)
enum class FormaPagamento { DINHEIRO, PIX, CARTAO, CONVENIO, OUTRO }
object FormaPagamentoSerializer : EnumComOutro<FormaPagamento>("FormaPagamento", FormaPagamento.entries.toTypedArray(), FormaPagamento.OUTRO)

/** Financeiro de uma consulta (BE-65, RN-FIN-01): só da nutri, nunca chega ao paciente. `versao` 0 = ainda não lançado. */
@Serializable
data class FinanceiroConsultaResposta(val consultaId: Uuid, val comparecimento: ComparecimentoConsulta, val valor: Double? = null, val formaPagamento: FormaPagamento? = null,
    val pago: Boolean = false, val versao: Long = 0)

@Serializable
data class AtualizarFinanceiroConsultaRequisicao(val comparecimento: ComparecimentoConsulta, val valor: Double? = null, val formaPagamento: FormaPagamento? = null, val pago: Boolean = false)

@Serializable
data class ResumoFinanceiroResposta(val de: Instant, val ate: Instant, val consultas: Int = 0, val compareceram: Int = 0, val faltaram: Int = 0, val pendentes: Int = 0,
    val recebido: Double = 0.0, val aReceber: Double = 0.0, val recebidoPorForma: Map<FormaPagamento, Double> = emptyMap())
