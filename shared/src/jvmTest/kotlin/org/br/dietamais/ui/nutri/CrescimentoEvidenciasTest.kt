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
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.ui.nutri.avaliacao.CrescimentoTela
import org.br.dietamais.ui.theme.DietaTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.time.Instant
import kotlin.time.Duration.Companion.days
import kotlin.uuid.Uuid

@OptIn(ExperimentalTestApi::class)
class CrescimentoEvidenciasTest {
    @Test fun graficosETabelasEmTresLargurasEFonteAmpliada() {
        val paciente=Uuid.random(); val vinculo=Uuid.random()
        val itens=(16..18).map { mes ->
            val data=Instant.parse("2026-01-01T12:00:00Z")+(mes*30).days
            AvaliacaoResposta(Uuid.random(),paciente,vinculo,data,10.0,0.8,15.6,ClassificacaoImc.NORMAL,
                versao=1,criadoEm=data,atualizadoEm=data,resultadosInfantis=ResultadosInfantisAvaliacaoDto(
                    idadeMeses=mes,sexoReferencia="feminino",curvas=listOf(CurvaCrescimentoAvaliacaoDto(
                        indicador="IMC_IDADE",valor=15.5+mes/100.0,unidade="kg/m²",idadeMesesReferencia=mes,
                        cortesPercentis=listOf(10.0,11.0,12.0,13.0,15.0,17.0,19.0,20.0).map { it+mes/100.0 },
                        percentil=IntervaloCurvaDto(50.0,85.0),fonte="OMS · fixture sintética",versaoReferencia="oms-1"))))
        }
        listOf(Triple(390,844,1f),Triple(820,1180,1f),Triple(1180,820,1f),Triple(390,844,2f))
            .flatMap { listOf(it to false,it to true) }.forEach { (tamanho,unica) ->
            val (w,h,fonte)=tamanho
            runSkikoComposeUiTest(size=Size(w.toFloat(),h.toFloat()),density=Density(1f,fonte)) {
                setContent { DietaTheme { Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) { CrescimentoTela(if(unica) itens.takeLast(1) else itens) } } }
                fun captura(sufixo:String) { val caso=if(unica) "unica-" else ""; val f=File("build/reports/MOB-59/curva-$w-$fonte-$caso$sufixo.png"); f.parentFile.mkdirs(); ImageIO.write(onRoot().captureToImage().toAwtImage(),"png",f) }
                waitForIdle(); captura("grafico")
                onNodeWithContentDescription("P3:",substring=true).assertExists()
                onNodeWithContentDescription("P50:",substring=true).assertExists()
                onNodeWithContentDescription("P97:",substring=true).assertExists()
                onNodeWithText("Percentil entre 50 e 85").performScrollTo().assertIsDisplayed(); captura("intervalo")
                onNodeWithText("Ver como tabela").performScrollTo().performClick()
                onNodeWithText("P50: 15,18 kg/m²",substring=true).performScrollTo().assertIsDisplayed(); captura("tabela")
            }
        }
    }
}
