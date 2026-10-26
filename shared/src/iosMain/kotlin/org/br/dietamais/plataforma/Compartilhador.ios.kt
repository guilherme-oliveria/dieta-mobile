@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package org.br.dietamais.plataforma

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

actual class Compartilhador : Compartilhamento {
    actual override suspend fun compartilhar(texto: String) = withContext(Dispatchers.Main) {
        val raiz = UIApplication.sharedApplication.keyWindow?.rootViewController ?: throw CompartilhamentoIndisponivel()
        var apresentador = raiz
        while (apresentador.presentedViewController != null) apresentador = apresentador.presentedViewController!!
        val atividade = UIActivityViewController(activityItems = listOf(texto), applicationActivities = null)
        atividade.popoverPresentationController?.apply {
            sourceView = apresentador.view
            sourceRect = apresentador.view.bounds
        }
        apresentador.presentViewController(atividade, animated = true, completion = null)
        ResultadoCompartilhamento.ABERTO
    }
}
