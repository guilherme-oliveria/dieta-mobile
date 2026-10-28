package org.br.dietamais.ui.paciente

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import org.br.dietamais.dominio.avaliacao.*
import org.br.dietamais.ui.paciente.evolucao.*
import org.br.dietamais.ui.theme.DietaTheme
import kotlin.test.*
import kotlin.time.Instant

@OptIn(ExperimentalTestApi::class)
class EvolucaoTelaTest {
    private val agora = Instant.parse("2026-10-28T12:00:00Z")
    private val pontos = listOf(PontoSerie(Instant.parse("2026-09-01T12:00:00Z"), 72.0), PontoSerie(Instant.parse("2026-10-01T12:00:00Z"), 70.0))

    @Test fun mostraUltimoValorEVariacaoDoPeriodo() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        val estado = EstadoEvolucao(series = mapOf(SerieEvolucao.PESO to pontos), serie = SerieEvolucao.PESO, carregado = true)
        setContent { DietaTheme { EvolucaoTela(estado, AcoesEvolucao(), agora) } }
        onNodeWithText("Último valor: 70 kg").assertExists(); onNodeWithText("Variação no período: -2 kg").assertExists()
        onNodeWithText("Registrar peso").assertDoesNotExist()
    }

    @Test fun pacienteVeBotaoDeRegistrarPesoESemMedidasMostraVazio() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        setContent { DietaTheme { EvolucaoTela(EstadoEvolucao(carregado = true, podeRegistrarPeso = true), AcoesEvolucao(), agora) } }
        onNodeWithText("Registrar peso").assertExists(); onNodeWithText("Ainda não há medidas para mostrar.").assertExists()
    }

    @Test fun pontosForaDoIntervaloMostramMensagem() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        val estado = EstadoEvolucao(series = mapOf(SerieEvolucao.PESO to pontos), serie = SerieEvolucao.PESO, carregado = true, intervalo = IntervaloEvolucao.TRES_MESES)
        setContent { DietaTheme { EvolucaoTela(estado, AcoesEvolucao(), Instant.parse("2027-10-28T12:00:00Z")) } }
        onNodeWithText("Sem medidas neste período.").assertExists()
    }

    @Test fun normalizaPontosNoQuadroUnitario() {
        val n = normalizar(listOf(PontoSerie(Instant.fromEpochMilliseconds(0), 70.0), PontoSerie(Instant.fromEpochMilliseconds(100), 72.0)))
        assertEquals(listOf(0f to 0f, 1f to 1f), n)
        assertEquals(listOf(0.5f to 0.5f), normalizar(listOf(PontoSerie(Instant.fromEpochMilliseconds(0), 70.0))))
    }
}
