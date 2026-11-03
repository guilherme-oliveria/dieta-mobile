package org.br.dietamais.data.sync

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.br.dietamais.plataforma.EstadoConexao
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class CoordenadorLeitura(escopo: CoroutineScope, private val sessao: StateFlow<ContextoLeituraSync?>,
    private val conexao: StateFlow<EstadoConexao>, private val executar: suspend () -> Unit,
    intervalo: Duration = 15.minutes) : AutoCloseable {
    private val escopo = CoroutineScope(escopo.coroutineContext + SupervisorJob(escopo.coroutineContext[Job]))
    private val primeiroPlano = MutableStateFlow(false)
    private val pedidos = Channel<Unit>(Channel.CONFLATED)
    private var rodando: Job? = null

    init {
        require(intervalo.isPositive())
        this.escopo.launch {
            for (pedido in pedidos) if (primeiroPlano.value && sessao.value != null) {
                val tarefa = launch { executar() }
                rodando = tarefa
                tarefa.join()
                rodando = null
            }
        }
        this.escopo.launch {
            var anterior = sessao.value
            combine(sessao, primeiroPlano) { contexto, ativo -> contexto to ativo }.collect { (contexto, ativo) ->
                if (contexto != anterior || !ativo) rodando?.cancel()
                anterior = contexto
                if (ativo && contexto != null) solicitar()
            }
        }
        val redeInicial = conexao.value
        this.escopo.launch {
            var anterior = redeInicial
            conexao.collect { rede ->
                if (rede == EstadoConexao.DISPONIVEL && anterior != rede && primeiroPlano.value) solicitar()
                anterior = rede
            }
        }
        this.escopo.launch {
            while (isActive) {
                delay(intervalo)
                if (primeiroPlano.value && sessao.value != null && conexao.value != EstadoConexao.INDISPONIVEL) solicitar()
            }
        }
    }
    fun primeiroPlano(ativo: Boolean) { primeiroPlano.value = ativo }
    fun solicitar() { pedidos.trySend(Unit) }
    override fun close() { pedidos.close(); escopo.cancel() }
}
