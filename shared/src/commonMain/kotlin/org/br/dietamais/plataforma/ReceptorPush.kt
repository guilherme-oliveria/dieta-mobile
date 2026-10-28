package org.br.dietamais.plataforma

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.br.dietamais.app.SolicitacoesNavegacao

/**
 * Ponte entre o FCM (Android) / APNs (iOS) e o código comum. A plataforma só chama estas funções; o desktop não as chama
 * (sem push). O token não é secreto, mas também não vai para log.
 */
object ReceptorPush {
    private val atual = MutableStateFlow<String?>(null)
    private val avisos = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val token: StateFlow<String?> = atual.asStateFlow()
    /** Uma emissão por push recebido: o ambiente responde com um sync. */
    val chegadas: SharedFlow<Unit> = avisos.asSharedFlow()

    fun novoToken(token: String) { if (token.isNotBlank() && token.length <= 4096) atual.value = token }
    /** Push recebido (app aberto ou não); [rota] só é usada se a pessoa tocou na notificação. */
    fun chegou() { avisos.tryEmit(Unit) }
    fun tocou(rota: String?) { chegou(); rota?.let(SolicitacoesNavegacao::pedir) }
}
