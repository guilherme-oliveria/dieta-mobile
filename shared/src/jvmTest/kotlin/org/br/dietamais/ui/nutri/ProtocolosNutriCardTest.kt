package org.br.dietamais.ui.nutri

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import org.br.dietamais.dominio.calculos.ProtocoloDobras
import org.br.dietamais.ui.nutri.protocolos.*
import org.br.dietamais.ui.theme.DietaTheme
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class ProtocolosNutriCardTest {
    @Test fun mostraAsEscolhasEOBotaoSoHabilitaQuandoMuda() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        var dobras: ProtocoloDobras? = null
        var estado = EstadoProtocolosNutri(carregado = true)
        setContent { DietaTheme { ProtocolosNutriCard(estado, AcoesProtocolosNutri(aoDobras = { dobras = it })) } }
        onNodeWithText("Meus protocolos").assertExists()
        onNodeWithText("Salvar protocolos").assertIsNotEnabled()
        onNodeWithText("3 dobras").performClick()
        assertEquals(ProtocoloDobras.TRES, dobras)
    }

    @Test fun comAlteracaoHabilitaESemLeituraPedeParaTentarDeNovo() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        setContent { DietaTheme { ProtocolosNutriCard(EstadoProtocolosNutri(carregado = true, alterado = true, aviso = MensagemProtocolos.SALVO), AcoesProtocolosNutri()) } }
        onNodeWithText("Salvar protocolos").assertIsEnabled()
        onNodeWithText("Protocolos salvos.").assertExists()
    }

    @Test fun semLeituraMostraTentarDeNovo() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        setContent { DietaTheme { ProtocolosNutriCard(EstadoProtocolosNutri(carregado = false, erro = MensagemProtocolos.SEM_CONEXAO), AcoesProtocolosNutri()) } }
        onNodeWithText("Tentar de novo").assertExists()
        onNodeWithText("Sem conexão. Os protocolos são salvos só com internet.").assertExists()
    }
}
