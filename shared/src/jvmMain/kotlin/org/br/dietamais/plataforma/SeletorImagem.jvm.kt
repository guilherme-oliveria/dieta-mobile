package org.br.dietamais.plataforma

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import java.awt.FileDialog
import java.awt.Frame

/** Diálogo de arquivo do sistema (AWT). Lê no máximo 1 MB: acima do limite da logo o ViewModel já recusa. */
@Composable
actual fun rememberSeletorImagem(aoEscolher: (ImagemEscolhida) -> Unit): (() -> Unit)? {
    val escolher by rememberUpdatedState(aoEscolher)
    return {
        val dialogo = FileDialog(null as Frame?, "Escolher imagem", FileDialog.LOAD).apply {
            setFilenameFilter { _, nome -> nome.lowercase().let { it.endsWith(".png") || it.endsWith(".jpg") || it.endsWith(".jpeg") } }
            isVisible = true
        }
        val arquivo = dialogo.file?.let { java.io.File(dialogo.directory, it) }
        val bytes = try { arquivo?.inputStream()?.use { it.readNBytes(1024 * 1024) } } catch (_: Exception) { null }
        if (bytes != null) escolher(ImagemEscolhida(bytes))
    }
}
