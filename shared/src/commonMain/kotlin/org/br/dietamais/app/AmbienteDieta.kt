package org.br.dietamais.app

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.br.dietamais.data.local.BancoLocal
import org.br.dietamais.data.remoto.ApiDieta
import org.br.dietamais.data.sync.*
import org.br.dietamais.plataforma.*

class AmbienteDieta(val config: Config, val bancoLocal: BancoLocal, val api: ApiDieta,
    val conectividade: ObservadorConectividade, dispatcher: CoroutineDispatcher = Dispatchers.Default) : AutoCloseable {
    val banco get() = bancoLocal.banco
    private val trabalho = SupervisorJob()
    private val escopo = CoroutineScope(trabalho + dispatcher)
    private val fechado = MutableStateFlow(false)
    private val encerramento = Mutex()
    private var encerrado = false
    private val contextoAtual = { if (fechado.value) null else api.contextoLeituraSync() }
    val sincronizador = Sincronizador(FonteLeituraHttp(api), ArmazemLeituraSql(banco, contextoAtual), contextoAtual)
    private val contexto = api.operacaoSessao.map { contextoAtual() }.stateIn(escopo, SharingStarted.Eagerly, contextoAtual())
    private val enviador = EnviadorFila(FonteEnvioHttp(api), banco, contextoAtual)
    private val coordenador = CoordenadorLeitura(escopo, contexto, conectividade.estado, { sincronizarFila() })
    private var falhasEnvio = 0

    /** Envia a fila e depois puxa as mudanças. Se o envio falhar, tenta de novo com backoff (30 s, 60 s, ... até 15 min). */
    private suspend fun sincronizarFila() {
        val envio = enviador.enviar()
        if (envio == ResultadoEnvio.SEM_REDE || envio == ResultadoEnvio.FALHOU) {
            falhasEnvio++
            val espera = minOf(30L shl minOf(falhasEnvio - 1, 5), 900L)
            escopo.launch { delay(espera * 1000); coordenador.solicitar() }
        } else falhasEnvio = 0
        sincronizador.puxar()
    }

    init {
        escopo.launch {
            try { api.restaurarSessao() }
            catch (cancelamento: CancellationException) { throw cancelamento }
            catch (_: Exception) { /* A tela de autenticação/Worker apresenta a falha do cofre ao restaurar. */ }
        }
    }
    fun primeiroPlano(ativo: Boolean) = coordenador.primeiroPlano(ativo)
    fun solicitarLeitura() = coordenador.solicitar()

    suspend fun sincronizarAgora(): ResultadoLeituraSync {
        if (fechado.value) return ResultadoLeituraSync.SEM_SESSAO
        api.restaurarSessao()
        val resultado = sincronizador.puxar()
        return if (resultado == ResultadoLeituraSync.EM_ANDAMENTO)
            sincronizador.estado.first { it.resultado != ResultadoLeituraSync.EM_ANDAMENTO }.resultado ?: ResultadoLeituraSync.FALHOU
        else resultado
    }

    suspend fun encerrar() = encerramento.withLock {
        if (!encerrado) {
            fechado.value = true
            coordenador.close()
            trabalho.cancelAndJoin()
            api.aguardarOperacoesSessao()
            api.close()
            conectividade.close()
            bancoLocal.close()
            encerrado = true
        }
    }
    override fun close() { CoroutineScope(Dispatchers.Default).launch { encerrar() } }
}

fun criarAmbienteDieta(config: Config, fabrica: FabricaDriver, cofre: CofreSessao, versaoApp: String,
    conectividade: ObservadorConectividade): AmbienteDieta {
    val local = BancoLocal(fabrica.criar())
    return try { AmbienteDieta(config, local, ApiDieta(config, versaoApp, plataformaApi(), cofre, local.banco), conectividade) }
    catch (falha: Exception) { local.close(); conectividade.close(); throw falha }
}
