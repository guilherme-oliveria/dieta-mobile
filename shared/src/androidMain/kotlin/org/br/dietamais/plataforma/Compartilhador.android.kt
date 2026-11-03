package org.br.dietamais.plataforma

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class Compartilhador(private val contexto: Context) : Compartilhamento {
    actual override suspend fun compartilhar(texto: String) = withContext(Dispatchers.Main) {
        try {
            val enviar = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, texto) }
            contexto.startActivity(Intent.createChooser(enviar, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            ResultadoCompartilhamento.ABERTO
        } catch (_: Exception) { throw CompartilhamentoIndisponivel() }
    }
}
