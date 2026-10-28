package org.br.dietamais.data.repositorio

import kotlinx.serialization.json.encodeToJsonElement
import org.br.dietamais.data.local.BancoDieta
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.sync.novoIdLocal
import kotlin.uuid.Uuid

/** Dietas de um paciente na visão da nutri (MOB-12/13). Online: rascunhos e publicação passam pela API. */
interface PortaDietas {
    suspend fun listar(pacienteId: Uuid): List<DietaResposta>
    suspend fun criar(pacienteId: Uuid, requisicao: SalvarDietaRequisicao, chave: Uuid): DietaResposta
    suspend fun salvar(id: Uuid, versao: Long, requisicao: SalvarDietaRequisicao): DietaResposta
    suspend fun publicar(id: Uuid, versao: Long): DietaSnapshotResposta
    suspend fun duplicar(id: Uuid, chave: Uuid = novoIdLocal()): DietaResposta
}

class DietasRepositorio(private val api: ApiDieta, private val banco: BancoDieta) : PortaDietas {
    override suspend fun listar(pacienteId: Uuid): List<DietaResposta> {
        val contexto = contexto()
        val resposta = api.obter<DietasResposta>("pacientes/$pacienteId/dietas")
        conferir(contexto)
        if (resposta.itens.any { it.pacienteId != pacienteId }) throw RespostaInvalida()
        return resposta.itens
    }
    override suspend fun criar(pacienteId: Uuid, requisicao: SalvarDietaRequisicao, chave: Uuid): DietaResposta {
        val contexto = contexto()
        val resposta = api.enviar<DietaResposta>("pacientes/$pacienteId/dietas", JsonContrato.encodeToJsonElement(requisicao), chave)
        conferir(contexto)
        if (resposta.pacienteId != pacienteId || (requisicao.id != null && resposta.id != requisicao.id)) throw RespostaInvalida()
        return resposta
    }
    override suspend fun salvar(id: Uuid, versao: Long, requisicao: SalvarDietaRequisicao): DietaResposta {
        val contexto = contexto()
        val resposta = api.substituir<DietaResposta>("dietas/$id", JsonContrato.encodeToJsonElement(requisicao), versao)
        conferir(contexto)
        if (resposta.id != id) throw RespostaInvalida()
        return resposta
    }
    override suspend fun publicar(id: Uuid, versao: Long): DietaSnapshotResposta {
        val contexto = contexto()
        val resposta = api.enviarComVersao<DietaSnapshotResposta>("dietas/$id/publicar", versao)
        conferir(contexto)
        if (resposta.dietaId != id) throw RespostaInvalida()
        return resposta
    }
    override suspend fun duplicar(id: Uuid, chave: Uuid): DietaResposta {
        val contexto = contexto()
        val resposta = api.enviar<DietaResposta>("dietas/$id/duplicar", chave = chave)
        conferir(contexto)
        return resposta
    }
    private fun contexto() = api.operacaoSessao.value?.takeIf { it.contexto.perfil == Perfil.NUTRICIONISTA } ?: throw SessaoAlterada()
    private fun conferir(esperado: OperacaoSessao) {
        val sessao = banco.sessaoQueries.atual().executeAsOneOrNull()
        if (api.operacaoSessao.value != esperado || sessao?.usuario_id != esperado.contexto.usuarioId.toString() || sessao.perfil != esperado.contexto.perfil.name) throw SessaoAlterada()
    }
}
