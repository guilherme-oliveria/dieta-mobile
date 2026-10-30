package org.br.dietamais.plataforma

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

/** Seletor de documentos do Android (sem permissão de armazenamento). Lê no máximo 1 MB: acima do limite da logo o ViewModel já recusa. */
@Composable
actual fun rememberSeletorImagem(aoEscolher: (ImagemEscolhida) -> Unit): (() -> Unit)? {
    val contexto = LocalContext.current
    val escolher by rememberUpdatedState(aoEscolher)
    val abrir = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val bytes = try { contexto.contentResolver.openInputStream(uri)?.use { it.readNBytes(LIMITE_LEITURA) } } catch (_: Exception) { null }
            if (bytes != null) escolher(ImagemEscolhida(bytes))
        }
    }
    return { abrir.launch("image/*") }
}

private const val LIMITE_LEITURA = 1024 * 1024
