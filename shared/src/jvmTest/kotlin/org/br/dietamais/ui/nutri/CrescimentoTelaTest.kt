package org.br.dietamais.ui.nutri

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.ui.nutri.avaliacao.CrescimentoTela
import org.br.dietamais.ui.theme.DietaTheme
import kotlin.test.Test
import kotlin.time.Instant
import kotlin.uuid.Uuid

@OptIn(ExperimentalTestApi::class)
class CrescimentoTelaTest {
    private val data = Instant.parse("2026-11-01T12:00:00Z")
    private fun avaliacao(): AvaliacaoResposta {
        val id = Uuid.random()
        return AvaliacaoResposta(id,id,id,data,10.0,0.8,15.625,ClassificacaoImc.OBESIDADE_III,
            versao=1,criadoEm=data,atualizadoEm=data,resultadosInfantis=ResultadosInfantisAvaliacaoDto(
                idadeMeses=18,sexoReferencia="feminino",curvas=listOf(CurvaCrescimentoAvaliacaoDto(
                    indicador="IMC_IDADE",valor=15.625,unidade="kg/m²",idadeMesesReferencia=18,
                    cortesPercentis=listOf(10.0,11.0,12.0,13.0,15.0,17.0,19.0,20.0),
                    percentil=IntervaloCurvaDto(50.0,85.0),fonte="OMS",versaoReferencia="oms-1"))))
    }

    @Test fun mostraIntervaloConfirmadoETabelaSemClassificacaoAdulta() = runSkikoComposeUiTest(size=Size(390f,844f)) {
        setContent { DietaTheme { CrescimentoTela(listOf(avaliacao())) } }
        onNodeWithText("Percentil entre 50 e 85").performScrollTo().assertExists()
        onNodeWithText("Obesidade III",substring=true).assertDoesNotExist()
        onNodeWithText("Ver como tabela").performScrollTo().performClick()
        onNodeWithText("P50: 15 kg/m²",substring=true).performScrollTo().assertExists()
        onAllNodesWithText("18 meses").assertCountEquals(8)
    }

    @Test fun semSnapshotNaoUsaMedidasAdultasComoCurva() = runSkikoComposeUiTest(size=Size(390f,844f)) {
        setContent { DietaTheme { CrescimentoTela(listOf(avaliacao().copy(resultadosInfantis=null))) } }
        onNodeWithText("Ainda não há resultados de crescimento confirmados.").assertExists()
        onNodeWithText("Ver como tabela").assertDoesNotExist()
    }

    @Test fun historicoAbreEFechaCrescimentoOffline() = runSkikoComposeUiTest(size=Size(390f,844f)) {
        val a=avaliacao()
        setContent { DietaTheme { org.br.dietamais.ui.nutri.avaliacao.HistoricoAvaliacoesTela(
            org.br.dietamais.ui.nutri.avaliacao.EstadoAvaliacoes(itens=listOf(a),semConexao=true),
            org.br.dietamais.ui.nutri.avaliacao.AcoesHistorico()) } }
        onNodeWithText("Ver crescimento").performClick()
        onNodeWithText("Percentil entre 50 e 85").performScrollTo().assertExists()
        onNodeWithText("Voltar às avaliações").performScrollTo().performClick()
        onNodeWithText("Ver crescimento").assertExists()
    }

    @Test fun umaReferenciaIdentificaP3P50P97NoGrafico() = runSkikoComposeUiTest(size=Size(390f,844f)) {
        val a=avaliacao()
        setContent { DietaTheme { CrescimentoTela(listOf(a)) } }
        onNodeWithContentDescription("P3: 11 kg/m²",substring=true).assertExists()
        onNodeWithContentDescription("P50: 15 kg/m²",substring=true).assertExists()
        onNodeWithContentDescription("P97: 19 kg/m²",substring=true).assertExists()
    }
}
