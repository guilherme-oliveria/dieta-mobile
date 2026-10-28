package org.br.dietamais.data.repositorio

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import org.br.dietamais.data.local.BancoDieta
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.sync.*
import kotlin.uuid.Uuid

interface PortaPacientes {
    fun observar(): Flow<List<PacienteResposta>>
    suspend fun atualizar()
    suspend fun criar(requisicao: CriarPacienteRequisicao, chave: Uuid): PacienteResposta
    suspend fun convidar(id: Uuid, chave: Uuid): ConviteCriadoResposta
    suspend fun assinatura(): AssinaturaResumoResposta?
}

class PacientesRepositorio(private val api: ApiDieta, private val banco: BancoDieta,
    private val sincronizador: Sincronizador = criarSincronizador(api, banco), private val aoAlterar: () -> Unit = {},
    private val escrita: EscritaOffline = EscritaOffline(banco, aoEnfileirar = aoAlterar), private val agora: () -> kotlin.time.Instant = { kotlin.time.Clock.System.now() }) : PortaPacientes {
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
        val iniciado = sincronizador.puxar()
        val resultado = if (iniciado == ResultadoLeituraSync.EM_ANDAMENTO)
            sincronizador.estado.first { it.resultado != ResultadoLeituraSync.EM_ANDAMENTO }.resultado else iniciado
        when (resultado) {
            ResultadoLeituraSync.CONCLUIDO -> conferir(contexto)
            ResultadoLeituraSync.SEM_REDE -> throw FalhaComunicacao()
            ResultadoLeituraSync.SEM_SESSAO, ResultadoLeituraSync.SESSAO_ALTERADA, ResultadoLeituraSync.CANCELADO -> throw SessaoAlterada()
            else -> {
                val erro = sincronizador.estado.value
                if (erro.codigoErro != null) throw ErroApi(erro.codigoErro, null, emptyList(), erro.statusHttp ?: 500, null, null)
                throw RespostaInvalida()
            }
        }
    }

    /**
     * Offline-first (MOB-29): grava a ficha otimista e a mutação na fila. O `vinculoId` provisório é o id da ficha; o servidor traz o real no pull.
     * Limite de plano e e-mail repetido (RN-ASS-03, RN-PAC) são decididos no /sync e voltam como pendência recusada.
     */
    override suspend fun criar(requisicao: CriarPacienteRequisicao, chave: Uuid): PacienteResposta = operacoes.withLock {
        contexto()
        val agoraLocal = agora()
        val otimista = PacienteResposta(requisicao.id, requisicao.id, requisicao.nome, requisicao.email, requisicao.dataNascimento, requisicao.sexo, requisicao.telefone,
            requisicao.estaturaReferencia, requisicao.objetivo, requisicao.observacoes, StatusVinculo.CONVIDADO, 0, agoraLocal, agoraLocal)
        escrita.criar("pacientes", requisicao.id, JsonContrato.encodeToJsonElement(requisicao).jsonObject, JsonContrato.encodeToString(otimista))
        otimista
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
