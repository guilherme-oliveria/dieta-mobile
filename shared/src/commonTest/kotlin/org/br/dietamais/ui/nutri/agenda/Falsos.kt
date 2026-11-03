package org.br.dietamais.ui.nutri.agenda

import kotlinx.coroutines.flow.MutableStateFlow
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.PortaConsultas
import org.br.dietamais.data.repositorio.PortaPacientes
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlin.uuid.Uuid

val agoraFalso = Instant.parse("2026-10-28T12:00:00Z") // quarta, 09:00 em São Paulo

fun consultaFalsa(pacienteId: Uuid, inicio: Instant, duracaoMin: Int = 50, tipo: TipoConsulta = TipoConsulta.RETORNO, status: StatusConsulta = StatusConsulta.AGENDADA, lugar: String? = null, versao: Long = 1) =
    ConsultaResposta(Uuid.random(), pacienteId, Uuid.random(), inicio, duracaoMin, tipo, lugar, status, versao, agoraFalso, agoraFalso)

fun pacienteFalso(nome: String, status: StatusVinculo = StatusVinculo.ATIVO) =
    PacienteResposta(Uuid.random(), Uuid.random(), nome, status = status, versao = 1, criadoEm = agoraFalso, atualizadoEm = agoraFalso)

class PortaConsultasFalsa(inicial: List<ConsultaResposta> = emptyList()) : PortaConsultas {
    val dados = MutableStateFlow(inicial)
    var falha: Exception? = null
    var atualizacoes = 0
    var criada: CriarConsultaRequisicao? = null
    var alterada: Triple<Uuid, Long, AtualizarConsultaRequisicao>? = null
    override fun observar() = dados
    override suspend fun atualizar(de: Instant, ate: Instant) { atualizacoes++; falha?.let { throw it } }
    override suspend fun criar(requisicao: CriarConsultaRequisicao): ConsultaResposta {
        falha?.let { throw it }; criada = requisicao
        return ConsultaResposta(requisicao.id!!, requisicao.pacienteId, Uuid.random(), requisicao.inicio, requisicao.duracaoMin, requisicao.tipo, requisicao.lugar, StatusConsulta.AGENDADA, 1, agoraFalso, agoraFalso)
            .also { dados.value = dados.value + it }
    }
    override suspend fun alterar(id: Uuid, versao: Long, requisicao: AtualizarConsultaRequisicao): ConsultaResposta {
        falha?.let { throw it }; alterada = Triple(id, versao, requisicao)
        val atual = dados.value.first { it.id == id }
        val nova = atual.copy(inicio = requisicao.inicio ?: atual.inicio, duracaoMin = requisicao.duracaoMin ?: atual.duracaoMin, tipo = requisicao.tipo ?: atual.tipo,
            lugar = requisicao.lugar?.ifEmpty { null } ?: atual.lugar, status = requisicao.status ?: atual.status, versao = versao + 1)
        dados.value = dados.value.map { if (it.id == id) nova else it }
        return nova
    }
}

class PortaPacientesFalsa(inicial: List<PacienteResposta>) : PortaPacientes {
    val dados = MutableStateFlow(inicial)
    override fun observar() = dados
    override suspend fun assinatura(): AssinaturaResumoResposta? = null
    override suspend fun atualizar() {}
    override suspend fun criar(requisicao: CriarPacienteRequisicao, chave: Uuid): PacienteResposta = error("não usado")
    override suspend fun convidar(id: Uuid, chave: Uuid): ConviteCriadoResposta = error("não usado")
}
