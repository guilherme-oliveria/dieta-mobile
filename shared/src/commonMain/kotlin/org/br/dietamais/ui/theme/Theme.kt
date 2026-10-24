package org.br.dietamais.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CoresClaras = lightColorScheme(
    primary = VerdeDieta,
    onPrimary = Color.White,
    primaryContainer = VerdeDietaClaro,
    onPrimaryContainer = TextoDieta,
    background = FundoDieta,
    onBackground = TextoDieta,
    surface = SuperficieDieta,
    onSurface = TextoDieta,
    onSurfaceVariant = TextoSecundarioDieta,
    error = AlertaDieta,
    onError = Color.White,
)

// Tema único do app (Android, iOS e desktop). A fonte Plus Jakarta Sans entra quando as telas forem montadas.
@Composable
fun DietaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CoresClaras,
        content = content,
    )
}
