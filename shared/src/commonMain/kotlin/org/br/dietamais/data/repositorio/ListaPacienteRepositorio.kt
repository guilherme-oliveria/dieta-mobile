package org.br.dietamais.data.repositorio

import kotlinx.serialization.json.encodeToJsonElement
import org.br.dietamais.data.local.BancoDieta
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import kotlin.uuid.Uuid

/** Lista de substituição de um paciente (MOB-11). Online nesta fase: o backend decide e devolve a lista efetiva. */
interface PortaListaPaciente {
    suspend fun obter(pacienteId: Uuid): ListaSubstituicaoResposta
    suspend fun salvar(pacienteId: Uuid, versao: Long, requisicao: AtualizarListaSubstituicaoRequisicao): ListaSubstituicaoResposta
}

class ListaPacienteRepositorio(private val api: ApiDieta, private val conferirArmazem: (OperacaoSessao) -> Unit) : PortaListaPaciente {
    constructor(api: ApiDieta, banco: BancoDieta) : this(api, { esperado ->
        val sessao = banco.sessaoQueries.atual().executeAsOneOrNull()
        if (sessao?.usuario_id != esperado.contexto.usuarioId.toString() || sessao.perfil != esperado.contexto.perfil.name) throw SessaoAlterada()
    })
    override suspend fun obter(pacienteId: Uuid): ListaSubstituicaoResposta {
        val contexto = contexto()
        val resposta = api.obter<ListaSubstituicaoResposta>("pacientes/$pacienteId/lista-substituicao")
        conferir(contexto)
        if (resposta.pacienteId != pacienteId) throw RespostaInvalida()
        return resposta
    }
    override suspend fun salvar(pacienteId: Uuid, versao: Long, requisicao: AtualizarListaSubstituicaoRequisicao): ListaSubstituicaoResposta {
        val contexto = contexto()
        val resposta = api.substituir<ListaSubstituicaoResposta>("pacientes/$pacienteId/lista-substituicao", JsonContrato.encodeToJsonElement(requisicao), versao)
        conferir(contexto)
        if (resposta.pacienteId != pacienteId) throw RespostaInvalida()
        return resposta
    }
    private fun contexto() = api.operacaoSessao.value?.takeIf { it.contexto.perfil == Perfil.NUTRICIONISTA } ?: throw SessaoAlterada()
    private fun conferir(esperado: OperacaoSessao) {
        if (api.operacaoSessao.value != esperado) throw SessaoAlterada()
        conferirArmazem(esperado)
    }
}
