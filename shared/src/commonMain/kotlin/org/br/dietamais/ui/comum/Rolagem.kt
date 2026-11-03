package org.br.dietamais.ui.comum

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Rola na vertical só quando [rolavel]. Telas de aba que também são hospedadas dentro de algo que já rola (a ficha do paciente) devem
 * receber `rolavel = false`: rolagem dentro de rolagem faz o Compose lançar "measured with an infinity maximum height constraints" e o app fecha.
 */
@Composable
fun Modifier.rolavelSe(rolavel: Boolean): Modifier {
    val estado = rememberScrollState()
    return if (rolavel) verticalScroll(estado) else this
}
