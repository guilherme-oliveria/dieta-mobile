package org.br.dietamais.data.sync

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.serialization.json.encodeToJsonElement
import org.br.dietamais.data.remoto.ApiDieta
import org.br.dietamais.data.remoto.JsonContrato
import org.br.dietamais.data.remoto.dto.DispositivoResposta
import org.br.dietamais.data.remoto.dto.Plataforma
import org.br.dietamais.data.remoto.dto.RegistrarDispositivoRequisicao
import kotlin.time.Duration.Companion.seconds

fun interface PortaDispositivosPush { suspend fun registrar(token: String) }

class DispositivosPushHttp(private val api: ApiDieta, private val plataforma: Plataforma) : PortaDispositivosPush {
    override suspend fun registrar(token: String) {
        api.enviar<DispositivoResposta>("eu/dispositivos", JsonContrato.encodeToJsonElement(RegistrarDispositivoRequisicao(token, plataforma)))
    }
}

/**
 * Registra o token de push no servidor assim que há sessão, token e rede, e de novo se qualquer um mudar (nova conta no mesmo
 * aparelho, token renovado). O servidor é idempotente por token. Falha tenta de novo em [espera]; desktop nunca registra.
 */
class RegistradorPush(private val porta: PortaDispositivosPush, escopo: CoroutineScope, contexto: Flow<ContextoLeituraSync?>,
    token: Flow<String?>, online: Flow<Boolean>, private val espera: kotlin.time.Duration = 60.seconds, ativo: Boolean = true) {
    private var registrado: Triple<Any, Any, String>? = null

    init {
        if (ativo) escopo.launch {
            combine(contexto, token, online) { c, t, o -> Triple(c, t, o) }.collectLatest { (c, t, o) ->
                if (c == null || t == null || !o) return@collectLatest
                val chave = Triple<Any, Any, String>(c.usuarioId, c.perfil, t)
                while (registrado != chave) {
                    try { porta.registrar(t); registrado = chave }
                    catch (cancelamento: CancellationException) { throw cancelamento }
                    catch (_: Exception) { delay(espera) }
                }
            }
        }
    }
}
