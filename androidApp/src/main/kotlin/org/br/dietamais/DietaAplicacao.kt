package org.br.dietamais

import android.app.Application
import org.br.dietamais.app.criarAmbienteDieta
import org.br.dietamais.plataforma.*

class DietaAplicacao : Application(), ProvedorAmbienteDieta {
    override val ambiente by lazy {
        val urlDev = getString(R.string.url_api_dev).trim()
        val convites = setOf(getString(R.string.convite_host)).filterNot { it == "nao-configurado.invalid" }.toSet()
        val config = if (urlDev.isEmpty()) Config(DOMINIOS_CONVITE = convites) else Config(URL_API = urlDev, DOMINIOS_CONVITE = convites)
        val versao = packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0"
        criarAmbienteDieta(config, FabricaDriver(this), ArmazenamentoSeguro(this), versao, Conectividade(this))
    }
    override fun onCreate() {
        super.onCreate()
        agendarLeituraPeriodica(this)
        FirebasePush.iniciar(this)
    }
}
