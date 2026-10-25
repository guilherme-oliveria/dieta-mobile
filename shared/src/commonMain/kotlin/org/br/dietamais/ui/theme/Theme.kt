package org.br.dietamais.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

internal val CoresClaras = lightColorScheme(
    primary = VerdeDieta,
    onPrimary = SuperficieDieta,
    primaryContainer = VerdeDietaClaro,
    onPrimaryContainer = VerdeDietaEscuro,
    secondary = VerdeDietaEscuro,
    onSecondary = SuperficieDieta,
    secondaryContainer = SuperficieSuave,
    onSecondaryContainer = TextoDieta,
    tertiary = InfoDieta,
    onTertiary = SuperficieDieta,
    tertiaryContainer = InfoClaro,
    onTertiaryContainer = InfoDieta,
    background = FundoDieta,
    onBackground = TextoDieta,
    surface = SuperficieDieta,
    onSurface = TextoDieta,
    onSurfaceVariant = TextoSecundarioDieta,
    surfaceVariant = SuperficieSuave,
    surfaceContainer = SuperficieSuave,
    surfaceContainerLow = SuperficieDieta,
    surfaceContainerHigh = SuperficieSuave,
    surfaceContainerHighest = SuperficieSuave,
    surfaceContainerLowest = SuperficieDieta,
    outline = BordaCampo,
    outlineVariant = BordaDieta,
    error = AlertaDieta,
    onError = SuperficieDieta,
    errorContainer = AlertaClaro,
    onErrorContainer = AlertaTexto,
)

// Tema claro do MVP, compartilhado entre Android, iOS e desktop.
@Composable
fun DietaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CoresClaras,
        typography = tipografiaDieta(),
        shapes = FormasDieta.material,
        content = content,
    )
}
