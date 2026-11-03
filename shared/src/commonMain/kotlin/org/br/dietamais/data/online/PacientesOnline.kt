package org.br.dietamais.data.online

import io.ktor.http.URLProtocol
import io.ktor.http.Url
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.encodeToJsonElement
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.PortaPacientes
import kotlin.uuid.Uuid

/** Pacientes da nutri direto da API (web, sem fila). Limite de plano e e-mail repetido voltam como erro da própria chamada. */
class PacientesOnline(private val api: ApiDieta) : PortaPacientes {
    private val recurso = RecursoOnline(api.sessao) { carregarTodos() }
    override fun observar(): Flow<List<PacienteResposta>> = recurso.fluxo

    override suspend fun atualizar() {
        contexto()
        recurso.atualizar()
    }

    override suspend fun criar(requisicao: CriarPacienteRequisicao, chave: Uuid): PacienteResposta {
        val esperado = contexto()
        val criado = api.enviar<PacienteResposta>("pacientes", JsonContrato.encodeToJsonElement(requisicao), chave)
        conferir(esperado)
        if (requisicao.id != null && criado.id != requisicao.id) throw RespostaInvalida()
        recurso.gravado(criado) { it.vinculoId == criado.vinculoId }
        return criado
    }

    override suspend fun convidar(id: Uuid, chave: Uuid): ConviteCriadoResposta {
        val esperado = contexto()
        val resposta = api.enviar<ConviteCriadoResposta>("pacientes/$id/convite", chave = chave)
        conferir(esperado)
        val link = try { Url(resposta.link) } catch (_: Exception) { throw RespostaInvalida() }
        if (resposta.codigo.length != 8 || link.protocol != URLProtocol.HTTPS || !link.user.isNullOrEmpty() || !link.password.isNullOrEmpty()) throw RespostaInvalida()
        return resposta
    }

    override suspend fun assinatura(): AssinaturaResumoResposta? {
        val esperado = contexto()
        val eu = api.obter<EuResposta>("eu")
        conferir(esperado)
        return eu.assinatura
    }

    private suspend fun carregarTodos(): List<PacienteResposta> {
        val esperado = contexto()
        val todos = mutableListOf<PacienteResposta>()
        var pagina = 0
        do {
            val resposta = api.obter<PaginaPacientesResposta>("pacientes", mapOf("pagina" to pagina.toString(), "tamanho" to "100", "ordem" to "nome,asc"))
            conferir(esperado)
            if (resposta.pagina != pagina || resposta.total < 0 || (resposta.itens.isEmpty() && todos.size < resposta.total)) throw RespostaInvalida()
            todos += resposta.itens
            pagina++
        } while (todos.size < resposta.total)
        if (todos.map { it.vinculoId }.distinct().size != todos.size) throw RespostaInvalida()
        return todos
    }

    private fun contexto() = api.operacaoSessao.value?.takeIf { it.contexto.perfil == Perfil.NUTRICIONISTA } ?: throw SessaoAlterada()
    private fun conferir(esperado: OperacaoSessao) { if (api.operacaoSessao.value != esperado) throw SessaoAlterada() }
}
