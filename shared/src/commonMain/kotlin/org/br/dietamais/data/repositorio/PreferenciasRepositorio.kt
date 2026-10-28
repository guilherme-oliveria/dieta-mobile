package org.br.dietamais.data.repositorio

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.encodeToJsonElement
import org.br.dietamais.data.local.BancoDieta
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*

interface PortaPreferencias {
    /** Último valor do servidor guardado no aparelho; null até a primeira leitura. */
    fun observar(): Flow<PreferenciasNotificacaoResposta?>
    /** Lembretes locais de refeição ligados? (padrão: sim). Só existe no aparelho. */
    fun observarLembretes(): Flow<Boolean>
    fun definirLembretes(ativo: Boolean)
    suspend fun atualizar(): PreferenciasNotificacaoResposta
    suspend fun salvar(requisicao: AtualizarPreferenciasRequisicao): PreferenciasNotificacaoResposta
}

/** Guarda em `registro_sync` (por conta): `preferencias/notificacao` com a versão do servidor e `preferencias-locais/lembretes`. */
class PreferenciasRepositorio(private val api: ApiDieta, private val banco: BancoDieta) : PortaPreferencias {
    override fun observar(): Flow<PreferenciasNotificacaoResposta?> = banco.registroSyncQueries.daSessao("preferencias").asFlow().mapToList(Dispatchers.Default).map { linhas ->
        try { linhas.firstOrNull()?.let { JsonContrato.decodeFromString<PreferenciasNotificacaoResposta>(it) } } catch (_: Exception) { throw RespostaInvalida() }
    }

    override fun observarLembretes(): Flow<Boolean> = banco.registroSyncQueries.daSessao("preferencias-locais").asFlow().mapToList(Dispatchers.Default).map { it.firstOrNull() != "false" }

    override fun definirLembretes(ativo: Boolean) {
        val s = banco.sessaoQueries.atual().executeAsOneOrNull() ?: throw SessaoAlterada()
        banco.registroSyncQueries.salvar(s.usuario_id, s.perfil, "preferencias-locais", "lembretes", 0, 0, ativo.toString())
    }

    override suspend fun atualizar(): PreferenciasNotificacaoResposta {
        val contexto = api.operacaoSessao.value ?: throw SessaoAlterada()
        val resposta = api.obter<PreferenciasNotificacaoResposta>("eu/preferencias-notificacao")
        guardar(contexto, resposta); return resposta
    }

    override suspend fun salvar(requisicao: AtualizarPreferenciasRequisicao): PreferenciasNotificacaoResposta {
        val contexto = api.operacaoSessao.value ?: throw SessaoAlterada()
        val versao = banco.registroSyncQueries.revisao(contexto.contexto.usuarioId.toString(), contexto.contexto.perfil.name, "preferencias", "notificacao").executeAsOneOrNull() ?: 0L
        val resposta = api.substituir<PreferenciasNotificacaoResposta>("eu/preferencias-notificacao", JsonContrato.encodeToJsonElement(requisicao), versao)
        guardar(contexto, resposta); return resposta
    }

    private fun guardar(contexto: OperacaoSessao, resposta: PreferenciasNotificacaoResposta) {
        if (api.operacaoSessao.value != contexto) throw SessaoAlterada()
        banco.registroSyncQueries.salvar(contexto.contexto.usuarioId.toString(), contexto.contexto.perfil.name, "preferencias", "notificacao",
            resposta.versao, 0, JsonContrato.encodeToString(resposta))
    }
}
