package org.br.dietamais.app

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.br.dietamais.data.remoto.ApiDieta
import org.br.dietamais.data.remoto.ArmazemSessaoMemoria
import org.br.dietamais.plataforma.CofreSessao
import org.br.dietamais.plataforma.Config
import org.br.dietamais.plataforma.ObservadorConectividade
import org.br.dietamais.plataforma.plataformaApi

/** Ambiente da web: só a API e a conectividade. Sem banco local, fila nem sync; o Postgres via API é a única fonte de verdade. */
class AmbienteWeb(val config: Config, val api: ApiDieta, val conectividade: ObservadorConectividade) : AutoCloseable {
    /** Trabalho em segundo plano das Portas (ex.: envio dos registros do paciente); acaba junto com o ambiente. */
    val escopo = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun close() {
        escopo.cancel()
        CoroutineScope(Dispatchers.Default).launch {
            api.aguardarOperacoesSessao()
            api.close()
            conectividade.close()
        }
    }
}

fun criarAmbienteWeb(config: Config, cofre: CofreSessao, versaoApp: String, conectividade: ObservadorConectividade): AmbienteWeb {
    val api = ApiDieta(config, versaoApp, plataformaApi(), cofre, ArmazemSessaoMemoria)
    return AmbienteWeb(config, api, conectividade)
}
