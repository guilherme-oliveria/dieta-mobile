package org.br.dietamais.data.sync

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import org.br.dietamais.data.remoto.ErroApi
import org.br.dietamais.data.remoto.FalhaComunicacao
import org.br.dietamais.data.remoto.RespostaInvalida
import org.br.dietamais.data.remoto.dto.Perfil
import org.br.dietamais.data.remoto.dto.SyncResposta
import kotlin.uuid.Uuid

data class ContextoLeituraSync(val usuarioId: Uuid, val perfil: Perfil, val operacao: String) {
    init { require(operacao.isNotBlank() && perfil in setOf(Perfil.NUTRICIONISTA, Perfil.PACIENTE)) }
    override fun toString() = "ContextoLeituraSync[identidade protegida]"
}

interface FonteLeituraSync {
    suspend fun puxar(cursor: String?): SyncResposta
}

interface ArmazemLeituraSync {
    fun cursor(contexto: ContextoLeituraSync): String?
    /** Confere a operação ativa e aplica dados + cursor na mesma transação local. */
    fun aplicar(contexto: ContextoLeituraSync, pagina: SyncResposta)
}

enum class ResultadoLeituraSync { CONCLUIDO, SEM_SESSAO, SESSAO_ALTERADA, EM_ANDAMENTO, SEM_REDE, FALHOU, CANCELADO }
data class EstadoLeituraSync(val resultado: ResultadoLeituraSync? = null, val codigoErro: String? = null, val statusHttp: Int? = null)

class Sincronizador(private val fonte: FonteLeituraSync, private val armazem: ArmazemLeituraSync,
    private val contextoAtual: () -> ContextoLeituraSync?) {
    private val mutex = Mutex()
    private val atual = MutableStateFlow(EstadoLeituraSync())
    val estado = atual.asStateFlow()

    suspend fun puxar(): ResultadoLeituraSync {
        if (!mutex.tryLock()) return ResultadoLeituraSync.EM_ANDAMENTO
        try {
            val contexto = contextoAtual() ?: return publicar(ResultadoLeituraSync.SEM_SESSAO)
            return puxarContexto(contexto)
        } finally { mutex.unlock() }
    }

    private suspend fun puxarContexto(contexto: ContextoLeituraSync): ResultadoLeituraSync {
        try {
            publicar(ResultadoLeituraSync.EM_ANDAMENTO)
            var cursor = armazem.cursor(contexto)?.takeIf { it.isNotBlank() }
            val vistos = mutableSetOf<String>().apply { cursor?.let(::add) }
            while (true) {
                if (contextoAtual() != contexto) return publicar(ResultadoLeituraSync.SESSAO_ALTERADA)
                val pagina = fonte.puxar(cursor)
                if (contextoAtual() != contexto) return publicar(ResultadoLeituraSync.SESSAO_ALTERADA)
                if (pagina.cursor.isBlank() || (pagina.temMais && pagina.cursor == cursor) ||
                    (pagina.cursor != cursor && !vistos.add(pagina.cursor))) throw RespostaInvalida()
                armazem.aplicar(contexto, pagina)
                if (contextoAtual() != contexto) return publicar(ResultadoLeituraSync.SESSAO_ALTERADA)
                cursor = pagina.cursor
                if (!pagina.temMais) return publicar(ResultadoLeituraSync.CONCLUIDO)
            }
        } catch (cancelamento: CancellationException) {
            publicar(ResultadoLeituraSync.CANCELADO)
            throw cancelamento
        } catch (falha: Exception) {
            if (contextoAtual() != contexto) return publicar(ResultadoLeituraSync.SESSAO_ALTERADA)
            return publicar(if (falha is FalhaComunicacao) ResultadoLeituraSync.SEM_REDE else ResultadoLeituraSync.FALHOU,
                (falha as? ErroApi)?.codigo, (falha as? ErroApi)?.status)
        }
    }

    private fun publicar(resultado: ResultadoLeituraSync, codigo: String? = null, status: Int? = null): ResultadoLeituraSync {
        atual.value = EstadoLeituraSync(resultado, codigo, status)
        return resultado
    }
}
