package org.br.dietamais.data.repositorio

import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.time.Instant
import kotlin.uuid.Uuid

/** Financeiro da consulta (BE-65, G-20): só online e só da nutri; fora do sync. */
interface PortaFinanceiro {
    suspend fun da(consulta: Uuid): FinanceiroConsultaResposta
    suspend fun salvar(consulta: Uuid, versao: Long, requisicao: AtualizarFinanceiroConsultaRequisicao): FinanceiroConsultaResposta
    suspend fun resumo(de: Instant, ate: Instant): ResumoFinanceiroResposta
}

class FinanceiroRepositorio(private val api: ApiDieta) : PortaFinanceiro {
    private fun exigirNutri() = api.operacaoSessao.value?.takeIf { it.contexto.perfil == Perfil.NUTRICIONISTA } ?: throw SessaoAlterada()

    override suspend fun da(consulta: Uuid): FinanceiroConsultaResposta {
        val contexto = exigirNutri()
        val r = api.obter<FinanceiroConsultaResposta>("consultas/$consulta/financeiro")
        if (api.operacaoSessao.value != contexto) throw SessaoAlterada()
        return r
    }

    override suspend fun salvar(consulta: Uuid, versao: Long, requisicao: AtualizarFinanceiroConsultaRequisicao): FinanceiroConsultaResposta {
        val contexto = exigirNutri()
        val r = api.substituir<FinanceiroConsultaResposta>("consultas/$consulta/financeiro", JsonContrato.encodeToJsonElement(requisicao), versao)
        if (api.operacaoSessao.value != contexto) throw SessaoAlterada()
        return r
    }

    override suspend fun resumo(de: Instant, ate: Instant): ResumoFinanceiroResposta {
        val contexto = exigirNutri()
        val r = api.obter<ResumoFinanceiroResposta>("eu/nutri/financeiro", mapOf("de" to de.toString(), "ate" to ate.toString()))
        if (api.operacaoSessao.value != contexto) throw SessaoAlterada()
        return r
    }
}
