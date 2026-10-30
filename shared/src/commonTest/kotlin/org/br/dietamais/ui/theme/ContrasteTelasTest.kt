package org.br.dietamais.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.test.Test
import kotlin.test.assertTrue

/** WCAG 2.x: texto normal pede 4,5:1. Pares que o app realmente usa (ver `docs/design/componentes.md`). */
class ContrasteTelasTest {
    private fun razao(a: Color, b: Color): Double {
        val (claro, escuro) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (claro + 0.05) / (escuro + 0.05)
    }

    private fun exige(texto: Color, fundo: Color, nome: String, minimo: Double = 4.5) {
        val r = razao(texto, fundo)
        assertTrue(r >= minimo, "$nome: contraste ${"%.2f".format(r)}:1, o mínimo é $minimo:1")
    }

    @Test
    fun textoPrincipalESecundarioNosFundosDoApp() {
        listOf(FundoDieta to "fundo", SuperficieDieta to "card", SuperficieSuave to "superfície suave").forEach { (fundo, nome) ->
            exige(TextoDieta, fundo, "texto principal sobre $nome")
            exige(TextoSecundarioDieta, fundo, "texto secundário sobre $nome")
        }
        exige(TextoRiscado, FundoDieta, "texto riscado sobre fundo")
        exige(TextoRiscado, SuperficieDieta, "texto riscado sobre card")
    }

    @Test
    fun textoDeMarcaEDeBotao() {
        exige(SuperficieDieta, VerdeDieta, "texto branco sobre o botão principal")
        exige(VerdeDieta, FundoDieta, "link verde sobre o fundo")
        exige(VerdeDieta, SuperficieDieta, "link verde sobre o card")
        exige(VerdeDietaEscuro, VerdeDietaClaro, "texto do chip 'feita'")
        exige(TextoSecundarioDieta, VerdeDietaClaro, "texto de apoio sobre o fundo verde claro")
    }

    @Test
    fun alertaEInfo() {
        exige(AlertaTexto, AlertaClaro, "texto de alerta sobre o aviso")
        exige(AlertaTexto, FundoDieta, "texto de alerta sobre o fundo")
        exige(AlertaTexto, SuperficieDieta, "texto de alerta sobre o card")
        exige(InfoDieta, SuperficieDieta, "texto informativo sobre o card")
        exige(InfoDieta, InfoClaro, "texto informativo sobre o aviso")
        // AlertaDieta (#C2551F) dá 4,19:1 sobre o fundo: só vale para ícone, ponto e borda (3:1), nunca para texto.
        exige(AlertaDieta, SuperficieDieta, "ícone de alerta sobre o card", minimo = 3.0)
        exige(VerdeDieta, FundoDieta, "foco verde sobre o fundo", minimo = 3.0)
    }
}
