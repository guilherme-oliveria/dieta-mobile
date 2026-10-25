package org.br.dietamais.data.remoto

import io.ktor.client.plugins.auth.providers.BearerTokens
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.br.dietamais.data.local.BancoDieta
import org.br.dietamais.data.remoto.dto.Perfil
import org.br.dietamais.data.remoto.dto.SessaoResposta
import org.br.dietamais.plataforma.CofreSessao
import kotlin.uuid.Uuid

data class ContextoSessao(val usuarioId: Uuid, val perfil: Perfil)

internal class SessaoLocal(private val cofre: CofreSessao, private val banco: BancoDieta) {
    private val mutex = Mutex()
    private var carregada = false
    private var atual: SessaoResposta? = null
    private var geracao = 0L
    private val estado = MutableStateFlow<ContextoSessao?>(null)
    val fluxo = estado.asStateFlow()

    suspend fun tokens(): BearerTokens? = mutex.withLock { carregar()?.tokens() }
    suspend fun geracao(): Long = mutex.withLock { carregar(); geracao }
    suspend fun corresponde(esperada: Long?): Boolean = mutex.withLock { esperada == geracao }

    suspend fun rejeitar(esperada: Long?) = mutex.withLock {
        if (esperada == geracao) withContext(NonCancellable) { apagar() }
    }

    suspend fun receber(resposta: SessaoResposta, esperada: Long) = mutex.withLock {
        withContext(NonCancellable) {
            if (esperada != geracao) throw SessaoAlterada()
            val anterior = carregar()
            if (resposta.accessToken == null && resposta.refreshToken == null && resposta.expiraEm == null && resposta.usuario.perfil == null) {
                apagar()
            } else {
                try { validar(resposta) } catch (falha: RespostaInvalida) { apagar(); throw falha }
                if (anterior?.usuario?.id != resposta.usuario.id || anterior.usuario.perfil != resposta.usuario.perfil) limparBanco()
                geracao++
                salvar(resposta)
            }
        }
    }

    suspend fun renovar(esperada: Long?, antigo: String?, renovar: suspend (String) -> SessaoResposta): BearerTokens? = mutex.withLock {
        withContext(NonCancellable) {
            if (esperada != geracao) return@withContext null
            val sessao = carregar() ?: return@withContext null
            // Outro 401 pode ter sido atendido antes deste, ou pode ter ocorrido login/logout.
            if (sessao.accessToken != antigo) return@withContext sessao.tokens()
            try {
                val resposta = renovar(requireNotNull(sessao.refreshToken))
                validar(resposta)
                if (resposta.usuario.id != sessao.usuario.id || resposta.usuario.perfil != sessao.usuario.perfil) throw RespostaInvalida()
                salvar(resposta)
                resposta.tokens()
            } catch (falha: Exception) {
                apagar()
                null
            }
        }
    }

    suspend fun encerrar(revogar: suspend (BearerTokens?) -> Unit = {}) = mutex.withLock {
        withContext(NonCancellable) {
            try { revogar(carregar()?.tokens()) } finally { apagar() }
        }
    }

    private suspend fun carregar(): SessaoResposta? {
        if (!carregada) {
            val texto = cofre.ler()
            val resposta = texto?.let {
                try { JsonContrato.decodeFromString<SessaoResposta>(it).also(::validar) }
                catch (falha: Exception) { apagar(); throw RespostaInvalida() }
            }
            if (resposta == null) limparBanco() else banco.sessaoQueries.salvar(resposta.usuario.id.toString(), resposta.usuario.perfil!!.name)
            atual = resposta
            carregada = true
            estado.value = resposta?.contexto()
        }
        return atual
    }

    private fun validar(resposta: SessaoResposta) {
        if (resposta.accessToken.isNullOrBlank() || resposta.refreshToken.isNullOrBlank() || resposta.expiraEm == null ||
            resposta.usuario.perfil !in setOf(Perfil.NUTRICIONISTA, Perfil.PACIENTE) || resposta.usuario.perfil !in resposta.usuario.perfis) throw RespostaInvalida()
    }

    private suspend fun salvar(resposta: SessaoResposta) {
        try {
            cofre.gravar(JsonContrato.encodeToString(resposta))
            banco.sessaoQueries.salvar(resposta.usuario.id.toString(), resposta.usuario.perfil!!.name)
            atual = resposta
            carregada = true
            estado.value = resposta.contexto()
        } catch (falha: Exception) {
            apagar()
            throw falha
        }
    }

    private suspend fun apagar() {
        geracao++
        atual = null
        carregada = true
        estado.value = null
        try { cofre.limpar() } finally { limparBanco() }
    }

    private fun limparBanco() = banco.transaction {
        banco.conflitoQueries.limpar()
        banco.mutacaoPendenteQueries.limpar()
        banco.cursorSyncQueries.limpar()
        banco.sessaoQueries.limpar()
    }

    private fun SessaoResposta.tokens() = BearerTokens(requireNotNull(accessToken), refreshToken)
    private fun SessaoResposta.contexto() = ContextoSessao(usuario.id, requireNotNull(usuario.perfil))
}
