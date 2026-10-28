package org.br.dietamais.data.sync

import kotlinx.serialization.json.JsonObject
import org.br.dietamais.data.local.BancoDieta
import org.br.dietamais.data.remoto.JsonContrato
import org.br.dietamais.data.remoto.dto.PacienteResposta
import kotlin.uuid.Uuid

/** Prefixo da coleção que guarda o registro como estava antes da primeira edição offline (para desfazer uma rejeição). */
internal const val ANTERIOR = "anterior/"

/**
 * Escrita offline-first da nutri (MOB-29): grava o registro otimista em `registro_sync` e a mutação na fila, na mesma transação
 * (RN-SYN-01). O registro entra com revisão 0, então o pull do servidor sempre o substitui depois que a mutação for aplicada.
 * A versão local é `versaoBase + 1`: uma segunda edição offline do mesmo registro parte dela, que é a que o servidor terá depois da primeira.
 */
class EscritaOffline(private val banco: BancoDieta, private val fila: FilaMutacoes = FilaMutacoes(banco), private val aoEnfileirar: () -> Unit = {}) {

    fun criar(colecao: String, id: Uuid, dados: JsonObject, local: String) {
        fila.gravarEEnfileirar(NovaMutacao(colecao, OperacaoMutacao.CRIAR, id, dados)) { salvarLocal(colecao, id, local, guardarAnterior = false) }
        aoEnfileirar()
    }

    fun atualizar(colecao: String, id: Uuid, versaoBase: Long, dados: JsonObject, local: String) {
        fila.gravarEEnfileirar(NovaMutacao(colecao, OperacaoMutacao.ATUALIZAR, id, dados, versaoBase)) { salvarLocal(colecao, id, local, guardarAnterior = true) }
        aoEnfileirar()
    }

    private fun BancoDieta.salvarLocal(colecao: String, id: Uuid, local: String, guardarAnterior: Boolean) {
        val sessao = checkNotNull(sessaoQueries.atual().executeAsOneOrNull())
        if (guardarAnterior) {
            val jaTem = mutacaoPendenteQueries.doRegistro(sessao.usuario_id, sessao.perfil, colecao, id.toString()).executeAsList().isNotEmpty()
            val atual = registroSyncQueries.dadosDe(sessao.usuario_id, sessao.perfil, colecao, id.toString()).executeAsOneOrNull()
            if (!jaTem && atual != null) registroSyncQueries.salvar(sessao.usuario_id, sessao.perfil, ANTERIOR + colecao, id.toString(), 0, 0, atual.dados_json)
        }
        registroSyncQueries.salvar(sessao.usuario_id, sessao.perfil, colecao, id.toString(), 0, 0, local)
        projetar(sessao.usuario_id, sessao.perfil, colecao, id.toString())
    }
}

/** Apaga o instantâneo anterior quando não resta nenhuma mutação do registro. */
internal fun BancoDieta.limparAnteriorSeLivre(usuario: String, perfil: String, colecao: String, id: String) {
    if (mutacaoPendenteQueries.doRegistro(usuario, perfil, colecao, id).executeAsList().isEmpty())
        registroSyncQueries.remover(usuario, perfil, ANTERIOR + colecao, id)
}

/** Devolve o registro ao estado que tinha antes da primeira edição offline (se houver instantâneo). */
internal fun BancoDieta.restaurarAnterior(usuario: String, perfil: String, colecao: String, id: String) {
    val antes = registroSyncQueries.dadosDe(usuario, perfil, ANTERIOR + colecao, id).executeAsOneOrNull() ?: return
    registroSyncQueries.salvar(usuario, perfil, colecao, id, 0, 0, antes.dados_json)
    projetar(usuario, perfil, colecao, id)
}

/**
 * Pacientes e alimentos também têm tabelas de leitura próprias (`paciente_cache`, `alimento_cache`). Depois de mexer no registro local,
 * a projeção as deixa iguais a ele: o que a tela lê reflete a edição otimista, a rejeição ou a escolha do servidor.
 */
internal fun BancoDieta.projetar(usuario: String, perfil: String, colecao: String, id: String) {
    val json = registroSyncQueries.dadosDe(usuario, perfil, colecao, id).executeAsOneOrNull()?.dados_json
    when (colecao) {
        "pacientes" -> {
            pacienteCacheQueries.limparPaciente(usuario, perfil, id)
            if (json != null) pacienteCacheQueries.salvar(usuario, perfil, JsonContrato.decodeFromString<PacienteResposta>(json).vinculoId.toString(), id, json)
        }
        "alimentos" -> if (json != null) alimentoCacheQueries.salvar(usuario, perfil, "alimentos", id, json) else alimentoCacheQueries.remover(usuario, perfil, "alimentos", id)
    }
}

/** Depois de recarregar um cache pela API, repõe as edições locais que o servidor ainda não conhece. */
internal fun BancoDieta.reprojetarAbertas(usuario: String, perfil: String, colecao: String) {
    mutacaoPendenteQueries.doEscopo(usuario, perfil).executeAsList()
        .filter { it.entidade == colecao && it.status in listOf("PENDENTE", "CONFLITO") }.map { it.id }.distinct()
        .forEach { projetar(usuario, perfil, colecao, it) }
}
