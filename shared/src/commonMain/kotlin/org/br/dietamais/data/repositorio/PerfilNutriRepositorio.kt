package org.br.dietamais.data.repositorio

import kotlinx.serialization.json.encodeToJsonElement
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*

interface PortaPerfilNutri {
    suspend fun obter(): PerfilNutriResposta
    suspend fun salvar(versao: Long, requisicao: AtualizarPerfilNutriRequisicao): PerfilNutriResposta
}

class PerfilNutriRepositorio(private val api: ApiDieta) : PortaPerfilNutri {
    override suspend fun obter(): PerfilNutriResposta {
        val contexto = contexto()
        return api.obter<PerfilNutriResposta>("eu/nutri").also { conferir(contexto) }
    }
    override suspend fun salvar(versao: Long, requisicao: AtualizarPerfilNutriRequisicao): PerfilNutriResposta {
        val contexto = contexto()
        return api.alterar<PerfilNutriResposta>("eu/nutri", JsonContrato.encodeToJsonElement(requisicao), versao).also { conferir(contexto) }
    }
    private fun contexto() = api.operacaoSessao.value?.takeIf { it.contexto.perfil == Perfil.NUTRICIONISTA } ?: throw SessaoAlterada()
    private fun conferir(esperado: OperacaoSessao) { if (api.operacaoSessao.value != esperado) throw SessaoAlterada() }
}
