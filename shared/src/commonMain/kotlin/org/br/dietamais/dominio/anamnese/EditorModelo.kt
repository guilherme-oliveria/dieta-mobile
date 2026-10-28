package org.br.dietamais.dominio.anamnese

import org.br.dietamais.data.remoto.dto.*
import kotlin.uuid.Uuid

enum class ErroModelo { NOME_VAZIO, SEM_PERGUNTAS, BLOCO_SEM_TITULO, PERGUNTA_SEM_TEXTO, OPCOES_INSUFICIENTES, OPCOES_REPETIDAS, CONDICAO_INCOMPLETA, CONDICAO_INVALIDA }

/** `opcoes` é o texto do campo, uma opção por linha. Sem `condicaoPergunta` a pergunta aparece sempre. */
data class PerguntaEditavel(val id: Uuid, val texto: String = "", val tipo: TipoPergunta = TipoPergunta.TEXTO, val opcoes: String = "", val obrigatoria: Boolean = false,
    val condicaoPergunta: Uuid? = null, val condicaoOperador: OperadorCondicao = OperadorCondicao.IGUAL, val condicaoValor: String = "")

data class BlocoEditavel(val titulo: String = "", val perguntas: List<PerguntaEditavel> = emptyList())

/** `id`/`versao` nulos = modelo novo; `chave` é o UUIDv7 reservado para a criação (idempotência). */
data class EditorModelo(val chave: Uuid, val id: Uuid? = null, val versao: Long? = null, val nome: String = "", val blocos: List<BlocoEditavel> = emptyList())

data class ValidacaoModelo(val nome: ErroModelo? = null, val blocos: Map<Int, ErroModelo> = emptyMap(), val perguntas: Map<Uuid, ErroModelo> = emptyMap()) {
    val valido get() = nome == null && blocos.isEmpty() && perguntas.isEmpty()
}

fun tipoTemOpcoes(tipo: TipoPergunta) = tipo == TipoPergunta.ESCOLHA_UNICA || tipo == TipoPergunta.MULTIPLA_ESCOLHA

fun opcoesDe(pergunta: PerguntaEditavel): List<String> = if (tipoTemOpcoes(pergunta.tipo)) pergunta.opcoes.lines().map { it.trim() }.filter { it.isNotEmpty() } else emptyList()

/** Tipos que o app sabe editar; `OUTRO` é só o fallback de um tipo futuro. */
val tiposEditaveis: List<TipoPergunta> = TipoPergunta.entries.filter { it != TipoPergunta.OUTRO }

fun editorNovo(chave: Uuid) = EditorModelo(chave, blocos = listOf(BlocoEditavel()))

fun editorDe(modelo: ModeloQuestionarioResposta): EditorModelo = EditorModelo(modelo.id, modelo.id, modelo.versao, modelo.nome, modelo.blocos.map { b ->
    BlocoEditavel(b.titulo, b.perguntas.map { p ->
        PerguntaEditavel(p.chave, p.texto, p.tipo, p.opcoes.orEmpty().joinToString("\n"), p.obrigatoria, p.condicao?.perguntaId, p.condicao?.operador ?: OperadorCondicao.IGUAL, p.condicao?.valor.orEmpty())
    })
})

fun EditorModelo.mudarNome(nome: String) = copy(nome = nome)
fun EditorModelo.adicionarBloco() = copy(blocos = blocos + BlocoEditavel())
fun EditorModelo.removerBloco(indice: Int) = copy(blocos = blocos.filterIndexed { i, _ -> i != indice }).limparCondicoes()
fun EditorModelo.mudarBloco(indice: Int, titulo: String) = copy(blocos = blocos.mapIndexed { i, b -> if (i == indice) b.copy(titulo = titulo) else b })
fun EditorModelo.adicionarPergunta(bloco: Int, id: Uuid) = copy(blocos = blocos.mapIndexed { i, b -> if (i == bloco) b.copy(perguntas = b.perguntas + PerguntaEditavel(id)) else b })
fun EditorModelo.removerPergunta(id: Uuid) = copy(blocos = blocos.map { b -> b.copy(perguntas = b.perguntas.filter { it.id != id }) }).limparCondicoes()
fun EditorModelo.mudarPergunta(id: Uuid, mudar: (PerguntaEditavel) -> PerguntaEditavel) =
    copy(blocos = blocos.map { b -> b.copy(perguntas = b.perguntas.map { if (it.id == id) mudar(it) else it }) }).limparCondicoes()

/** Sobe/desce a pergunta dentro do bloco (a ordem importa: a condição só aponta para perguntas anteriores). */
fun EditorModelo.moverPergunta(id: Uuid, delta: Int): EditorModelo = copy(blocos = blocos.map { b ->
    val i = b.perguntas.indexOfFirst { it.id == id }
    val j = i + delta
    if (i < 0 || j !in b.perguntas.indices) b else b.copy(perguntas = b.perguntas.toMutableList().also { val p = it.removeAt(i); it.add(j, p) })
}).limparCondicoes()

