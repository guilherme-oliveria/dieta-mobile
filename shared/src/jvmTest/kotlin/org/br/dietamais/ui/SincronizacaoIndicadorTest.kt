package org.br.dietamais.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import org.br.dietamais.data.sync.*
import org.br.dietamais.plataforma.EstadoConexao
import org.br.dietamais.ui.comum.IndicadorLeitura
import org.br.dietamais.ui.theme.DietaTheme
import org.br.dietamais.ui.theme.Dimens
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class SincronizacaoIndicadorTest {
    @Test fun offlineMostraCacheEAtualizacaoEmVooNaoDuplicaAcao() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        setContent { DietaTheme { IndicadorLeitura(EstadoLeituraSync(ResultadoLeituraSync.EM_ANDAMENTO), EstadoConexao.INDISPONIVEL, {}) } }
        onNodeWithText("Sem internet. Mostrando o que está salvo.").assertIsDisplayed()
        onNodeWithText("Atualizar dados").assertIsNotEnabled()
        onNodeWithText("Sincronizando dados…").assertIsDisplayed()
    }
    @Test fun taxaUsaCodigoEPermiteNovaTentativaExplicita() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        var atualizacoes = 0
        setContent { DietaTheme { IndicadorLeitura(EstadoLeituraSync(ResultadoLeituraSync.FALHOU, "TAXA_EXCEDIDA", 429), EstadoConexao.DISPONIVEL, { atualizacoes++ }) } }
        onNodeWithText("Muitas solicitações. Aguarde um pouco antes de tentar novamente.").assertIsDisplayed()
        onNodeWithText("Atualizar dados").performClick(); assertEquals(1, atualizacoes)
    }
    @Test fun indicadorNosTresTamanhosEFonteAmpliada() {
        listOf(390 to 844, 820 to 1180, 1180 to 820).forEach { (w, h) -> capturar(w, h, 1f) }
        capturar(390, 844, 2f)
    }
    private fun capturar(largura: Int, altura: Int, fonte: Float) = runSkikoComposeUiTest(size = Size(largura.toFloat(), altura.toFloat()), density = Density(1f, fonte)) {
        setContent { DietaTheme { Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().padding(Dimens.margemCompacta)) {
                IndicadorLeitura(EstadoLeituraSync(ResultadoLeituraSync.SEM_REDE), EstadoConexao.INDISPONIVEL, {})
            }
        } } }
        val acao = onNodeWithText("Atualizar dados").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertTrue(acao.left >= 0 && acao.right <= largura && acao.bottom <= altura)
        val arquivo = File("build/reports/sincronizacao/offline-$largura-fonte-$fonte.png")
        arquivo.parentFile.mkdirs(); ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", arquivo)
    }
}
