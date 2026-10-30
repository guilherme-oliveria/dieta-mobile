package org.br.dietamais.ui.nutri

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.ui.nutri.avaliacao.AcoesHistorico
import org.br.dietamais.ui.nutri.avaliacao.EstadoAvaliacoes
import org.br.dietamais.ui.nutri.avaliacao.HistoricoAvaliacoesTela
import org.br.dietamais.ui.nutri.paciente.PacienteFichaTela
import org.br.dietamais.ui.nutri.pacientes.AbaFicha
import org.br.dietamais.ui.nutri.pacientes.EstadoPacientes
import org.br.dietamais.ui.paciente.evolucao.AcoesEvolucao
import org.br.dietamais.ui.paciente.evolucao.EstadoEvolucao
import org.br.dietamais.ui.paciente.evolucao.EvolucaoTela
import org.br.dietamais.ui.theme.DietaTheme
import kotlin.test.Test
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * Regressão: abrir Avaliações ou Evolução na ficha do paciente (perfil nutri, app) fechava o app. A ficha já rola na vertical e essas abas
 * rolavam de novo por dentro: o Compose lança "Vertically scrollable component was measured with an infinity maximum height constraints".
 */
@OptIn(ExperimentalTestApi::class)
class FichaAbasRolagemTest {
    private val paciente = PacienteResposta(Uuid.random(), Uuid.random(), "Pessoa fictícia", status = StatusVinculo.ATIVO,
        versao = 0, criadoEm = Instant.parse("2026-01-01T00:00:00Z"), atualizadoEm = Instant.parse("2026-01-01T00:00:00Z"))

    @Test fun abaEvolucaoAbreDentroDaFichaSemFecharOApp() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        setContent { DietaTheme { PacienteFichaTela(EstadoPacientes(selecionado = paciente, abaFicha = AbaFicha.EVOLUCAO), {}, {}, {}) { _, _ ->
            EvolucaoTela(EstadoEvolucao(carregado = true), AcoesEvolucao(), rolavel = false)
        } } }
        onNodeWithText("Pessoa fictícia").assertIsDisplayed()
    }

    @Test fun abaAvaliacoesAbreDentroDaFichaSemFecharOApp() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        setContent { DietaTheme { PacienteFichaTela(EstadoPacientes(selecionado = paciente, abaFicha = AbaFicha.AVALIACOES), {}, {}, {}) { _, _ ->
            HistoricoAvaliacoesTela(EstadoAvaliacoes(), AcoesHistorico(), rolavel = false)
        } } }
        onNodeWithText("Pessoa fictícia").assertIsDisplayed()
    }

    @Test fun comRolagemPropriaDentroDaFichaOComposeLancaEPorIssoAsRotasDaFichaPassamRolavelFalso() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        // Documenta o erro original: se alguém voltar a deixar a aba rolando por dentro da ficha, este é o sintoma.
        val erro = runCatching {
            setContent { DietaTheme { PacienteFichaTela(EstadoPacientes(selecionado = paciente, abaFicha = AbaFicha.EVOLUCAO), {}, {}, {}) { _, _ ->
                EvolucaoTela(EstadoEvolucao(carregado = true), AcoesEvolucao(), rolavel = true)
            } } }
            waitForIdle()
        }.exceptionOrNull()
        kotlin.test.assertNotNull(erro, "rolagem dentro de rolagem deveria lançar")
    }

    @Test fun foraDaFichaATelaDoPacienteContinuaRolando() = runSkikoComposeUiTest(size = Size(390f, 300f)) {
        setContent { DietaTheme { EvolucaoTela(EstadoEvolucao(carregado = true), AcoesEvolucao()) } }
        onNode(hasScrollAction()).assertExists()
    }

    @Test fun abaDietaComListaEMontadorAbertoDentroDaFichaNaoFechaOApp() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        val rascunho = org.br.dietamais.ui.nutri.dieta.RascunhoDieta(titulo = "Plano fictício")
        setContent { DietaTheme { PacienteFichaTela(EstadoPacientes(selecionado = paciente, abaFicha = AbaFicha.DIETA), {}, {}, {}) { _, _ ->
            org.br.dietamais.ui.nutri.dieta.DietasListaTela(org.br.dietamais.ui.nutri.dieta.EstadoDieta(), {}, {}, {}, {})
            androidx.compose.ui.window.Dialog(onDismissRequest = {}, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
                org.br.dietamais.ui.nutri.dieta.MontarDietaTela(org.br.dietamais.ui.nutri.dieta.EstadoDieta(rascunho = rascunho), org.br.dietamais.ui.nutri.dieta.AcoesMontador())
            }
        } } }
        waitForIdle()
        onNodeWithText("Pessoa fictícia").assertExists()
    }

    @Test fun abaListaDentroDaFichaNaoFechaOApp() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        setContent { DietaTheme { PacienteFichaTela(EstadoPacientes(selecionado = paciente, abaFicha = AbaFicha.LISTA), {}, {}, {}) { _, _ ->
            org.br.dietamais.ui.nutri.listapaciente.ListaPacienteTela(org.br.dietamais.ui.nutri.listapaciente.EstadoListaPaciente())
        } } }
        onNodeWithText("Pessoa fictícia").assertExists()
    }
}
