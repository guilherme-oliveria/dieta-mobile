package org.br.dietamais.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.test.Test
import kotlin.test.assertTrue

class ContrasteTest {
    @Test
    fun textosDaBaseAtendemContrasteAA() {
        val pares = listOf(
            SuperficieDieta to VerdeDieta,
            TextoDieta to FundoDieta,
            TextoSecundarioDieta to FundoDieta,
            VerdeDietaEscuro to VerdeDietaClaro,
            AlertaTexto to AlertaClaro,
            AlertaTexto to SuperficieDieta,
            InfoDieta to InfoClaro,
            TextoRiscado to SuperficieDieta,
        )
        pares.forEach { (texto, fundo) ->
            assertTrue(contraste(texto, fundo) >= 4.5f, "Contraste abaixo de AA: $texto sobre $fundo")
        }
    }

    private fun contraste(primeira: Color, segunda: Color): Float {
        val maior = maxOf(primeira.luminance(), segunda.luminance())
        val menor = minOf(primeira.luminance(), segunda.luminance())
        return (maior + 0.05f) / (menor + 0.05f)
    }
}
