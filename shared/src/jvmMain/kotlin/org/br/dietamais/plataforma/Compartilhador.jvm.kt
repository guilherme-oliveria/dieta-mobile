package org.br.dietamais.plataforma

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class Compartilhador : Compartilhamento {
    actual override suspend fun compartilhar(texto: String) = withContext(Dispatchers.IO) {
        try {
            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(texto), null)
            ResultadoCompartilhamento.COPIADO
        } catch (_: Exception) { throw CompartilhamentoIndisponivel() }
    }
}
