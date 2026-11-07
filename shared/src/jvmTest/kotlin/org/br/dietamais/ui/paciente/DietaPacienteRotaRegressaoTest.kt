package org.br.dietamais.ui.paciente

import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.navigationevent.*
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.datetime.*
import org.br.dietamais.app.*
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.*
import org.br.dietamais.ui.paciente.dieta.*
import org.br.dietamais.ui.paciente.substituicao.TrocaTela
import org.br.dietamais.ui.theme.DietaTheme
import kotlin.test.*
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

/** Exercita o mesmo conteúdo de rota usado nos dois shells, com ViewModel real e porta local falhando. */
@OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
class DietaPacienteRotaRegressaoTest {
    private class Rotina : PortaRotina {
        var falha = true
        val dados = MutableStateFlow(DadosRotina())
        override fun observar() = dados
        override fun registrarCheckin(dietaId: Uuid, refeicaoId: Uuid, data: LocalDate, status: StatusCheckin, trocas: List<TrocaCheckin>, agora: Instant) {
            if (falha) throw IllegalStateException("falha simulada")
            dados.value = dados.value.copy(checkins = dados.value.checkins + CheckinResposta(Uuid.random(), Uuid.random(), dietaId, refeicaoId, data, status, trocas, agora))
        }
        override fun registrarAgua(ml: Int, agora: Instant) = Unit
        override fun registrarPeso(pesoKg: Double, agora: Instant) = Unit
    }
    private fun vm(rotina: Rotina): DietaPacienteViewModel {
        val s = JsonContrato.decodeFromString<DietasPacienteResposta>(recursoContrato("fase-2/listarMinhasDietas/resposta.json")).itens.single()
        val alimento = s.listaSubstituicao.alimentos.single()
        val snapshot = s.copy(listaSubstituicao = s.listaSubstituicao.copy(alimentos = listOf(alimento, alimento.copy(id = Uuid.random(), nome = "Substituto fictício"))))
        val porta = object : PortaDietaPaciente {
            override fun observar() = flowOf(DadosDietaPaciente(listOf(snapshot)))
            override suspend fun selecionar(dietaId: Uuid, versao: Long): DietaEmUsoResposta = error("não deve selecionar")
        }
        return DietaPacienteViewModel(porta, rotina, object : Clock { override fun now() = Instant.parse("2026-11-01T12:00:00Z") },
            TimeZone.UTC, Dispatchers.Unconfined, tique = flowOf(Unit))
    }
    @Test fun falhaEmComiFicaVisivelEAceitaNovaTentativa() = verificarFalha("Comi", "Feita")
    @Test fun falhaEmPuleiFicaVisivelEAceitaNovaTentativa() = verificarFalha("Pulei", "Pulada")
    @Test fun falhaNaTrocaFicaVisivelNoDialogoEPreservaEscolha() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        val rotina = Rotina(); val vm = vm(rotina); val r = vm.estado.value.refeicoes.single()
        val item = r.itens.single()
        val substituto = vm.estado.value.dieta!!.listaSubstituicao.alimentos.first { it.nome == "Substituto fictício" }
        vm.abrirRefeicao(r.id); vm.abrirTroca(r.id, item.id); vm.escolherSubstituto(item.id, substituto.id)
        setContent { DietaTheme { val estado by vm.estado.collectAsState(); TrocaTela(estado, vm::escolherSubstituto, vm::confirmarTroca, vm::cancelarTroca) } }
        onNodeWithText("Confirmar troca").performClick()
        onNodeWithText("Não foi possível concluir. Tente de novo.").assertIsDisplayed()
        assertEquals(substituto.id, vm.estado.value.troca?.escolhas?.get(item.id))
        rotina.falha = false
        onNodeWithText("Confirmar troca").performClick()
        assertNull(vm.estado.value.troca)
        assertEquals(StatusCheckin.TROCADO, vm.estado.value.checkins[r.id])
    }
    @Test fun trocaFiltraPelaBuscaEVoltaPelaSeta() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        val rotina = Rotina(); val vm = vm(rotina); val r = vm.estado.value.refeicoes.single()
        vm.abrirRefeicao(r.id); vm.abrirTroca(r.id, r.itens.single().id)
        var voltou = 0
        setContent { DietaTheme { val estado by vm.estado.collectAsState(); TrocaTela(estado, vm::escolherSubstituto, vm::confirmarTroca) { voltou++ } } }
        onNodeWithText("Substituto fictício", substring = true).assertIsDisplayed()
        onNodeWithContentDescription("Buscar alimento").performTextInput("zzz")
        onNodeWithText("Nenhum alimento encontrado").assertIsDisplayed()
        onAllNodesWithText("Substituto fictício", substring = true).assertCountEquals(0)
        onNodeWithContentDescription("Voltar para Hoje").performClick()
        assertEquals(1, voltou)
    }
    private fun verificarFalha(botao: String, status: String) = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        val rotina = Rotina(); val vm = vm(rotina); val id = vm.estado.value.refeicoes.single().id
        vm.abrirRefeicao(id)
        setContent { DietaTheme { val estado by vm.estado.collectAsState(); DietaPacienteTela(estado, vm::abrirRefeicao, vm::escolher, aoMarcar = vm::marcar) } }
        onNodeWithText(botao).assertIsEnabled().performClick()
        onNodeWithText("Não foi possível concluir. Tente de novo.").assertIsDisplayed()
        assertTrue(rotina.dados.value.checkins.isEmpty())
        rotina.falha = false
        onNodeWithText(botao).assertIsEnabled().performClick()
        onNodeWithText("Registro salvo: $status").assertIsDisplayed()
        onNodeWithText("Não foi possível concluir. Tente de novo.").assertDoesNotExist()
        assertEquals(1, rotina.dados.value.checkins.size)
    }
    @Test fun voltarForaDaDietaNaoFechaListaOuDetalheOcultosEDialogoTemPrioridade() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        val vm = vm(Rotina().apply { falha = false })
        val entrada = DirectNavigationEventInput()
        val dono = object : NavigationEventDispatcherOwner { override val navigationEventDispatcher = NavigationEventDispatcher() }
        dono.navigationEventDispatcher.addInput(entrada)
        setContent { CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides dono) {
            DietaTheme { NavegacaoApp(PerfilNavegacao.PACIENTE, conteudoDieta = { DietaPacienteRota(vm) }) }
        } }
        onNode(hasText("Dieta") and hasClickAction()).performClick()
        onNodeWithText("Ver lista de substituição").performClick()
        onNode(hasText("Compras") and hasClickAction()).performClick()
        waitForIdle(); onNode(hasText("Compras") and hasClickAction()).assertIsSelected()
        runOnUiThread { entrada.backCompleted() }; waitForIdle()
        assertTrue(vm.estado.value.listaAberta)
        onNode(hasText("Hoje") and hasClickAction()).assertIsSelected()
        onNode(hasText("Dieta") and hasClickAction()).performClick()
        onNodeWithText("Lista de substituição").assertIsDisplayed()
        runOnUiThread { entrada.backCompleted() }; waitForIdle()
        assertFalse(vm.estado.value.listaAberta)
        onNode(hasText("Dieta") and hasClickAction()).assertIsSelected()
        onNodeWithText("Refeição fictícia", substring = true).performClick()
        val refeicao = vm.estado.value.refeicaoAberta
        onNode(hasText("Perfil") and hasClickAction()).performClick()
        waitForIdle(); onNode(hasText("Perfil") and hasClickAction()).assertIsSelected()
        runOnUiThread { entrada.backCompleted() }; waitForIdle()
        assertEquals(refeicao, vm.estado.value.refeicaoAberta)
        onNode(hasText("Dieta") and hasClickAction()).performClick()
        onNodeWithText("Trocar").performClick()
        onNodeWithText("Cancelar").assertIsDisplayed()
        runOnUiThread { entrada.backCompleted() }; waitForIdle()
        assertNull(vm.estado.value.troca)
        assertEquals(refeicao, vm.estado.value.refeicaoAberta)
        onNode(hasText("Dieta") and hasClickAction()).assertIsSelected()
        runOnUiThread { entrada.backCompleted() }; waitForIdle()
        assertNull(vm.estado.value.refeicaoAberta)
        dono.navigationEventDispatcher.dispose()
    }
}
