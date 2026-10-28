package org.br.dietamais.dominio.anamnese

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.br.dietamais.data.remoto.JsonContrato
import org.br.dietamais.data.remoto.dto.*
import kotlin.uuid.Uuid

/** Id da pergunta; as respostas da API sempre o trazem (só as requisições de criação podem omiti-lo). */
val PerguntaQuestionario.chave: Uuid get() = checkNotNull(id) { "Pergunta sem id." }

@Serializable
data class ItemRefeicao(val refeicao: String, val descricao: String, val horario: String? = null)

/** Mesma regra do backend (RN-ANA-02): a pergunta condicional só aparece se a de referência satisfaz o operador; sem resposta, IGUAL/CONTEM escondem e DIFERENTE mostra. */
fun visivel(pergunta: PerguntaQuestionario, respostas: Map<Uuid, JsonElement>): Boolean {
    val c = pergunta.condicao ?: return true
    val r = respostas[c.perguntaId]
    val igual = when {
        r is JsonPrimitive && r.isString -> r.content == c.valor
        r is JsonArray -> r.size == 1 && (r[0] as? JsonPrimitive)?.contentOrNull == c.valor
        else -> false
    }
    val contem = r is JsonArray && r.any { (it as? JsonPrimitive)?.contentOrNull == c.valor }
    return when (c.operador) {
        OperadorCondicao.IGUAL -> igual
        OperadorCondicao.DIFERENTE -> !igual
        OperadorCondicao.CONTEM -> contem
        OperadorCondicao.OUTRO -> true // operador que este app não conhece: mostra a pergunta e deixa o backend decidir
    }
}

/** Um passo do assistente = um bloco com ao menos uma pergunta visível. */
data class PassoAnamnese(val titulo: String, val perguntas: List<PerguntaQuestionario>)

fun passos(blocos: List<BlocoQuestionario>, respostas: Map<Uuid, JsonElement>): List<PassoAnamnese> =
    blocos.map { b -> PassoAnamnese(b.titulo, b.perguntas.filter { it.tipo != TipoPergunta.OUTRO && visivel(it, respostas) }) }.filter { it.perguntas.isNotEmpty() }

/** Perguntas obrigatórias e visíveis ainda sem resposta (o backend confere de novo ao enviar). */
fun faltando(blocos: List<BlocoQuestionario>, respostas: Map<Uuid, JsonElement>): List<Uuid> =
    blocos.flatMap { it.perguntas }.filter { it.obrigatoria && it.tipo != TipoPergunta.OUTRO && visivel(it, respostas) && respostas[it.chave] == null }.map { it.chave }

private val HORA = Regex("([01][0-9]|2[0-3]):[0-5][0-9]")

/** Feedback imediato; a validação que vale é a do backend. */
fun respostaValida(pergunta: PerguntaQuestionario, valor: JsonElement): Boolean = when (pergunta.tipo) {
    TipoPergunta.TEXTO -> valor is JsonPrimitive && valor.isString && valor.content.isNotBlank() && valor.content.length <= 2000
    TipoPergunta.NUMERO -> valor is JsonPrimitive && !valor.isString && valor.doubleOrNull?.isFinite() == true
    TipoPergunta.ESCOLHA_UNICA -> valor is JsonPrimitive && valor.isString && pergunta.opcoes.orEmpty().contains(valor.content)
    TipoPergunta.MULTIPLA_ESCOLHA -> valor is JsonArray && valor.isNotEmpty() && valor.all { it is JsonPrimitive && it.isString && pergunta.opcoes.orEmpty().contains(it.content) } &&
        valor.map { (it as JsonPrimitive).content }.toSet().size == valor.size
    TipoPergunta.ESCALA_1_5 -> valor is JsonPrimitive && !valor.isString && valor.intOrNull in 1..5
    TipoPergunta.HORARIO -> valor is JsonPrimitive && valor.isString && HORA.matches(valor.content)
    TipoPergunta.LISTA_POR_REFEICAO -> valor is JsonArray && valor.isNotEmpty() && valor.size <= 12 && valor.all { item ->
        runCatching { JsonContrato.decodeFromJsonElement<ItemRefeicao>(item) }.getOrNull()?.let {
            it.refeicao.isNotBlank() && it.refeicao.length <= 60 && it.descricao.isNotBlank() && it.descricao.length <= 500 && (it.horario == null || HORA.matches(it.horario))
        } == true
    }
    TipoPergunta.OUTRO -> false
}

fun jsonTexto(texto: String): JsonElement = JsonPrimitive(texto.trim())
fun jsonNumero(texto: String): JsonElement? = texto.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }?.let { JsonPrimitive(it) }
fun jsonLista(itens: List<String>): JsonElement = JsonArray(itens.map { JsonPrimitive(it) })
fun jsonItens(itens: List<ItemRefeicao>): JsonElement = JsonContrato.encodeToJsonElement(itens)

/** Texto para leitura (a nutri vê as respostas do paciente na ficha). */
fun textoDaResposta(pergunta: PerguntaQuestionario, valor: JsonElement?): String? {
    if (valor == null) return null
    return when (pergunta.tipo) {
        TipoPergunta.MULTIPLA_ESCOLHA -> (valor as? JsonArray)?.joinToString(", ") { (it as? JsonPrimitive)?.contentOrNull.orEmpty() }
        TipoPergunta.LISTA_POR_REFEICAO -> runCatching { JsonContrato.decodeFromJsonElement<List<ItemRefeicao>>(valor) }.getOrNull()
            ?.joinToString("\n") { item -> listOfNotNull(item.horario, item.refeicao).joinToString(" ") + ": " + item.descricao }
        TipoPergunta.NUMERO -> (valor as? JsonPrimitive)?.doubleOrNull?.let { org.br.dietamais.dominio.avaliacao.textoDecimal(it) }
        else -> (valor as? JsonPrimitive)?.contentOrNull
    }
}

/** Quantas perguntas visíveis já têm resposta, para a barra de progresso do assistente. */
fun progresso(blocos: List<BlocoQuestionario>, respostas: Map<Uuid, JsonElement>): Pair<Int, Int> {
    val visiveis = passos(blocos, respostas).flatMap { it.perguntas }
    return visiveis.count { respostas[it.chave] != null } to visiveis.size
}