/** Perguntas que podem servir de referência a uma condição: as que vêm antes dela no modelo. */
fun EditorModelo.anteriores(id: Uuid): List<PerguntaEditavel> {
    val todas = blocos.flatMap { it.perguntas }
    val indice = todas.indexOfFirst { it.id == id }
    return if (indice <= 0) emptyList() else todas.subList(0, indice).filter { it.texto.isNotBlank() }
}

/** Mantém o editor sempre coerente: condição que perdeu a referência (removida, movida, valor fora das opções) é desligada. */
private fun EditorModelo.limparCondicoes(): EditorModelo {
    val todas = blocos.flatMap { it.perguntas }
    val ordem = todas.map { it.id }
    return copy(blocos = blocos.map { b -> b.copy(perguntas = b.perguntas.map { p ->
        val ref = p.condicaoPergunta?.let { r -> todas.firstOrNull { it.id == r } }
        val valida = ref != null && ordem.indexOf(ref.id) < ordem.indexOf(p.id)
        when {
            p.condicaoPergunta == null -> p
            !valida -> p.semCondicao()
            else -> p.copy(condicaoOperador = operadoresPara(ref!!).let { if (p.condicaoOperador in it) p.condicaoOperador else it.first() },
                condicaoValor = opcoesDe(ref).let { if (it.isNotEmpty() && p.condicaoValor !in it) "" else p.condicaoValor })
        }
    }) })
}

private fun PerguntaEditavel.semCondicao() = copy(condicaoPergunta = null, condicaoOperador = OperadorCondicao.IGUAL, condicaoValor = "")

/** CONTEM só vale para múltipla escolha (backend, RN-ANA-02). */
fun operadoresPara(referencia: PerguntaEditavel): List<OperadorCondicao> =
    if (referencia.tipo == TipoPergunta.MULTIPLA_ESCOLHA) listOf(OperadorCondicao.IGUAL, OperadorCondicao.DIFERENTE, OperadorCondicao.CONTEM)
    else listOf(OperadorCondicao.IGUAL, OperadorCondicao.DIFERENTE)

fun EditorModelo.ligarCondicao(id: Uuid, referencia: Uuid) = mudarPergunta(id) { p -> p.copy(condicaoPergunta = referencia, condicaoValor = "") }
fun EditorModelo.desligarCondicao(id: Uuid) = mudarPergunta(id) { it.semCondicao() }

fun EditorModelo.validar(): ValidacaoModelo {
    val blocosComErro = mutableMapOf<Int, ErroModelo>()
    val perguntasComErro = mutableMapOf<Uuid, ErroModelo>()
    val todas = blocos.flatMap { it.perguntas }
    blocos.forEachIndexed { i, b -> if (b.titulo.isBlank()) blocosComErro[i] = ErroModelo.BLOCO_SEM_TITULO }
    todas.forEachIndexed { indice, p ->
        val opcoes = opcoesDe(p)
        val ref = p.condicaoPergunta?.let { r -> todas.take(indice).firstOrNull { it.id == r } }
        val erro = when {
            p.texto.isBlank() -> ErroModelo.PERGUNTA_SEM_TEXTO
            tipoTemOpcoes(p.tipo) && opcoes.size < 2 -> ErroModelo.OPCOES_INSUFICIENTES
            opcoes.toSet().size != opcoes.size -> ErroModelo.OPCOES_REPETIDAS
            p.condicaoPergunta != null && ref == null -> ErroModelo.CONDICAO_INVALIDA
            p.condicaoPergunta != null && p.condicaoValor.isBlank() -> ErroModelo.CONDICAO_INCOMPLETA
            ref != null && p.condicaoOperador !in operadoresPara(ref) -> ErroModelo.CONDICAO_INVALIDA
            ref != null && opcoesDe(ref).let { it.isNotEmpty() && p.condicaoValor !in it } -> ErroModelo.CONDICAO_INVALIDA
            else -> null
        }
        if (erro != null) perguntasComErro[p.id] = erro
    }
    val nome = when {
        this.nome.isBlank() -> ErroModelo.NOME_VAZIO
        todas.isEmpty() -> ErroModelo.SEM_PERGUNTAS
        else -> null
    }
    return ValidacaoModelo(nome, blocosComErro, perguntasComErro)
}

fun EditorModelo.paraRequisicao(): SalvarModeloRequisicao = SalvarModeloRequisicao(id ?: chave, nome.trim(), blocos.map { b ->
    BlocoQuestionario(b.titulo.trim(), b.perguntas.map { p ->
        PerguntaQuestionario(p.id, p.texto.trim(), p.tipo, opcoesDe(p).ifEmpty { null }, p.obrigatoria,
            p.condicaoPergunta?.let { CondicaoPergunta(it, p.condicaoOperador, p.condicaoValor.trim()) })
    })
})
