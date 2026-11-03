package org.br.dietamais

import android.Manifest
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CompletableDeferred
import org.br.dietamais.plataforma.NotificadorLocal
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import org.br.dietamais.plataforma.IdentidadeDispositivo
import org.br.dietamais.plataforma.Compartilhador
import org.br.dietamais.plataforma.ReceptorPush
import org.br.dietamais.plataforma.receberLinkConvite

class MainActivity : ComponentActivity() {
    private var respostaPermissao: CompletableDeferred<Boolean>? = null
    // registrado antes de STARTED, como o Android exige
    private val pedidoPermissao = registerForActivityResult(ActivityResultContracts.RequestPermission()) { respostaPermissao?.complete(it) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        intent?.dataString?.let(::receberLinkConvite)
        if (savedInstanceState == null) intent?.let(::receberToqueNotificacao)

        val notificador = NotificadorLocal(this) {
            CompletableDeferred<Boolean>().also { respostaPermissao = it; pedidoPermissao.launch(Manifest.permission.POST_NOTIFICATIONS) }.await()
        }

        setContent {
            val identidade = remember { IdentidadeDispositivo(applicationContext) }
            val compartilhador = remember { Compartilhador(this@MainActivity) }
            val ambiente = remember { (application as DietaAplicacao).ambiente }
            App(ambiente = ambiente, identidadeDispositivo = identidade, compartilhamento = compartilhador, notificacao = notificador)
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.dataString?.let(::receberLinkConvite)
        receberToqueNotificacao(intent)
    }
}

/** O FCM entrega o `data` do push como extras da intent quando a pessoa toca na notificação com o app fora da tela. */
private fun receberToqueNotificacao(intent: Intent) {
    val extras = intent.extras ?: return
    if (extras.getString("sync") == null) return // só pushes do servidor trazem `sync`
    ReceptorPush.tocou(extras.getString("rota"))
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
