package org.br.dietamais.data.repositorio

import kotlinx.serialization.json.encodeToJsonElement
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*

/** "Meus protocolos" (BE-42): leitura e gravação online, com `If-Match` (versão `0` na primeira gravação). */
interface PortaProtocolosNutri {
    suspend fun obter(): ProtocolosNutriResposta
    suspend fun salvar(versao: Long, requisicao: SalvarProtocolosNutriRequisicao): ProtocolosNutriResposta
}

class ProtocolosNutriRepositorio(private val api: ApiDieta) : PortaProtocolosNutri {
    override suspend fun obter(): ProtocolosNutriResposta {
        val contexto = contexto()
        return api.obter<ProtocolosNutriResposta>("eu/nutri/protocolos").also { conferir(contexto) }
    }
    override suspend fun salvar(versao: Long, requisicao: SalvarProtocolosNutriRequisicao): ProtocolosNutriResposta {
        val contexto = contexto()
        return api.alterar<ProtocolosNutriResposta>("eu/nutri/protocolos", JsonContrato.encodeToJsonElement(requisicao), versao).also { conferir(contexto) }
    }
    private fun contexto() = api.operacaoSessao.value?.takeIf { it.contexto.perfil == Perfil.NUTRICIONISTA } ?: throw SessaoAlterada()
    private fun conferir(esperado: OperacaoSessao) { if (api.operacaoSessao.value != esperado) throw SessaoAlterada() }
}
