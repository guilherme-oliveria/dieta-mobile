package org.br.dietamais

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import org.br.dietamais.plataforma.FabricaDriver
import org.br.dietamais.plataforma.ArmazenamentoSeguro

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val fabrica = remember { FabricaDriver(applicationContext) }
            val cofre = remember { ArmazenamentoSeguro(applicationContext) }
            val versao = remember { packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0" }
            App(fabricaDriver = fabrica, cofreSessao = cofre, versaoApp = versao)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
