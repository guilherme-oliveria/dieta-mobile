package org.br.dietamais.data.repositorio

import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.remoto.dto.ExameCatalogoDto
import org.br.dietamais.data.remoto.dto.ExamesCatalogoResposta

/** Catálogo de exames por faixa etária (BE-56a): só online, a nutri precisa de rede para escolher exames. */
interface PortaExames {
    suspend fun catalogo(faixaEtaria: String): List<ExameCatalogoDto>
}

class ExamesRepositorio(private val api: ApiDieta) : PortaExames {
    override suspend fun catalogo(faixaEtaria: String): List<ExameCatalogoDto> {
        val contexto = api.operacaoSessao.value?.takeIf { it.contexto.perfil == Perfil.NUTRICIONISTA } ?: throw SessaoAlterada()
        val resposta = api.obter<ExamesCatalogoResposta>("referencias/exames", mapOf("faixaEtaria" to faixaEtaria))
        if (api.operacaoSessao.value != contexto) throw SessaoAlterada()
        return resposta.itens
    }
}
