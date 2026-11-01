package org.br.dietamais.data.repositorio

import kotlinx.serialization.json.encodeToJsonElement
import org.br.dietamais.data.local.BancoDieta
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.sync.novoIdLocal
import kotlin.uuid.Uuid

/**
 * Modelos de dieta da nutri (MOB-56, BE-52). Só online: a lista vem de `GET /eu/nutri/modelos-dieta` (sistema e dela) e as ações passam pela API.
 * A leitura offline pelo sync (`modelosDieta`) fica para a MOB-29b.
 */
interface PortaModelosDieta {
    suspend fun listar(): List<ModeloDietaResposta>
    suspend fun duplicar(id: Uuid, chave: Uuid = novoIdLocal()): ModeloDietaResposta
    suspend fun excluir(id: Uuid, versao: Long)
    suspend fun salvarComoModelo(dietaId: Uuid, requisicao: SalvarComoModeloRequisicao, chave: Uuid = novoIdLocal()): ModeloDietaResposta
}

class ModelosDietaRepositorio(private val api: ApiDieta, private val conferirArmazem: (OperacaoSessao) -> Unit) : PortaModelosDieta {
    constructor(api: ApiDieta, banco: BancoDieta) : this(api, { esperado ->
        val sessao = banco.sessaoQueries.atual().executeAsOneOrNull()
        if (sessao?.usuario_id != esperado.contexto.usuarioId.toString() || sessao.perfil != esperado.contexto.perfil.name) throw SessaoAlterada()
    })

    override suspend fun listar(): List<ModeloDietaResposta> {
        val contexto = contexto()
        val resposta = api.obter<ModelosDietaResposta>("eu/nutri/modelos-dieta")
        conferir(contexto)
        return resposta.itens
    }
    override suspend fun duplicar(id: Uuid, chave: Uuid): ModeloDietaResposta {
        val contexto = contexto()
        val resposta = api.enviar<ModeloDietaResposta>("eu/nutri/modelos-dieta/$id/duplicar", chave = chave)
        conferir(contexto)
        return resposta
    }
    override suspend fun excluir(id: Uuid, versao: Long) {
        val contexto = contexto()
        api.excluirComVersao("eu/nutri/modelos-dieta/$id", versao)
        conferir(contexto)
    }
    override suspend fun salvarComoModelo(dietaId: Uuid, requisicao: SalvarComoModeloRequisicao, chave: Uuid): ModeloDietaResposta {
        val contexto = contexto()
        val resposta = api.enviar<ModeloDietaResposta>("eu/nutri/modelos-dieta/de-dieta/$dietaId", JsonContrato.encodeToJsonElement(requisicao), chave)
        conferir(contexto)
        return resposta
    }
    private fun contexto() = api.operacaoSessao.value?.takeIf { it.contexto.perfil == Perfil.NUTRICIONISTA } ?: throw SessaoAlterada()
    private fun conferir(esperado: OperacaoSessao) {
        if (api.operacaoSessao.value != esperado) throw SessaoAlterada()
        conferirArmazem(esperado)
    }
}
