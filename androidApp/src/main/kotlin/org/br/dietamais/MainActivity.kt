package org.br.dietamais

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import org.br.dietamais.plataforma.FabricaDriver
import org.br.dietamais.plataforma.ArmazenamentoSeguro
import org.br.dietamais.plataforma.IdentidadeDispositivo
import org.br.dietamais.plataforma.Compartilhador
import org.br.dietamais.plataforma.Config
import org.br.dietamais.plataforma.receberLinkConvite

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        intent?.dataString?.let(::receberLinkConvite)

        setContent {
            val fabrica = remember { FabricaDriver(applicationContext) }
            val cofre = remember { ArmazenamentoSeguro(applicationContext) }
            val identidade = remember { IdentidadeDispositivo(applicationContext) }
            val compartilhador = remember { Compartilhador(this@MainActivity) }
            val config = remember { Config(DOMINIOS_CONVITE = setOf(getString(R.string.convite_host)).filterNot { it == "nao-configurado.invalid" }.toSet()) }
            val versao = remember { packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0" }
            App(config = config, fabricaDriver = fabrica, cofreSessao = cofre, versaoApp = versao, identidadeDispositivo = identidade, compartilhamento = compartilhador)
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.dataString?.let(::receberLinkConvite)
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
