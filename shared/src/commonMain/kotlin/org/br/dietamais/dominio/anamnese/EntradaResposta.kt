package org.br.dietamais.dominio.anamnese

import kotlinx.serialization.json.JsonElement
import org.br.dietamais.data.remoto.dto.PerguntaQuestionario
import org.br.dietamais.data.remoto.dto.TipoPergunta
import org.br.dietamais.dominio.avaliacao.textoDecimal
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement
import org.br.dietamais.data.remoto.JsonContrato
import kotlin.uuid.Uuid

/** O que o paciente digitou numa pergunta e ainda não foi gravado: texto cru, linhas da lista por refeição ou um valor já pronto. */
sealed interface EntradaResposta {
    data class Texto(val texto: String) : EntradaResposta
    data class Itens(val itens: List<ItemRefeicao>) : EntradaResposta
    data class Valor(val valor: JsonElement?) : EntradaResposta
}

/** `valor` nulo = apagar a resposta; `valida` falso = a digitação não vira resposta (o app não grava). */
data class RespostaConvertida(val valor: JsonElement?, val valida: Boolean)

fun converter(pergunta: PerguntaQuestionario, entrada: EntradaResposta): RespostaConvertida = when (entrada) {
    is EntradaResposta.Texto -> {
        val texto = entrada.texto.trim()
        when {
            texto.isEmpty() -> RespostaConvertida(null, true)
            pergunta.tipo == TipoPergunta.TEXTO -> jsonTexto(texto).let { RespostaConvertida(it, respostaValida(pergunta, it)) }
            pergunta.tipo == TipoPergunta.NUMERO -> jsonNumero(texto).let { RespostaConvertida(it, it != null) }
            pergunta.tipo == TipoPergunta.HORARIO -> JsonPrimitive(texto).let { RespostaConvertida(it, respostaValida(pergunta, it)) }
            else -> RespostaConvertida(null, false)
        }
    }
    is EntradaResposta.Itens -> {
        val preenchidos = entrada.itens.filter { it.descricao.isNotBlank() || it.refeicao.isNotBlank() }
            .map { it.copy(refeicao = it.refeicao.trim(), descricao = it.descricao.trim(), horario = it.horario?.trim()?.ifEmpty { null }) }
        if (preenchidos.isEmpty()) RespostaConvertida(null, true) else jsonItens(preenchidos).let { RespostaConvertida(it, respostaValida(pergunta, it)) }
    }
    is EntradaResposta.Valor -> RespostaConvertida(entrada.valor, entrada.valor == null || respostaValida(pergunta, entrada.valor))
}

/** Valor atual para preencher o campo: a digitação em andamento ou, sem ela, a resposta já gravada. */
fun entradaInicial(pergunta: PerguntaQuestionario, gravada: JsonElement?): EntradaResposta = when (pergunta.tipo) {
    TipoPergunta.TEXTO, TipoPergunta.HORARIO -> EntradaResposta.Texto((gravada as? JsonPrimitive)?.contentOrNull.orEmpty())
    TipoPergunta.NUMERO -> EntradaResposta.Texto((gravada as? JsonPrimitive)?.doubleOrNull?.let { textoDecimal(it) }.orEmpty())
    TipoPergunta.LISTA_POR_REFEICAO -> EntradaResposta.Itens(gravada?.let { runCatching { JsonContrato.decodeFromJsonElement<List<ItemRefeicao>>(it) }.getOrNull() }.orEmpty())
    else -> EntradaResposta.Valor(gravada)
}

enum class ErroResposta { OBRIGATORIA, INVALIDA }

/** Respostas gravadas + digitação válida em andamento (para decidir quais perguntas condicionais aparecem). */
fun respostasEfetivas(gravadas: Map<Uuid, JsonElement>, entradas: Map<Uuid, EntradaResposta>, perguntas: List<PerguntaQuestionario>): Map<Uuid, JsonElement> {
    val resultado = gravadas.toMutableMap()
    for (p in perguntas) {
        val entrada = entradas[p.chave] ?: continue
        val c = converter(p, entrada)
        if (!c.valida) continue
        if (c.valor == null) resultado.remove(p.chave) else resultado[p.chave] = c.valor
    }
    return resultado
}

/** Entradas -> o que gravar. Inválidas ficam de fora e voltam em `erros`. */
data class ResultadoEntradas(val gravar: Map<Uuid, JsonElement?>, val erros: Map<Uuid, ErroResposta>)

fun consolidar(perguntas: List<PerguntaQuestionario>, entradas: Map<Uuid, EntradaResposta>): ResultadoEntradas {
    val gravar = mutableMapOf<Uuid, JsonElement?>()
    val erros = mutableMapOf<Uuid, ErroResposta>()
    for (p in perguntas) {
        val entrada = entradas[p.chave] ?: continue
        val c = converter(p, entrada)
        if (c.valida) gravar[p.chave] = c.valor else erros[p.chave] = ErroResposta.INVALIDA
    }
    return ResultadoEntradas(gravar, erros)
}
