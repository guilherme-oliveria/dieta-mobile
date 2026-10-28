package org.br.dietamais.data.sync

import kotlinx.serialization.json.*
import org.br.dietamais.data.local.BancoDieta
import org.br.dietamais.data.remoto.JsonContrato
import org.br.dietamais.data.remoto.dto.AlimentoResposta

// Estado conhecido do servidor, separado da projeção otimista. A revisão é a do feed, nunca o If-Match.
private const val BASE_ALIMENTOS = "base-remota/alimentos"

internal fun BancoDieta.guardarBaseAlimento(usuario: String, perfil: String, id: String) {
    if (registroSyncQueries.estadoDe(usuario, perfil, BASE_ALIMENTOS, id).executeAsOneOrNull() != null) return
    val atual = registroSyncQueries.estadoDe(usuario, perfil, "alimentos", id).executeAsOneOrNull() ?: return
    registroSyncQueries.salvar(usuario, perfil, BASE_ALIMENTOS, id, atual.revisao, atual.excluido, atual.dados_json)
}

internal fun BancoDieta.escolherAlimentoServidor(usuario: String, perfil: String, id: String, atual: JsonObject?) {
    val base = registroSyncQueries.estadoDe(usuario, perfil, BASE_ALIMENTOS, id).executeAsOneOrNull()
    val conhecida = base?.dados_json?.let { JsonContrato.parseToJsonElement(it).jsonObject["versao"]?.jsonPrimitive?.longOrNull }
    val escolhida = atual?.get("versao")?.jsonPrimitive?.longOrNull
    if (base?.excluido == 1L || (conhecida != null && escolhida != null && conhecida > escolhida)) return
    registroSyncQueries.salvar(usuario, perfil, BASE_ALIMENTOS, id, base?.revisao ?: 0, if (atual == null) 1 else 0, atual?.toString())
}

internal fun BancoDieta.receberAlimento(usuario: String, perfil: String, id: String, revisao: Long, json: String?) {
    val base = registroSyncQueries.estadoDe(usuario, perfil, BASE_ALIMENTOS, id).executeAsOneOrNull()
    val anterior = base?.revisao ?: registroSyncQueries.revisao(usuario, perfil, "alimentos", id).executeAsOneOrNull()
    if (anterior != null && (revisao < anterior || (base != null && revisao == anterior))) return
    registroSyncQueries.salvar(usuario, perfil, BASE_ALIMENTOS, id, revisao, if (json == null) 1 else 0, json)
    reconciliarAlimento(usuario, perfil, id)
}

/** GET não fornece revisão de projeção: conserva a já conhecida e compara somente versões da entidade. */
internal fun BancoDieta.receberAlimentoDoCatalogo(usuario: String, perfil: String, alimento: AlimentoResposta) {
    val id = alimento.id.toString()
    val base = registroSyncQueries.estadoDe(usuario, perfil, BASE_ALIMENTOS, id).executeAsOneOrNull()
    val versao = base?.dados_json?.let { JsonContrato.decodeFromString<AlimentoResposta>(it).versao }
    if (base?.excluido == 1L || (versao != null && versao > alimento.versao)) {
        reconciliarAlimento(usuario, perfil, id)
        return
    }
    registroSyncQueries.salvar(usuario, perfil, BASE_ALIMENTOS, id, base?.revisao ?: 0, 0, JsonContrato.encodeToString(alimento))
    reconciliarAlimento(usuario, perfil, id)
}

internal fun BancoDieta.confirmarAlimento(usuario: String, perfil: String, mutacao: MutacaoPendente, versao: Long?) {
    val id = mutacao.id.toString()
    val base = registroSyncQueries.estadoDe(usuario, perfil, BASE_ALIMENTOS, id).executeAsOneOrNull()
    val anterior = base?.dados_json ?: registroSyncQueries.dadosDe(usuario, perfil, ANTERIOR + "alimentos", id).executeAsOneOrNull()?.dados_json
        ?: registroSyncQueries.dadosDe(usuario, perfil, "alimentos", id).executeAsOneOrNull()?.dados_json
    if (base?.excluido == 1L || anterior == null) return
    val objeto = JsonContrato.parseToJsonElement(anterior).jsonObject
    val confirmada = versao ?: (mutacao.versaoBase?.plus(1) ?: 0)
    if (base != null && (objeto["versao"]?.jsonPrimitive?.longOrNull ?: 0) > confirmada) return
    // CRIAR não envia ativo: o contrato cria ativo=true, mesmo que outra edição local já o tenha desativado.
    val dados = if (mutacao.operacao == OperacaoMutacao.CRIAR) JsonObject(mutacao.dados + ("ativo" to JsonPrimitive(true))) else mutacao.dados
    val novo = sobreporAlimento(objeto, dados, confirmada)
    registroSyncQueries.salvar(usuario, perfil, BASE_ALIMENTOS, id, base?.revisao ?: 0, 0, novo.toString())
}

/** Deve rodar dentro da mesma transação que altera a fila ou recebe a página. */
internal fun BancoDieta.reconciliarAlimento(usuario: String, perfil: String, id: String) {
    val base = registroSyncQueries.estadoDe(usuario, perfil, BASE_ALIMENTOS, id).executeAsOneOrNull()
    val atual = registroSyncQueries.estadoDe(usuario, perfil, "alimentos", id).executeAsOneOrNull()
    val abertas = mutacaoPendenteQueries.doRegistro(usuario, perfil, "alimentos", id).executeAsList()
        .filter { it.status == "PENDENTE" || it.status == "CONFLITO" }
    val anterior = registroSyncQueries.dadosDe(usuario, perfil, ANTERIOR + "alimentos", id).executeAsOneOrNull()?.dados_json
    var json = base?.dados_json ?: if (base == null) anterior ?: atual?.dados_json?.takeIf { abertas.isNotEmpty() } else null
    if (base?.excluido != 1L && abertas.isNotEmpty() && json != null) {
        var objeto = JsonContrato.parseToJsonElement(json).jsonObject
        abertas.forEach { m ->
            objeto = sobreporAlimento(objeto, JsonContrato.parseToJsonElement(m.dados_json).jsonObject, m.versao_base?.plus(1) ?: 0)
        }
        // A data otimista pertence à edição local; o servidor só a substitui ao encerrar as pendências.
        atual?.dados_json?.let { JsonContrato.parseToJsonElement(it).jsonObject["atualizadoEm"] }?.let {
            objeto = JsonObject(objeto + ("atualizadoEm" to it))
        }
        json = objeto.toString()
    }
    registroSyncQueries.salvar(usuario, perfil, "alimentos", id, base?.revisao ?: 0, if (json == null) 1 else 0, json)
    projetar(usuario, perfil, "alimentos", id)
}

private fun sobreporAlimento(base: JsonObject, dados: JsonObject, versao: Long): JsonObject {
    val campos = setOf("grupoId", "nome", "medidaCaseira", "gramasPorPorcao", "ativo")
    return JsonObject(base + dados.filterKeys { it in campos } + ("versao" to JsonPrimitive(versao)))
}
