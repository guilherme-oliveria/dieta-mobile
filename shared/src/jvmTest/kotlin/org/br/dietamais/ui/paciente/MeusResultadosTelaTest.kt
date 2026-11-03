package org.br.dietamais.ui.paciente

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import org.br.dietamais.dominio.avaliacao.*
import org.br.dietamais.ui.paciente.evolucao.MeusResultadosTela
import org.br.dietamais.ui.theme.DietaTheme
import kotlin.test.Test
import kotlin.time.Instant

@OptIn(ExperimentalTestApi::class)
class MeusResultadosTelaTest {
    private val m = MeusResultados(Instant.parse("2026-10-01T12:00:00Z"), 100.0, 34.6,
        listOf(Indicador(TipoIndicador.PESO_ALTURA, Semaforo.ALERTA, imc = org.br.dietamais.data.remoto.dto.ClassificacaoImc.OBESIDADE_I, valor = 34.6)),
        listOf(ParteCorpo.GORDURA to 24.0, ParteCorpo.MASSA_MAGRA to 76.0), 24.0, MetaPeso(18.0, 91.9, 8.1), 60.0..80.0, "Bom caminho",
        listOf(0.0 to 95.0, 30.0 to 100.0))

    @Test fun mostraSemaforoComTextoMetaEResumo() = runSkikoComposeUiTest(size = Size(390f, 1600f)) {
        setContent { DietaTheme { MeusResultadosTela(m) } }
        onNodeWithText("Vale conversar com sua nutri").assertExists()
        onNodeWithText("Peso para a altura").assertExists()
        onNodeWithText("Peso para 18% de gordura: 91,9 kg").assertExists()
        onNodeWithText("Faltam 8,1 kg para chegar lá.").assertExists()
        onNodeWithText("Bom caminho").assertExists()
    }

    @Test fun semAvaliacaoMostraEstadoVazio() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        setContent { DietaTheme { MeusResultadosTela(null) } }
        onNodeWithText("Sua nutri ainda não compartilhou uma avaliação com você.").assertExists()
    }
}
