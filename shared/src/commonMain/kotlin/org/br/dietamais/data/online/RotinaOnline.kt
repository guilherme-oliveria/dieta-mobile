package org.br.dietamais.data.online

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.encodeToJsonElement
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.DadosRotina
import org.br.dietamais.data.repositorio.PortaRotina
import org.br.dietamais.data.sync.novoIdLocal
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * Rotina do paciente na web (BE-38): lê os últimos 90 dias de `GET /eu/rotina` e grava por `POST`. O registro aparece na tela na
 * hora (otimista, em memória) e é trocado pelo que o servidor gravou; se o servidor recusar, o registro some e a tela volta ao que era.
 */
class RotinaOnline(private val api: ApiDieta, private val escopo: CoroutineScope, private val fuso: () -> TimeZone = { TimeZone.currentSystemDefault() },
    private val agora: () -> Instant = { Clock.System.now() }) : PortaRotina {
    private class Dados(val dono: Uuid, val rotina: DadosRotina)
    private val guardado = MutableStateFlow<Dados?>(null)

    private val visivel: Flow<DadosRotina> = combine(api.sessao, guardado) { sessao, dados ->
        if (sessao != null && dados?.dono == sessao.usuarioId) dados.rotina else DadosRotina()
    }

    override fun observar(): Flow<DadosRotina> = flow {
        carregar()
        emitAll(visivel)
    }

    /** Releitura do período; usada ao abrir a tela e depois de cada registro. */
    suspend fun carregar() {
        val esperado = contexto()
        val hoje = agora().toLocalDateTime(fuso()).date
        val resposta = api.obter<RotinaResposta>("eu/rotina", mapOf("de" to hoje.minus(DatePeriod(days = DIAS - 1)).toString(), "ate" to hoje.toString()))
        if (api.operacaoSessao.value != esperado) throw SessaoAlterada()
        guardado.value = Dados(esperado.contexto.usuarioId, DadosRotina(resposta.checkins, resposta.agua, resposta.pesos))
    }

    override fun registrarCheckin(dietaId: Uuid, refeicaoId: Uuid, data: LocalDate, status: StatusCheckin, trocas: List<TrocaCheckin>, agora: Instant) {
        require((status == StatusCheckin.TROCADO) == trocas.isNotEmpty() && status != StatusCheckin.OUTRO)
        val id = novoIdLocal()
        val corpo = JsonContrato.encodeToJsonElement(CheckinRequisicao(id, dietaId, refeicaoId, data, status, trocas, agora))
        otimista({ it.copy(checkins = it.checkins + CheckinResposta(id, donoAtual(), dietaId, refeicaoId, data, status, trocas, agora)) },
            { it.copy(checkins = it.checkins.filter { c -> c.id != id }) },
            { val r = api.enviar<CheckinResposta>("eu/checkins", corpo, id); substituindo { d -> d.copy(checkins = d.checkins.map { c -> if (c.id == id) r else c }) } })
    }

    override fun registrarAgua(ml: Int, agora: Instant) {
        require(ml in 1..5000)
        val id = novoIdLocal()
        val corpo = JsonContrato.encodeToJsonElement(AguaRequisicao(id, ml, agora))
        otimista({ it.copy(agua = it.agua + AguaResposta(id, donoAtual(), ml, agora)) }, { it.copy(agua = it.agua.filter { a -> a.id != id }) },
            { val r = api.enviar<AguaResposta>("eu/agua", corpo, id); substituindo { d -> d.copy(agua = d.agua.map { a -> if (a.id == id) r else a }) } })
    }

    override fun registrarPeso(pesoKg: Double, agora: Instant) {
        require(pesoKg in 20.0..400.0)
        val id = novoIdLocal()
        val corpo = JsonContrato.encodeToJsonElement(PesoRequisicao(id, pesoKg, agora))
        otimista({ it.copy(pesos = it.pesos + PesoResposta(id, donoAtual(), pesoKg, agora)) }, { it.copy(pesos = it.pesos.filter { p -> p.id != id }) },
            { val r = api.enviar<PesoResposta>("eu/pesos", corpo, id); substituindo { d -> d.copy(pesos = d.pesos.map { p -> if (p.id == id) r else p }) } })
    }

    private fun donoAtual(): Uuid = (api.sessao.value ?: throw SessaoAlterada()).usuarioId
    private fun contexto() = api.operacaoSessao.value?.takeIf { it.contexto.perfil == Perfil.PACIENTE } ?: throw SessaoAlterada()

    private fun atualizar(dono: Uuid, mudar: (DadosRotina) -> DadosRotina) = guardado.update { atual ->
        if (atual?.dono == dono) Dados(dono, mudar(atual.rotina)) else atual ?: Dados(dono, mudar(DadosRotina()))
    }

    private fun substituindo(mudar: (DadosRotina) -> DadosRotina): (DadosRotina) -> DadosRotina = mudar

    private fun otimista(adicionar: (DadosRotina) -> DadosRotina, desfazer: (DadosRotina) -> DadosRotina, enviar: suspend () -> ((DadosRotina) -> DadosRotina)) {
        val dono = donoAtual()
        contexto()
        atualizar(dono, adicionar)
        escopo.launch {
            try { val confirmar = enviar(); atualizar(dono, confirmar) }
            catch (cancelamento: CancellationException) { atualizar(dono, desfazer); throw cancelamento }
            catch (_: Exception) { atualizar(dono, desfazer) }
        }
    }

    private companion object { const val DIAS = 90 }
}
