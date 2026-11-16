package org.br.dietamais.dominio.anamnese

import kotlinx.serialization.json.*
import org.br.dietamais.data.remoto.dto.*
import kotlin.math.abs
import kotlin.uuid.Uuid

/** Toda escrita local invalida resultados anteriores até receber a confirmação do servidor. */
fun QuestionarioResposta.comRespostasLocais(novas: Map<Uuid, JsonElement?>): QuestionarioResposta {
    if (novas.isEmpty()) return this
    val mescladas = respostas.toMutableMap()
    novas.forEach { (id, valor) -> if (valor == null || valor is JsonNull) mescladas.remove(id) else mescladas[id] = valor }
    return copy(respostas = mescladas, derivados = null)
}

/** Valores numericamente iguais (3 e 3.0) não mantêm uma edição já confirmada pendente. */
fun mesmoValorResposta(a: JsonElement?, b: JsonElement?): Boolean =
    if (a is JsonPrimitive && b is JsonPrimitive && !a.isString && !b.isString && a.doubleOrNull != null && b.doubleOrNull != null) a.doubleOrNull == b.doubleOrNull else a == b

fun derivadosAtuais(questionario: QuestionarioResposta, entradas: Map<Uuid, EntradaResposta>): AnamneseDerivada? {
    val perguntas = questionario.blocos.flatMap { it.perguntas }.associateBy { it.chave }
    for ((id, entrada) in entradas) {
        val p = perguntas[id] ?: continue
        val convertida = converter(p, entrada)
        if (!convertida.valida || !mesmoValorResposta(convertida.valor, questionario.respostas[id])) return null
    }
    return questionario.derivados
}

enum class AssuntoAnamnese { PRESSAO, ALCOOL, SAL, OLEO, ATIVIDADE }
private val bebidas = "ABSINTO|CACHACA|CERVEJA|ICE|RUM_GIM|VINHO|VODCA|UISQUE"
private val bebida = Regex("ALCOOL_($bebidas)_(UNIDADE|QUANTIDADE|DIAS_SEMANA)")
private val atividade = Regex("ATIVIDADE_(SEGUNDA|TERCA|QUARTA|QUINTA|SEXTA|SABADO|DOMINGO)_(INICIO|FIM)")

fun assuntoAnamnese(codigo: String?): AssuntoAnamnese? = when {
    codigo == "PA_SISTOLICA" || codigo == "PA_DIASTOLICA" -> AssuntoAnamnese.PRESSAO
    codigo in listOf("ALCOOL_BEBIDA", "ALCOOL_UNIDADE", "ALCOOL_QUANTIDADE", "ALCOOL_DIAS_SEMANA") || codigo != null && bebida.matches(codigo) -> AssuntoAnamnese.ALCOOL
    codigo == "SAL_GRAMAS_MES" || codigo == "SAL_PESSOAS" -> AssuntoAnamnese.SAL
    codigo == "OLEO_ML_MES" || codigo == "OLEO_PESSOAS" -> AssuntoAnamnese.OLEO
    codigo == "ATIVIDADE_INICIO" || codigo == "ATIVIDADE_FIM" || codigo != null && atividade.matches(codigo) -> AssuntoAnamnese.ATIVIDADE
    else -> null
}

/** Somente feedback dos limites publicados; nunca calcula resultados clínicos no cliente. */
fun numeroAnamneseValido(codigo: String?, valor: Double): Boolean {
    if (!valor.isFinite() || abs(valor) > 1_000_000_000) return false
    return when (assuntoAnamnese(codigo)) {
        AssuntoAnamnese.PRESSAO -> valor in 1.0..1000.0 && valor % 1.0 == 0.0
        AssuntoAnamnese.SAL, AssuntoAnamnese.OLEO -> if (codigo!!.endsWith("_PESSOAS")) valor in 1.0..1_000_000.0 && valor % 1.0 == 0.0 else valor >= 0
        AssuntoAnamnese.ALCOOL -> if (codigo!!.endsWith("_DIAS_SEMANA")) valor in 0.0..7.0 else valor >= 0
        else -> true
    }
}
