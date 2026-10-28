package org.br.dietamais.dominio.pendencias

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

data class CampoDiferente(val nome: String, val minha: String?, val servidor: String?)

/** Campos que a minha edição mudou em relação ao que o servidor tem agora. Só entram os campos enviados na mutação; `id` e `versao` ficam de fora. */
fun diferencas(minha: JsonObject, servidor: JsonObject?): List<CampoDiferente> =
    minha.entries.filter { it.key != "id" && it.key != "versao" }.mapNotNull { (nome, valor) ->
        val deles = servidor?.get(nome)
        if (servidor != null && valor == deles) null else CampoDiferente(nome, textoDe(valor), deles?.let(::textoDe))
    }

private fun textoDe(valor: JsonElement): String? = when (valor) {
    JsonNull -> null
    is JsonPrimitive -> valor.content
    else -> valor.toString()
}
