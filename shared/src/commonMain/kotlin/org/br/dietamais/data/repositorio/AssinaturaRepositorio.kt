package org.br.dietamais.data.repositorio

import kotlinx.serialization.json.encodeToJsonElement
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*

interface PortaAssinatura {
    suspend fun obter(): AssinaturaResposta
    suspend fun pedirCheckout(plano: String): CheckoutResposta
}

class AssinaturaRepositorio(private val api: ApiDieta) : PortaAssinatura {
    override suspend fun obter(): AssinaturaResposta {
        val contexto = contexto()
        return api.obter<AssinaturaResposta>("assinatura").also { conferir(contexto) }
    }
    override suspend fun pedirCheckout(plano: String): CheckoutResposta {
        val contexto = contexto()
        return api.enviar<CheckoutResposta>("assinatura/checkout", JsonContrato.encodeToJsonElement(CheckoutRequisicao(plano))).also { conferir(contexto) }
    }
    private fun contexto() = api.operacaoSessao.value?.takeIf { it.contexto.perfil == Perfil.NUTRICIONISTA } ?: throw SessaoAlterada()
    private fun conferir(esperado: OperacaoSessao) { if (api.operacaoSessao.value != esperado) throw SessaoAlterada() }
}
