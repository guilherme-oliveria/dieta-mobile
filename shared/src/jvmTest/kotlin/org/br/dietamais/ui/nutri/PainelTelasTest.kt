package org.br.dietamais.ui.nutri

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import kotlinx.datetime.TimeZone
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.dominio.painel.ItemPainel
import org.br.dietamais.dominio.painel.ResumoPainel
import org.br.dietamais.ui.nutri.agenda.consultaFalsa
import org.br.dietamais.ui.nutri.agenda.pacienteFalso
import org.br.dietamais.ui.nutri.painel.*
import org.br.dietamais.ui.theme.DietaTheme
import kotlin.test.*
import kotlin.time.Instant
import kotlin.uuid.Uuid

@OptIn(ExperimentalTestApi::class)
class PainelTelasTest {
    private val fuso = TimeZone.of("America/Sao_Paulo")
    private val ana = pacienteFalso("Ana Fictícia")

    @Test fun mostraConsultaSemRegistrosRespondidosEPlano() = runSkikoComposeUiTest(size = Size(390f, 1400f)) {
        val consulta = consultaFalsa(ana.id, Instant.parse("2026-10-28T17:30:00Z"), 50)
        val assinatura = AssinaturaResumoResposta(Uuid.random(), "Básico", StatusAssinatura.ATIVA, 10, 4, Instant.parse("2026-12-01T00:00:00Z"))
        val resumo = ResumoPainel(listOf(consulta), listOf(ItemPainel(ana.id, "Ana Fictícia")), listOf(ItemPainel(ana.id, "Ana Fictícia")), assinatura)
        var aberto: Uuid? = null
        setContent { DietaTheme { PainelTela(EstadoPainel(resumo, listOf(ana)), AcoesPainel(aoAbrirPaciente = { aberto = it }), fuso) } }
        onNodeWithText("14:30 – 15:20").assertIsDisplayed()
        onNodeWithText("4 de 10 pacientes").assertIsDisplayed()
        onAllNodesWithText("Ana Fictícia")[0].performClick(); assertEquals(ana.id, aberto)
        onNodeWithText("Nenhuma consulta hoje.").assertDoesNotExist()
    }

    @Test fun vazioMostraMensagensEOfflineAvisa() = runSkikoComposeUiTest(size = Size(390f, 1400f)) {
        setContent { DietaTheme { PainelTela(EstadoPainel(semConexao = true), AcoesPainel(), fuso) } }
        onNodeWithText("Nenhuma consulta hoje.").assertIsDisplayed()
        onNodeWithText("Todos os pacientes estão em dia.").assertIsDisplayed()
        onNodeWithText("Nenhum questionário respondido nos últimos 7 dias.").assertIsDisplayed()
        onNodeWithText("Sem conexão: mostrando o que está salvo no aparelho.").assertIsDisplayed()
    }
}
