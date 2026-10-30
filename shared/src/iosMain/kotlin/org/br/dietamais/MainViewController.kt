package org.br.dietamais

import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.runtime.remember
import org.br.dietamais.plataforma.FabricaDriver
import org.br.dietamais.plataforma.ArmazenamentoSeguro
import org.br.dietamais.plataforma.IdentidadeDispositivo
import org.br.dietamais.plataforma.Compartilhador
import org.br.dietamais.plataforma.Config
import org.br.dietamais.plataforma.NotificadorLocal
import org.br.dietamais.plataforma.Conectividade
import org.br.dietamais.app.criarAmbienteDieta
import platform.Foundation.NSBundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.br.dietamais.data.repositorio.RotinaRepositorio
import org.br.dietamais.data.sync.RegistradorAcaoLembrete
import org.br.dietamais.data.sync.ResultadoAcaoLembrete
import org.br.dietamais.plataforma.ReceptorAcoesLembrete

fun MainViewController() = ComposeUIViewController {
    val identidade = remember { IdentidadeDispositivo() }
    val compartilhador = remember { Compartilhador() }
    val notificador = remember { NotificadorLocal() }
    val receptor = remember {
        ReceptorAcoesLembrete { acao, dieta, refeicao, dia ->
            val ambiente = AmbienteIos.ambiente
            CoroutineScope(Dispatchers.Default).launch {
                if (RegistradorAcaoLembrete(RotinaRepositorio(ambiente.banco)).registrar(dieta, refeicao, dia, acao) == ResultadoAcaoLembrete.GRAVADO) ambiente.solicitarLeitura()
            }
        }.also { it.instalar() }
    }
    App(ambiente = AmbienteIos.ambiente, identidadeDispositivo = identidade, compartilhamento = compartilhador, notificacao = notificador)
}

internal object AmbienteIos {
    val ambiente by lazy {
        val dominio = NSBundle.mainBundle.objectForInfoDictionaryKey("ConviteHost") as? String
        val convites = listOfNotNull(dominio).filter { it.isNotBlank() && it != "nao-configurado.invalid" }.toSet()
        // Host da API (sem esquema) vindo do xcconfig; vazio mantém o padrão de desenvolvimento (localhost).
        val hostApi = (NSBundle.mainBundle.objectForInfoDictionaryKey("DietaApiHost") as? String)?.trim().orEmpty()
        val config = if (hostApi.isEmpty() || hostApi.startsWith("$(")) Config(DOMINIOS_CONVITE = convites) else Config(URL_API = "https://$hostApi", DOMINIOS_CONVITE = convites)
        val versao = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "1.0"
        criarAmbienteDieta(config, FabricaDriver(), ArmazenamentoSeguro(), versao, Conectividade())
    }
}
