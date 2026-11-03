package org.br.dietamais.ui.nutri

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.dominio.avaliacao.*
import org.br.dietamais.ui.nutri.avaliacao.*
import org.br.dietamais.ui.paciente.evolucao.MeusResultadosTela
import org.br.dietamais.ui.theme.DietaTheme
import kotlin.test.Test
import kotlin.time.Instant
import kotlin.uuid.Uuid

@OptIn(ExperimentalTestApi::class)
class AvaliacaoInfantilTelaTest {
    @Test fun lactenteMostraComprimentoEPesoNascerSemProtocoloAdulto() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        val form = FormularioAvaliacao(infantilAtiva = true, dataHora = Instant.parse("2026-11-01T12:00:00Z"),
            valores = mapOf(MedidaAvaliacao.PESO to "3,25", MedidaAvaliacao.ESTATURA to "48,5"))
        setContent { DietaTheme { AvaliacaoTela(EstadoAvaliacoes(formulario = form, nascimento = kotlinx.datetime.LocalDate(2026, 9, 1)), AcoesAvaliacao()) } }
        onNodeWithContentDescription("Comprimento (cm)").assertExists()
        onNodeWithContentDescription("Peso ao nascer (kg)").assertExists()
        onNodeWithText("3 dobras").assertDoesNotExist()
        onNodeWithText("Continuar").performScrollTo().assertIsEnabled()
    }

    @Test fun necessidadesInfantisNaoOferecemMetasOuMetodosAdultos() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        val form = FormularioAvaliacao(infantilAtiva = true, passo = PassoAvaliacao.NECESSIDADES)
        setContent { DietaTheme { AvaliacaoTela(EstadoAvaliacoes(formulario = form), AcoesAvaliacao()) } }
        onNodeWithContentDescription("Fator de atividade").assertExists()
        onNodeWithText("FAO 2004").assertDoesNotExist()
        onNodeWithText("Faixa esportiva").assertDoesNotExist()
        onNodeWithText("Resultados disponíveis após sincronizar.").performScrollTo().assertExists()
    }

    @Test fun pacienteVeIntervaloConfirmadoSemReguaOuClassificacaoAdulta() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        val data = Instant.parse("2026-11-01T12:00:00Z")
        val a = AvaliacaoResposta(Uuid.random(), Uuid.random(), Uuid.random(), data, 10.0, 0.811, 15.2, ClassificacaoImc.ABAIXO_DO_PESO,
            versao = 1, criadoEm = data, atualizadoEm = data, faixaEtaria = FaixaEtariaAvaliacao.CRIANCA,
            resultadosInfantis = ResultadosInfantisAvaliacaoDto(idadeMeses = 12, idadeTexto = "1 ano", curvas = listOf(
                CurvaCrescimentoAvaliacaoDto(indicador = "IMC_IDADE", classificacao = "A", percentil = IntervaloCurvaDto(50.0, 85.0), escoreZ = IntervaloCurvaDto(0.0, 1.0)))))
        setContent { DietaTheme { MeusResultadosTela(meusResultados(listOf(a))) } }
        onNodeWithText("Percentil entre 50 e 85").performScrollTo().assertExists()
        onNodeWithText("Abaixo do peso", substring = true).assertDoesNotExist()
        onNodeWithText("Escore Z", substring = true).assertDoesNotExist()
    }

    @Test fun adolescenteSemNascimentoLocalMantemEscolhaDaFormulaAposAlteracao() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        val form = FormularioAvaliacao(infantilAtiva = true, idadeMeses = 156, passo = PassoAvaliacao.COMPOSICAO,
            valores = mapOf(MedidaAvaliacao.DOBRA_TRICEPS to "10", MedidaAvaliacao.DOBRA_SUBESCAPULAR to "10"))
        setContent { DietaTheme { AvaliacaoTela(EstadoAvaliacoes(formulario = form, sexo = Sexo.MASCULINO), AcoesAvaliacao()) } }
        onNodeWithText("Categoria da fórmula de Slaughter").assertExists()
        onNodeWithText("Branca").performScrollTo().assertExists()
    }

    @Test fun percentuaisTemUmSoSimboloSemEspacoAntesDele() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        val r = ResultadosInfantisAvaliacaoDto(idadeMeses = 48,
            frisancho = listOf(FrisanchoAvaliacaoDto(indicador = "CB", valor = 16.0, unidade = "cm", adequacaoPercentual = 100.0, classificacao = "A")),
            composicao = ComposicaoInfantilDto(percentualGordura = 17.6)) // fixture de apresentação, sem representar vetor clínico
        setContent { DietaTheme { PainelResultadosInfantis(r) } }
        onNodeWithText("Adequação à referência: 100%").assertExists()
        onNodeWithText("Gordura corporal: 17,6%").assertExists()
    }
}
