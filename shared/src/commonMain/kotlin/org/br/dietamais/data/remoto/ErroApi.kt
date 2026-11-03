package org.br.dietamais.data.remoto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ErroCampoResposta(val campo: String, val codigo: String, val mensagem: String)

@Serializable
data class ProblemaResposta(val type: String, val title: String, val status: Int, val detail: String,
    val codigo: String, val regra: String? = null, val campos: List<ErroCampoResposta>,
    val traceId: String? = null, val atual: JsonElement? = null)

class ErroApi(val codigo: String, val regra: String?, val campos: List<ErroCampoResposta>,
    val status: Int, val traceId: String?, val atual: JsonElement?) : Exception("API: $codigo ($status)")

class FalhaComunicacao : Exception("Não foi possível comunicar com a API.")
class RespostaInvalida : Exception("A API retornou uma resposta incompatível.")
class SessaoAlterada : Exception("A sessão foi alterada durante a operação.")
