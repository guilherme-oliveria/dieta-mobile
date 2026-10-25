package org.br.dietamais.data.sync

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.br.dietamais.data.local.BancoDieta
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

enum class OperacaoMutacao { CRIAR, ATUALIZAR, EXCLUIR }

data class NovaMutacao(
    val entidade: String,
    val operacao: OperacaoMutacao,
    val id: Uuid,
    val dados: JsonObject,
    val versaoBase: Long? = null,
)

data class MutacaoPendente(
    val idMutacao: Uuid,
    val entidade: String,
    val operacao: OperacaoMutacao,
    val id: Uuid,
    val versaoBase: Long?,
    val dados: JsonObject,
    val tentativas: Long,
)

@OptIn(ExperimentalUuidApi::class)
fun novoIdLocal(): Uuid = Uuid.generateV7()

class FilaMutacoes(private val banco: BancoDieta, private val novoId: () -> Uuid = ::novoIdLocal) {
    /** RN-SYN-01: a escrita e o registro durável da mutação usam o mesmo driver/transação. */
    fun gravarEEnfileirar(mutacao: NovaMutacao, gravar: BancoDieta.() -> Unit): Uuid = banco.transactionWithResult {
        val sessao = checkNotNull(banco.sessaoQueries.atual().executeAsOneOrNull()) { "Nenhuma sessão local ativa." }
        val idMutacao = novoId()
        banco.gravar()
        banco.mutacaoPendenteQueries.enfileirar(
            id_mutacao = idMutacao.toString(), usuario_id = sessao.usuario_id, perfil = sessao.perfil,
            entidade = mutacao.entidade, operacao = mutacao.operacao.name, id = mutacao.id.toString(),
            versao_base = mutacao.versaoBase, dados_json = mutacao.dados.toString(),
        )
        idMutacao
    }

    fun pendentes(): List<MutacaoPendente> = banco.transactionWithResult {
        val sessao = banco.sessaoQueries.atual().executeAsOneOrNull() ?: return@transactionWithResult emptyList()
        banco.mutacaoPendenteQueries.pendentes(sessao.usuario_id, sessao.perfil).executeAsList().map {
            MutacaoPendente(
                idMutacao = Uuid.parse(it.id_mutacao), entidade = it.entidade,
                operacao = OperacaoMutacao.valueOf(it.operacao), id = Uuid.parse(it.id), versaoBase = it.versao_base,
                dados = Json.parseToJsonElement(it.dados_json).jsonObject, tentativas = it.tentativas,
            )
        }
    }
}
