package org.br.dietamais

import android.app.Application
import org.br.dietamais.app.criarAmbienteDieta
import org.br.dietamais.plataforma.*

class DietaAplicacao : Application(), ProvedorAmbienteDieta {
    override val ambiente by lazy {
        val config = Config(DOMINIOS_CONVITE = setOf(getString(R.string.convite_host)).filterNot { it == "nao-configurado.invalid" }.toSet())
        val versao = packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0"
        criarAmbienteDieta(config, FabricaDriver(this), ArmazenamentoSeguro(this), versao, Conectividade(this))
    }
    override fun onCreate() {
        super.onCreate()
        agendarLeituraPeriodica(this)
    }
}
