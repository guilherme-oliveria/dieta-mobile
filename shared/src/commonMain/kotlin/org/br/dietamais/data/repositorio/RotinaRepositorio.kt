package org.br.dietamais.data.repositorio

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import org.br.dietamais.data.local.BancoDieta
import org.br.dietamais.data.remoto.JsonContrato
import org.br.dietamais.data.remoto.RespostaInvalida
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.sync.FilaMutacoes
import org.br.dietamais.data.sync.NovaMutacao
import org.br.dietamais.data.sync.OperacaoMutacao
import org.br.dietamais.data.sync.novoIdLocal
import kotlin.time.Instant
import kotlin.uuid.Uuid

data class DadosRotina(val checkins: List<CheckinResposta> = emptyList(), val agua: List<AguaResposta> = emptyList(), val pesos: List<PesoResposta> = emptyList())

interface PortaRotina {
    fun observar(): Flow<DadosRotina>
    /** Emite quando o servidor recusa um registro já mostrado (só na web, que grava pela API); o app local nunca emite. */
    val falhas: Flow<Unit> get() = kotlinx.coroutines.flow.emptyFlow()
    /** Offline-first: grava o registro local e a mutação na mesma transação (RN-SYN-01). */
    fun registrarCheckin(dietaId: Uuid, refeicaoId: Uuid, data: LocalDate, status: StatusCheckin, trocas: List<TrocaCheckin>, agora: Instant)
    fun registrarAgua(ml: Int, agora: Instant)
    fun registrarPeso(pesoKg: Double, agora: Instant)
}

class RotinaRepositorio(private val banco: BancoDieta, private val fila: FilaMutacoes = FilaMutacoes(banco)) : PortaRotina {
    override fun observar(): Flow<DadosRotina> = combine(
        banco.registroSyncQueries.daSessao("checkins").asFlow().mapToList(Dispatchers.Default),
        banco.registroSyncQueries.daSessao("agua").asFlow().mapToList(Dispatchers.Default),
        banco.registroSyncQueries.daSessao("pesos").asFlow().mapToList(Dispatchers.Default)) { checkins, agua, pesos ->
        try { DadosRotina(checkins.map { JsonContrato.decodeFromString<CheckinResposta>(it) }, agua.map { JsonContrato.decodeFromString<AguaResposta>(it) },
            pesos.map { JsonContrato.decodeFromString<PesoResposta>(it) }) }
        catch (_: Exception) { throw RespostaInvalida() }
    }

    /** O servidor recusou depois um registro já mostrado (a criação otimista some): avisa a tela, que de outro modo ficaria muda. Recusas antigas não contam. */
    override val falhas: Flow<Unit> = flow {
        var vistas: Set<String>? = null
        banco.mutacaoPendenteQueries.daSessaoAtual().asFlow().mapToList(Dispatchers.Default).collect { lista ->
            val agora = lista.filter { it.status == "REJEITADA" && it.entidade in setOf("checkins", "agua", "pesos") }.map { it.id_mutacao }.toSet()
            val antes = vistas
            vistas = agora
            if (antes != null && (agora - antes).isNotEmpty()) emit(Unit)
        }
    }

    override fun registrarCheckin(dietaId: Uuid, refeicaoId: Uuid, data: LocalDate, status: StatusCheckin, trocas: List<TrocaCheckin>, agora: Instant) {
        require((status == StatusCheckin.TROCADO) == trocas.isNotEmpty() && status != StatusCheckin.OUTRO)
        val id = novoIdLocal()
        gravar("checkins", id, JsonContrato.encodeToJsonElement(CheckinRequisicao(id, dietaId, refeicaoId, data, status, trocas, agora)).jsonObject) { paciente ->
            JsonContrato.encodeToString(CheckinResposta(id, paciente, dietaId, refeicaoId, data, status, trocas, agora))
        }
    }

    override fun registrarAgua(ml: Int, agora: Instant) {
        require(ml in 1..5000)
        val id = novoIdLocal()
        gravar("agua", id, JsonContrato.encodeToJsonElement(AguaRequisicao(id, ml, agora)).jsonObject) { JsonContrato.encodeToString(AguaResposta(id, it, ml, agora)) }
    }

    override fun registrarPeso(pesoKg: Double, agora: Instant) {
        require(pesoKg in 20.0..400.0)
        val id = novoIdLocal()
        gravar("pesos", id, JsonContrato.encodeToJsonElement(PesoRequisicao(id, pesoKg, agora)).jsonObject) { JsonContrato.encodeToString(PesoResposta(id, it, pesoKg, agora)) }
    }

    /** O registro local entra com revisão 0; quando o servidor devolver a versão 1 pelo pull ele a substitui. */
    private fun gravar(colecao: String, id: Uuid, dados: JsonObject, local: (Uuid) -> String) {
        fila.gravarEEnfileirar(NovaMutacao(colecao, OperacaoMutacao.CRIAR, id, dados)) {
            val sessao = checkNotNull(sessaoQueries.atual().executeAsOneOrNull())
            registroSyncQueries.salvar(sessao.usuario_id, sessao.perfil, colecao, id.toString(), 0, 0, local(Uuid.parse(sessao.usuario_id)))
        }
    }
}
