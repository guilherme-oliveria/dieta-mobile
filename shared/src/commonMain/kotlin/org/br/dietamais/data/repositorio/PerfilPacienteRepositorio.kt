package org.br.dietamais.data.repositorio

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.br.dietamais.data.local.BancoDieta
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*

/** Vínculos do paciente já sincronizados (leitura local; a escolha de nutri ativa é P2). */
interface PortaVinculosLocais { fun observarVinculos(): Flow<List<VinculoResposta>> }

class VinculosLocaisRepositorio(private val banco: BancoDieta) : PortaVinculosLocais {
    override fun observarVinculos(): Flow<List<VinculoResposta>> = banco.registroSyncQueries.daSessao("vinculos").asFlow().mapToList(Dispatchers.Default).map { linhas ->
        try { linhas.map { JsonContrato.decodeFromString<VinculoResposta>(it) }.sortedBy { it.nutricionista.nome.lowercase() } } catch (_: Exception) { throw RespostaInvalida() }
    }
}

/** Link .ics da agenda do celular (BE-17). Gerar outro link revoga o anterior; o token só aparece na criação. */
interface PortaCalendario {
    suspend fun gerar(): CalendarioTokenResposta
    suspend fun revogar()
}

class CalendarioRepositorio(private val api: ApiDieta) : PortaCalendario {
    override suspend fun gerar(): CalendarioTokenResposta {
        val contexto = api.operacaoSessao.value?.takeIf { it.contexto.perfil == Perfil.PACIENTE } ?: throw SessaoAlterada()
        val resposta = api.enviar<CalendarioTokenResposta>("eu/calendario/token")
        if (api.operacaoSessao.value != contexto) throw SessaoAlterada()
        return resposta
    }
    override suspend fun revogar() {
        val contexto = api.operacaoSessao.value?.takeIf { it.contexto.perfil == Perfil.PACIENTE } ?: throw SessaoAlterada()
        api.excluir("eu/calendario/token")
        if (api.operacaoSessao.value != contexto) throw SessaoAlterada()
    }
}
