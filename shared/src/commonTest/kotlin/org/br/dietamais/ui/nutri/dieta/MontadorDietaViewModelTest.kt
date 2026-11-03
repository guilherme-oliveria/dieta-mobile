package org.br.dietamais.ui.nutri.dieta

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.*
import kotlin.test.*
import kotlin.time.Instant
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class MontadorDietaViewModelTest {
    private val paciente = Uuid.parse("019a0f32-5000-7000-8000-0000000000aa")
    private val grupo = GrupoAlimentoResposta(Uuid.parse("019a0f32-5000-7000-8000-000000000001"), "Pão", 150.0, 28.0, 4.5, 1.25)
    private val pao = AlimentoResposta(Uuid.parse("019a0f32-5000-7000-8000-000000000002"), null, grupo.id, "Pão fictício", "1 fatia", 50.0, true, 1, Instant.parse("2026-10-26T12:00:00Z"))
    private val zero = TotaisNutricionaisResposta(0.0, 0.0, 0.0, 0.0)

    private inner class Dietas : PortaDietas {
        val criadas = mutableListOf<SalvarDietaRequisicao>(); val salvas = mutableListOf<SalvarDietaRequisicao>(); val publicadas = mutableListOf<Long>()
        var falha: Exception? = null
        private fun resposta(req: SalvarDietaRequisicao, id: Uuid, versao: Long) = DietaResposta(id, paciente, Uuid.random(), req.titulo, StatusDieta.RASCUNHO, req.orientacao,
            emptyList(), zero, versao, Instant.parse("2026-10-26T12:00:00Z"), Instant.parse("2026-10-26T12:00:00Z"))
        override suspend fun listar(pacienteId: Uuid) = emptyList<DietaResposta>()
        override suspend fun criar(pacienteId: Uuid, requisicao: SalvarDietaRequisicao, chave: Uuid): DietaResposta { falha?.let { throw it }; criadas += requisicao; return resposta(requisicao, requisicao.id!!, 1) }
        override suspend fun salvar(id: Uuid, versao: Long, requisicao: SalvarDietaRequisicao): DietaResposta { falha?.let { throw it }; salvas += requisicao; return resposta(requisicao, id, versao + 1) }
        override suspend fun publicar(id: Uuid, versao: Long): DietaSnapshotResposta { falha?.let { throw it }; publicadas += versao; error("resposta não usada") }
        override suspend fun duplicar(id: Uuid, chave: Uuid) = error("não usado")
    }
    private val catalogo = object : PortaAlimentos {
        override fun observar() = flowOf(CatalogoAlimentos(listOf(grupo), listOf(pao)))
        override suspend fun atualizar() {}
        override suspend fun criar(requisicao: CriarAlimentoRequisicao, chave: Uuid) = error("não usado")
        override suspend fun alterar(id: Uuid, versao: Long, requisicao: AtualizarAlimentoRequisicao) = error("não usado")
    }
    private fun TestScope.vm(d: Dietas) = MontadorDietaViewModel(d, catalogo, StandardTestDispatcher(testScheduler)).also { it.abrirPaciente(paciente); runCurrent() }

    @Test fun totaisAtualizamEmTempoRealComAFormulaDoBackend() = runTest {
        val vm = vm(Dietas()); vm.nova(); vm.adicionarRefeicao()
        val ref = vm.estado.value.rascunho!!.refeicoes.single().chave
        vm.adicionarItem(ref, pao.id); val item = vm.estado.value.rascunho!!.refeicoes.single().itens.single().chave
        assertEquals(150.0, vm.estado.value.totais.kcalReais)
        vm.mudarPorcoes(ref, item, "1,5"); assertEquals(225.0, vm.estado.value.totais.kcalReais); assertEquals(1.88, vm.estado.value.totais.gordurasReais)
        vm.mudarPorcoes(ref, item, "abc"); assertEquals(0.0, vm.estado.value.totais.kcalReais)
    }
    @Test fun salvarValidaCamposECriaComIdDoClienteDepoisAtualizaComVersao() = runTest {
        val d = Dietas(); val vm = vm(d); vm.nova(); vm.adicionarRefeicao(); vm.salvar(); runCurrent()
        assertTrue(d.criadas.isEmpty()); assertEquals(MensagemDieta.TITULO_OBRIGATORIO, vm.estado.value.errosCampos["titulo"])
        val ref = vm.estado.value.rascunho!!.refeicoes.single().chave
        vm.mudarTitulo("Plano fictício"); vm.mudarNomeRefeicao(ref, "Café"); vm.adicionarItem(ref, pao.id); vm.salvar(); runCurrent()
        val criada = d.criadas.single(); assertEquals(vm.estado.value.rascunho!!.dietaId, criada.id); assertEquals(0, criada.refeicoes.single().ordem)
        assertEquals(1L, vm.estado.value.rascunho!!.versao); assertFalse(vm.estado.value.rascunho!!.alterado)
        vm.mudarTitulo("Plano novo"); vm.salvar(); runCurrent()
        assertEquals("Plano novo", d.salvas.single().titulo); assertNull(d.salvas.single().id); assertEquals(2L, vm.estado.value.rascunho!!.versao)
    }
    @Test fun moverRemoverELimiteDeRefeicoes() = runTest {
        val vm = vm(Dietas()); vm.nova(); repeat(25) { vm.adicionarRefeicao() }
        assertEquals(MontadorDietaViewModel.LIMITE_REFEICOES, vm.estado.value.rascunho!!.refeicoes.size)
        val (a, b) = vm.estado.value.rascunho!!.refeicoes.take(2).map { it.chave }
        vm.moverRefeicao(a, 1); assertEquals(b, vm.estado.value.rascunho!!.refeicoes.first().chave)
        vm.removerRefeicao(a); assertEquals(19, vm.estado.value.rascunho!!.refeicoes.size)
    }
    @Test fun publicarExigeSalvoESemAlteracaoEMostraDietaVazia() = runTest {
        val d = Dietas(); val vm = vm(d); vm.nova(); vm.pedirPublicacao(); assertFalse(vm.estado.value.confirmarPublicar)
        vm.mudarTitulo("Plano"); vm.salvar(); runCurrent(); vm.pedirPublicacao(); assertTrue(vm.estado.value.confirmarPublicar)
        d.falha = ErroApi("DIETA_VAZIA", "RN-DIE-06", emptyList(), 422, null, null)
        vm.confirmarPublicacao(); runCurrent()
        assertEquals(MensagemDieta.DIETA_VAZIA, vm.estado.value.erro); assertFalse(vm.estado.value.confirmarPublicar)
    }
    @Test fun validadoresDePorcoesEHorario() {
        assertEquals(1.5, MontadorDietaViewModel.porcoesValidas("1,5")); assertNull(MontadorDietaViewModel.porcoesValidas("0")); assertNull(MontadorDietaViewModel.porcoesValidas("1,23456"))
        assertNotNull(MontadorDietaViewModel.horarioValido("7:05")); assertNull(MontadorDietaViewModel.horarioValido("24:00")); assertNull(MontadorDietaViewModel.horarioValido("8h"))
    }
}
