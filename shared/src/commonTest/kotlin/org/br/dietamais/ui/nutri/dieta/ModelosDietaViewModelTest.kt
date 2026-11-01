package org.br.dietamais.ui.nutri.dieta

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.*
import kotlin.test.*
import kotlin.time.Instant
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class ModelosDietaViewModelTest {
    private val agora = Instant.parse("2026-11-01T12:00:00Z")
    private val zero = TotaisNutricionaisResposta(0.0, 0.0, 0.0, 0.0)
    private fun modelo(titulo: String, escopo: String, versao: Long = 1, restricoes: List<String> = emptyList()) =
        ModeloDietaResposta(Uuid.random(), escopo, titulo, restricoes = restricoes, kcalReferencia = 1800.0, refeicoes = emptyList(), versao = versao, criadoEm = agora, atualizadoEm = agora)

    private inner class Modelos(var lista: List<ModeloDietaResposta>) : PortaModelosDieta {
        var falha: Exception? = null
        val excluidos = mutableListOf<Pair<Uuid, Long>>(); val salvosDe = mutableListOf<Uuid>()
        override suspend fun listar(): List<ModeloDietaResposta> { falha?.let { throw it }; return lista }
        override suspend fun duplicar(id: Uuid, chave: Uuid): ModeloDietaResposta { falha?.let { throw it }; return modelo(lista.first { it.id == id }.titulo + " (cópia)", "NUTRI") }
        override suspend fun excluir(id: Uuid, versao: Long) { falha?.let { throw it }; excluidos += id to versao }
        override suspend fun salvarComoModelo(dietaId: Uuid, requisicao: SalvarComoModeloRequisicao, chave: Uuid): ModeloDietaResposta { falha?.let { throw it }; salvosDe += dietaId; return modelo(requisicao.titulo, "NUTRI") }
    }
    private fun TestScope.vm(m: Modelos) = ModelosDietaViewModel(m, StandardTestDispatcher(testScheduler))

    @Test fun listaSeparaOsDoSistemaDosDela() = runTest {
        val sistema = modelo("Hipertensão", "SISTEMA", restricoes = listOf("ALTO_SODIO")); val meu = modelo("Meu", "NUTRI")
        val vm = vm(Modelos(listOf(sistema, meu))); vm.carregar(); runCurrent()
        assertEquals(listOf(sistema), vm.estado.value.visiveis)
        vm.escolherAba(AbaModelos.MEUS); assertEquals(listOf(meu), vm.estado.value.visiveis)
        assertTrue(vm.estado.value.carregado)
    }
    @Test fun duplicarCriaCopiaEPassaParaMeus() = runTest {
        val sistema = modelo("Hipertensão", "SISTEMA"); val vm = vm(Modelos(listOf(sistema))); vm.carregar(); runCurrent()
        vm.duplicar(sistema); runCurrent()
        assertEquals(AbaModelos.MEUS, vm.estado.value.aba); assertEquals(listOf("Hipertensão (cópia)"), vm.estado.value.visiveis.map { it.titulo })
        assertEquals(MensagemModelos.DUPLICADO, vm.estado.value.aviso)
    }
    @Test fun modeloDoSistemaNaoSeApagaEACopiaPedeConfirmacaoComAVersao() = runTest {
        val sistema = modelo("Do sistema", "SISTEMA"); val meu = modelo("Meu", "NUTRI", versao = 3); val m = Modelos(listOf(sistema, meu))
        val vm = vm(m); vm.carregar(); runCurrent()
        vm.pedirExclusao(sistema); assertNull(vm.estado.value.apagar); assertEquals(MensagemModelos.SOMENTE_LEITURA, vm.estado.value.erro)
        vm.pedirExclusao(meu); assertEquals(meu, vm.estado.value.apagar); assertTrue(m.excluidos.isEmpty())
        vm.cancelarExclusao(); assertNull(vm.estado.value.apagar); assertTrue(m.excluidos.isEmpty())
        vm.pedirExclusao(meu); vm.confirmarExclusao(); runCurrent()
        assertEquals(listOf(meu.id to 3L), m.excluidos); assertEquals(listOf(sistema), vm.estado.value.modelos); assertEquals(MensagemModelos.APAGADO, vm.estado.value.aviso)
    }
    @Test fun salvarDietaComoModeloAdicionaEAvisa() = runTest {
        val m = Modelos(emptyList()); val vm = vm(m); vm.carregar(); runCurrent()
        val dieta = DietaResposta(Uuid.random(), Uuid.random(), Uuid.random(), "Dieta da Ana", StatusDieta.RASCUNHO, null, emptyList(), zero, 1, agora, agora)
        vm.salvarComoModelo(dieta); runCurrent()
        assertEquals(listOf(dieta.id), m.salvosDe); assertEquals(listOf("Dieta da Ana"), vm.estado.value.modelos.map { it.titulo }); assertEquals(MensagemModelos.SALVO, vm.estado.value.aviso)
    }
    @Test fun falhasViramMensagensPeloCodigoDoErro() = runTest {
        val m = Modelos(emptyList()); val vm = vm(m)
        m.falha = FalhaComunicacao(); vm.carregar(); runCurrent(); assertEquals(MensagemModelos.SEM_CONEXAO, vm.estado.value.erro); assertTrue(vm.estado.value.semConexao); assertFalse(vm.estado.value.carregado)
        m.falha = ErroApi("CONFLITO_VERSAO", null, emptyList(), 409, null, null); vm.recarregar(); runCurrent(); assertEquals(MensagemModelos.CONFLITO, vm.estado.value.erro)
        m.falha = ErroApi("ACESSO_NEGADO", "RN-MOD-01", emptyList(), 403, null, null); vm.recarregar(); runCurrent(); assertEquals(MensagemModelos.SOMENTE_LEITURA, vm.estado.value.erro)
    }
}
