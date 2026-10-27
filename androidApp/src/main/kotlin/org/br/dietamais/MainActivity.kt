package org.br.dietamais

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import org.br.dietamais.plataforma.IdentidadeDispositivo
import org.br.dietamais.plataforma.Compartilhador
import org.br.dietamais.plataforma.receberLinkConvite

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        intent?.dataString?.let(::receberLinkConvite)

        setContent {
            val identidade = remember { IdentidadeDispositivo(applicationContext) }
            val compartilhador = remember { Compartilhador(this@MainActivity) }
            val ambiente = remember { (application as DietaAplicacao).ambiente }
            App(ambiente = ambiente, identidadeDispositivo = identidade, compartilhamento = compartilhador)
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
