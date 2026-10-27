package org.br.dietamais.plataforma

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.CancellationException
import org.br.dietamais.app.AmbienteDieta
import org.br.dietamais.data.sync.ResultadoLeituraSync
import java.util.concurrent.TimeUnit

interface ProvedorAmbienteDieta { val ambiente: AmbienteDieta }

class LeituraPeriodica(contexto: Context, parametros: WorkerParameters) : CoroutineWorker(contexto, parametros) {
    override suspend fun doWork(): Result {
        val ambiente = (applicationContext as? ProvedorAmbienteDieta)?.ambiente ?: return Result.failure()
        return try {
            when (ambiente.sincronizarAgora()) {
                ResultadoLeituraSync.CONCLUIDO, ResultadoLeituraSync.SEM_SESSAO, ResultadoLeituraSync.SESSAO_ALTERADA -> Result.success()
                ResultadoLeituraSync.SEM_REDE, ResultadoLeituraSync.EM_ANDAMENTO, ResultadoLeituraSync.CANCELADO -> Result.retry()
                ResultadoLeituraSync.FALHOU -> {
                    val status = ambiente.sincronizador.estado.value.statusHttp
                    if (status == 429 || (status != null && status >= 500)) Result.retry() else Result.failure()
                }
            }
        } catch (cancelamento: CancellationException) { throw cancelamento }
        catch (_: ArmazenamentoSeguroIndisponivel) { Result.retry() }
        catch (_: Exception) { Result.failure() }
    }
}

fun agendarLeituraPeriodica(contexto: Context) {
    val trabalho = PeriodicWorkRequestBuilder<LeituraPeriodica>(15, TimeUnit.MINUTES)
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build()
    WorkManager.getInstance(contexto).enqueueUniquePeriodicWork("dietamais-sync-leitura", ExistingPeriodicWorkPolicy.KEEP, trabalho)
}
