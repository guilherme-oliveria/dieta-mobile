package org.br.dietamais.ui.nutri

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import kotlinx.datetime.LocalDate
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.dominio.avaliacao.*
import org.br.dietamais.ui.nutri.avaliacao.*
import org.br.dietamais.ui.paciente.evolucao.MeusResultadosTela
import org.br.dietamais.ui.theme.DietaTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.time.Instant
import kotlin.uuid.Uuid

@OptIn(ExperimentalTestApi::class)
class AvaliacaoInfantilEvidenciasTest {
    private val data = Instant.parse("2026-11-01T12:00:00Z")
    private fun resultados(adolescente: Boolean) = ResultadosInfantisAvaliacaoDto(idadeMeses = if (adolescente) 156 else 48,
        idadeTexto = if (adolescente) "13 anos" else "4 anos", sexoReferencia = "masculino", modoCurva = "PERCENTIL",
        curvas = listOf(CurvaCrescimentoAvaliacaoDto(indicador = "IMC_IDADE", classificacao = if (adolescente) "NO" else "A",
            percentil = IntervaloCurvaDto(50.0, 85.0), escoreZ = IntervaloCurvaDto(0.0, 1.0), fonte = "OMS 2006/2007", versaoReferencia = "oms-1.0.0")),
        frisancho = if (adolescente) null else listOf(FrisanchoAvaliacaoDto(indicador = "CB", valor = 16.0, unidade = "cm", adequacaoPercentual = 100.0, classificacao = "A"),
            FrisanchoAvaliacaoDto(indicador = "AMBC", valor = 1300.0, unidade = "mm²", p5 = 1297.0, p10 = 1264.0, motivo = "REFERENCIA_INCONSISTENTE_G16")),
        composicao = if (!adolescente) null else ComposicaoInfantilDto(17.6, "ADEQ", 82.4, 5.28, 24.72),
        energia = EnergiaInfantilDto(gebIomKcal = 1042.4, getIomKcal = 1965.46, getRdiKcal = 1678.68))

    @Test fun telasEmTresTamanhosEFonteDuzentos() {
        listOf(Triple(390, 844, 1f), Triple(820, 1180, 1f), Triple(1180, 820, 1f), Triple(390, 844, 2f)).forEach { (w, h, fonte) ->
            listOf("nova", "g16", "adolescente", "paciente").forEach { tipo ->
                runSkikoComposeUiTest(size = Size(w.toFloat(), h.toFloat()), density = Density(1f, fonte)) {
                    val adolescente = tipo == "adolescente" || tipo == "paciente"
                    val r = resultados(adolescente)
                    val form = FormularioAvaliacao(dataHora = data, infantilAtiva = true, idadeMeses = if (adolescente) 156 else 48,
                        passo = if (tipo == "adolescente") PassoAvaliacao.COMPOSICAO else PassoAvaliacao.MEDIDAS,
                        valores = mapOf(MedidaAvaliacao.PESO to "30", MedidaAvaliacao.ESTATURA to "130", MedidaAvaliacao.BRACO to "16", MedidaAvaliacao.DOBRA_TRICEPS to "10", MedidaAvaliacao.DOBRA_SUBESCAPULAR to "10"),
                        infantil = FormularioInfantil("3,301", "PERCENTIL", "BRANCA", "1,2"), resultadosInfantis = if (tipo == "nova") null else r)
                    val a = AvaliacaoResposta(Uuid.random(), Uuid.random(), Uuid.random(), data, 30.0, 1.3, 17.75, ClassificacaoImc.ABAIXO_DO_PESO,
                        versao = 1, criadoEm = data, atualizadoEm = data, faixaEtaria = FaixaEtariaAvaliacao.ADOLESCENTE, resultadosInfantis = r)
                    setContent { DietaTheme { Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        if (tipo == "paciente") MeusResultadosTela(meusResultados(listOf(a)))
                        else AvaliacaoTela(EstadoAvaliacoes(formulario = form, sexo = Sexo.MASCULINO, nascimento = if (adolescente) LocalDate(2013,11,1) else LocalDate(2022,11,1)), AcoesAvaliacao())
                    } } }
                    when (tipo) {
                        "nova" -> onNodeWithContentDescription("Peso ao nascer (kg)").assertExists()
                        "g16" -> onNodeWithText("A referência de área muscular do braço aos 4 anos tem cortes inconsistentes. A classificação não está disponível.").performScrollTo().assertIsDisplayed()
                        "adolescente" -> onNodeWithText("Branca").performScrollTo().assertIsDisplayed()
                        else -> onNodeWithText("Percentil entre 50 e 85").performScrollTo().assertIsDisplayed()
                    }
                    waitForIdle()
                    val arquivo = File("build/reports/MOB-58/$tipo-$w-fonte-$fonte.png")
                    arquivo.parentFile.mkdirs(); ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", arquivo)
                    if (tipo != "paciente") {
                        onNodeWithText("Continuar").assertIsDisplayed().assertIsEnabled()
                        if (fonte == 2f) ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File("build/reports/MOB-58/$tipo-$w-fonte-$fonte-acoes.png"))
                    } else {
                        onNodeWithText("Energia diária estimada: 1678,68 kcal").performScrollTo().assertIsDisplayed()
                        ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File("build/reports/MOB-58/$tipo-$w-fonte-$fonte-energia.png"))
                        onNodeWithText("Escore Z", substring = true).assertDoesNotExist()
                    }
                }
            }
        }
    }
}
