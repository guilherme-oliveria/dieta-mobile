package org.br.dietamais.data.repositorio

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonObject
import org.br.dietamais.data.local.BancoDieta
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import kotlin.uuid.Uuid

interface PortaAvisos {
    /** Histórico do perfil ativo, do mais novo para o mais antigo. Vem do banco local, então funciona offline. */
    fun observar(): Flow<List<AvisoResposta>>
    /** Exigem rede; quem chama pede um sync em seguida para a marca de leitura voltar pelo diário. */
    suspend fun marcarLido(id: Uuid)
    suspend fun marcarTodosLidos()
}

class AvisosRepositorio(private val api: ApiDieta, private val banco: BancoDieta) : PortaAvisos {
    override fun observar(): Flow<List<AvisoResposta>> =
        banco.registroSyncQueries.daSessao("avisos").asFlow().mapToList(Dispatchers.Default).map { linhas ->
            try { linhas.map { JsonContrato.decodeFromString<AvisoResposta>(it) }.sortedWith(compareByDescending<AvisoResposta> { it.criadoEm }.thenByDescending { it.id.toString() }) }
            catch (_: Exception) { throw RespostaInvalida() }
        }

    override suspend fun marcarLido(id: Uuid) {
        val contexto = api.operacaoSessao.value ?: throw SessaoAlterada()
        val aviso = api.enviar<AvisoResposta>("eu/avisos/$id/lido")
        if (api.operacaoSessao.value != contexto || aviso.id != id) throw SessaoAlterada()
    }

    override suspend fun marcarTodosLidos() {
        val contexto = api.operacaoSessao.value ?: throw SessaoAlterada()
        api.enviarSemResposta("eu/avisos/lidos", JsonObject(emptyMap()))
        if (api.operacaoSessao.value != contexto) throw SessaoAlterada()
    }
}
