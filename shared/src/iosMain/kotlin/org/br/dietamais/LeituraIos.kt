package org.br.dietamais

import kotlinx.coroutines.*

class CancelamentoLeituraIos internal constructor(private val tarefa: Job) {
    fun cancelar() = tarefa.cancel()
}

fun executarLeituraIos(aoConcluir: (String) -> Unit): CancelamentoLeituraIos {
    val escopo = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val tarefa = escopo.launch {
        try {
            val resultado = AmbienteIos.ambiente.sincronizarAgora()
            withContext(Dispatchers.Main) { aoConcluir(resultado.name) }
        } catch (cancelamento: CancellationException) { throw cancelamento }
        catch (_: Exception) { withContext(Dispatchers.Main) { aoConcluir("FALHOU") } }
        finally { escopo.cancel() }
    }
    return CancelamentoLeituraIos(tarefa)
}
