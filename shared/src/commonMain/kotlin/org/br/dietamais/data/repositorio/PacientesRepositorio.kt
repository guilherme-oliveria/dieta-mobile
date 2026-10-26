package org.br.dietamais.data.repositorio

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.encodeToJsonElement
import org.br.dietamais.data.local.BancoDieta
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import kotlin.uuid.Uuid

interface PortaPacientes {
    fun observar(): Flow<List<PacienteResposta>>
    suspend fun atualizar()
    suspend fun criar(requisicao: CriarPacienteRequisicao, chave: Uuid): PacienteResposta
    suspend fun convidar(id: Uuid, chave: Uuid): ConviteCriadoResposta
    suspend fun assinatura(): AssinaturaResumoResposta?
}

class PacientesRepositorio(private val api: ApiDieta, private val banco: BancoDieta) : PortaPacientes {
    private val operacoes = Mutex()
    override suspend fun assinatura(): AssinaturaResumoResposta? {
        val contexto = contexto()
        val eu = api.obter<EuResposta>("eu")
        conferir(contexto)
        if (eu.usuario.id != contexto.usuarioId || eu.usuario.perfil != contexto.perfil) throw RespostaInvalida()
        return eu.assinatura
    }
    override fun observar(): Flow<List<PacienteResposta>> = banco.pacienteCacheQueries.daSessao().asFlow()
        .mapToList(Dispatchers.Default).map { registros ->
            try { registros.map { JsonContrato.decodeFromString<PacienteResposta>(it) }
                .sortedWith(compareBy<PacienteResposta> { it.nome.lowercase() }.thenBy { it.vinculoId.toString() }) }
            catch (_: Exception) { throw RespostaInvalida() }
        }

    override suspend fun atualizar() = operacoes.withLock {
        val contexto = contexto()
        val recebidos = linkedMapOf<Uuid, PacienteResposta>()
        var pagina = 0
        do {
            val resposta = api.obter<PaginaPacientesResposta>("pacientes", mapOf("pagina" to pagina.toString(), "tamanho" to "20", "ordem" to "nome,asc"))
            if (resposta.pagina != pagina || resposta.tamanho != 20 || resposta.total < 0 || resposta.itens.size > 20) throw RespostaInvalida()
            resposta.itens.forEach { recebidos[it.vinculoId] = it }
            pagina++
        } while (resposta.itens.size == 20 && pagina.toLong() * 20 < resposta.total)
        banco.transaction {
            conferir(contexto)
            banco.pacienteCacheQueries.limparDaConta(contexto.usuarioId.toString(), contexto.perfil.name)
            recebidos.values.forEach { salvar(contexto, it) }
        }
    }

    override suspend fun criar(requisicao: CriarPacienteRequisicao, chave: Uuid): PacienteResposta = operacoes.withLock {
        val contexto = contexto()
        val resposta = api.enviar<PacienteResposta>("pacientes", JsonContrato.encodeToJsonElement(requisicao), chave)
        if (resposta.id != requisicao.id) throw RespostaInvalida()
        banco.transaction { conferir(contexto); salvar(contexto, resposta) }
        resposta
    }

    override suspend fun convidar(id: Uuid, chave: Uuid): ConviteCriadoResposta = operacoes.withLock {
        val contexto = contexto()
        val resposta = api.enviar<ConviteCriadoResposta>("pacientes/$id/convite", chave = chave)
        conferir(contexto)
        val link = try { Url(resposta.link) } catch (_: Exception) { throw RespostaInvalida() }
        val protocoloPermitido = link.protocol == URLProtocol.HTTPS || (link.protocol.name == "dietamais" && link.host == "convites")
        if (resposta.codigo.length != 8 || !protocoloPermitido || !link.user.isNullOrEmpty() || !link.password.isNullOrEmpty()) throw RespostaInvalida()
        resposta
    }

    private fun contexto(): ContextoSessao = api.sessao.value?.takeIf { it.perfil == Perfil.NUTRICIONISTA } ?: throw SessaoAlterada()
    private fun conferir(esperado: ContextoSessao) {
        val sessao = banco.sessaoQueries.atual().executeAsOneOrNull()
        if (api.sessao.value != esperado || sessao?.usuario_id != esperado.usuarioId.toString() || sessao.perfil != esperado.perfil.name) throw SessaoAlterada()
    }
    private fun salvar(contexto: ContextoSessao, paciente: PacienteResposta) {
        banco.pacienteCacheQueries.salvar(contexto.usuarioId.toString(), contexto.perfil.name, paciente.vinculoId.toString(), paciente.id.toString(), JsonContrato.encodeToString(paciente))
    }
}
