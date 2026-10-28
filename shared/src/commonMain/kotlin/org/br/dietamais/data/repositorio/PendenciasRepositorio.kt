package org.br.dietamais.data.repositorio

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.json.*
import org.br.dietamais.data.local.BancoDieta
import org.br.dietamais.data.sync.ANTERIOR
import org.br.dietamais.data.sync.OperacaoMutacao
import org.br.dietamais.data.sync.restaurarAnterior
import kotlin.uuid.Uuid

enum class EstadoPendencia { AGUARDANDO, CONFLITO, REJEITADA }

data class Pendencia(val idMutacao: Uuid, val entidade: String, val operacao: OperacaoMutacao, val id: Uuid, val estado: EstadoPendencia,
    val minha: JsonObject, val servidor: JsonObject?)

interface PortaPendencias {
    /** Mutações da sessão que ainda não foram aplicadas (aguardando, em conflito ou rejeitadas), da mais antiga para a mais nova. */
    fun observar(): Flow<List<Pendencia>>
    /** Reenvia a minha versão sobre a do servidor (só em conflito). */
    fun manterMinha(idMutacao: Uuid)
    /** Aceita a versão do servidor e descarta a minha edição. */
    fun usarServidor(idMutacao: Uuid)
    /** Desfaz a edição local (volta ao estado de antes) e tira da fila. */
    fun descartar(idMutacao: Uuid)
}

/** Resolução de conflitos e rejeições (MOB-30). Funciona só no banco local; quem chama pede o sync depois (`aoMudar`). */
class PendenciasRepositorio(private val banco: BancoDieta, private val aoMudar: () -> Unit = {}) : PortaPendencias {
    override fun observar(): Flow<List<Pendencia>> = combine(
        banco.mutacaoPendenteQueries.daSessaoAtual().asFlow().mapToList(Dispatchers.Default),
        banco.conflitoQueries.daSessaoAtual().asFlow().mapToList(Dispatchers.Default),
    ) { mutacoes, conflitos ->
        val atuais = conflitos.associate { it.id_mutacao to objetoOuNulo(it.atual_json) }
        mutacoes.mapNotNull { m ->
            val minha = objetoOuNulo(m.dados_json) ?: JsonObject(emptyMap())
            Pendencia(Uuid.parse(m.id_mutacao), m.entidade, OperacaoMutacao.valueOf(m.operacao), Uuid.parse(m.id),
                when (m.status) { "PENDENTE" -> EstadoPendencia.AGUARDANDO; "CONFLITO" -> EstadoPendencia.CONFLITO; else -> EstadoPendencia.REJEITADA },
                minha, atuais[m.id_mutacao])
        }
    }

    override fun manterMinha(idMutacao: Uuid) {
        val mudou = banco.transactionWithResult {
            val sessao = banco.sessaoQueries.atual().executeAsOneOrNull() ?: return@transactionWithResult false
            val m = banco.mutacaoPendenteQueries.buscar(idMutacao.toString(), sessao.usuario_id, sessao.perfil).executeAsOneOrNull()
            if (m == null || m.status != "CONFLITO") return@transactionWithResult false
            val atual = banco.conflitoQueries.buscar(idMutacao.toString(), sessao.usuario_id, sessao.perfil).executeAsOneOrNull()?.let(::objetoOuNulo)
            val versaoAtual = atual?.get("versao")?.jsonPrimitive?.longOrNull ?: return@transactionWithResult false
            banco.mutacaoPendenteQueries.reabrir(versaoAtual, idMutacao.toString(), sessao.usuario_id, sessao.perfil)
            banco.conflitoQueries.remover(idMutacao.toString(), sessao.usuario_id, sessao.perfil)
            // a minha versão otimista passa a ser a seguinte à do servidor, que é a base de uma nova edição offline
            banco.registroSyncQueries.dadosDe(sessao.usuario_id, sessao.perfil, m.entidade, m.id).executeAsOneOrNull()?.let { linha ->
                linha.dados_json?.let(::objetoOuNulo)?.takeIf { "versao" in it }?.let { local ->
                    val novo = JsonObject(local + ("versao" to JsonPrimitive(versaoAtual + 1)))
                    banco.registroSyncQueries.salvar(sessao.usuario_id, sessao.perfil, m.entidade, m.id, 0, 0, novo.toString())
                }
            }
            true
        }
        if (mudou) aoMudar()
    }

    override fun usarServidor(idMutacao: Uuid) = encerrar(idMutacao) { (usuario, perfil), m, atual ->
        if (atual != null) banco.registroSyncQueries.salvar(usuario, perfil, m.entidade, m.id, 0, 0, atual.toString())
        else banco.registroSyncQueries.remover(usuario, perfil, m.entidade, m.id)
    }

    override fun descartar(idMutacao: Uuid) = encerrar(idMutacao) { (usuario, perfil), m, _ ->
        if (m.operacao == OperacaoMutacao.CRIAR.name) banco.registroSyncQueries.remover(usuario, perfil, m.entidade, m.id)
        else banco.restaurarAnterior(usuario, perfil, m.entidade, m.id)
    }

    /** Aplica `efeito` ao registro e tira da fila todas as mutações dele (com seus conflitos e o instantâneo anterior). */
    private fun encerrar(idMutacao: Uuid, efeito: (Pair<String, String>, org.br.dietamais.data.local.Mutacao_pendente, JsonObject?) -> Unit) {
        val mudou = banco.transactionWithResult {
            val sessao = banco.sessaoQueries.atual().executeAsOneOrNull() ?: return@transactionWithResult false
            val m = banco.mutacaoPendenteQueries.buscar(idMutacao.toString(), sessao.usuario_id, sessao.perfil).executeAsOneOrNull() ?: return@transactionWithResult false
            val atual = banco.conflitoQueries.buscar(idMutacao.toString(), sessao.usuario_id, sessao.perfil).executeAsOneOrNull()?.let(::objetoOuNulo)
            efeito(sessao.usuario_id to sessao.perfil, m, atual)
            banco.mutacaoPendenteQueries.doRegistro(sessao.usuario_id, sessao.perfil, m.entidade, m.id).executeAsList()
                .forEach { banco.conflitoQueries.remover(it.id_mutacao, sessao.usuario_id, sessao.perfil) }
            banco.mutacaoPendenteQueries.removerDoRegistro(sessao.usuario_id, sessao.perfil, m.entidade, m.id)
            banco.registroSyncQueries.remover(sessao.usuario_id, sessao.perfil, ANTERIOR + m.entidade, m.id)
            true
        }
        if (mudou) aoMudar()
    }

    private fun objetoOuNulo(texto: String): JsonObject? = try { Json.parseToJsonElement(texto) as? JsonObject } catch (_: Exception) { null }
}
