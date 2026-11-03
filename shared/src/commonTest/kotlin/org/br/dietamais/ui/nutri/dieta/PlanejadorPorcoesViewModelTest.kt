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
class PlanejadorPorcoesViewModelTest {
    private val paciente = Uuid.parse("019a0f32-5000-7000-8000-0000000000aa")
    private val arroz = Uuid.parse("019a0f32-5000-7000-8000-000000000001")
    private val vegetal = Uuid.parse("019a0f32-5000-7000-8000-000000000002")
    private val zero = TotaisNutricionaisResposta(0.0, 0.0, 0.0, 0.0)
    private fun opcao(n: Int, kcal: Double, gli: Double, pro: Double, lip: Double, guia: Boolean = false) =
        OpcaoPorcaoResposta(n, kcal, 100.0, "g", null, guia, mapOf("energia" to kcal, "gli" to gli, "pro" to pro, "lip" to lip))
    private val referencia = PorcoesGruposResposta("Planilha fictícia", emptyMap(), listOf(
        GrupoPorcoesResposta(arroz, "Arroz", listOf(opcao(1, 100.0, 20.0, 2.0, 1.0), opcao(2, 130.0, 28.0, 3.0, 0.5, guia = true))),
        GrupoPorcoesResposta(vegetal, "Vegetal cru", listOf(opcao(1, 10.0, 2.0, 0.5, 0.0)))))

    private inner class Dietas : PortaDietas {
        val criadas = mutableListOf<SalvarDietaRequisicao>(); val salvas = mutableListOf<SalvarDietaRequisicao>()
        var falha: Exception? = null
        private fun resposta(req: SalvarDietaRequisicao, id: Uuid, versao: Long) = DietaResposta(id, paciente, Uuid.random(), req.titulo, StatusDieta.RASCUNHO, req.orientacao,
            emptyList(), zero, versao, Instant.parse("2026-10-26T12:00:00Z"), Instant.parse("2026-10-26T12:00:00Z"))
        override suspend fun listar(pacienteId: Uuid) = emptyList<DietaResposta>()
        override suspend fun criar(pacienteId: Uuid, requisicao: SalvarDietaRequisicao, chave: Uuid): DietaResposta { falha?.let { throw it }; criadas += requisicao; return resposta(requisicao, requisicao.id!!, 1) }
        override suspend fun salvar(id: Uuid, versao: Long, requisicao: SalvarDietaRequisicao): DietaResposta { falha?.let { throw it }; salvas += requisicao; return resposta(requisicao, id, versao + 1) }
        override suspend fun publicar(id: Uuid, versao: Long) = error("não usado")
        override suspend fun duplicar(id: Uuid, chave: Uuid) = error("não usado")
    }
    private val porta = object : PortaPorcoes { override suspend fun obter() = referencia }
    private fun TestScope.vm(d: Dietas) = PlanejadorPorcoesViewModel(d, porta, StandardTestDispatcher(testScheduler)).also { it.abrirPaciente(paciente); runCurrent(); it.nova() }
    private fun PlanejadorPorcoesViewModel.principal() = estado.value.rascunho!!.refeicoes.first().id

    @Test fun grupoNovoGanhaAPorcaoDoGuiaEATrocaValeParaTodosOsItens() = runTest {
        val vm = vm(Dietas()); val ref = vm.principal()
        vm.adicionarGrupo(ref, arroz); assertEquals(2, vm.estado.value.rascunho!!.escolhas[arroz])
        assertEquals(130.0, vm.estado.value.totalDia.energia)
        vm.adicionarRefeicao(); val almoco = vm.estado.value.rascunho!!.refeicoes.last().id
        vm.adicionarGrupo(almoco, arroz); assertEquals(260.0, vm.estado.value.totalDia.energia)
        vm.escolherPorcao(arroz, 1); assertEquals(200.0, vm.estado.value.totalDia.energia)
        vm.escolherPorcao(arroz, 9); assertEquals(1, vm.estado.value.rascunho!!.escolhas[arroz])
    }
    @Test fun quantidadeAndaDeMeioEmMeioELimitaNosExtremos() = runTest {
        val vm = vm(Dietas()); val ref = vm.principal(); vm.adicionarGrupo(ref, arroz)
        val item = vm.estado.value.rascunho!!.refeicoes.single().itens.single().chave
        vm.ajustarPorcoes(ref, item, 3); assertEquals(2.5 * 130.0, vm.estado.value.totalDia.energia)
        vm.ajustarPorcoes(ref, item, -20); assertEquals(65.0, vm.estado.value.totalDia.energia)
        vm.ajustarPorcoes(ref, item, 500); assertEquals(99.0, (vm.estado.value.rascunho!!.refeicoes.single().itens.single() as ItemPlano.DoGrupo).porcoes)
    }
    @Test fun aVontadeETextoLivreNaoEntramNaConta() = runTest {
        val vm = vm(Dietas()); val ref = vm.principal()
        vm.adicionarAVontade(ref, vegetal); vm.adicionarTexto(ref, "  Chá sem açúcar "); vm.adicionarTexto(ref, "   ")
        assertEquals(2, vm.estado.value.rascunho!!.refeicoes.single().itens.size); assertEquals(0.0, vm.estado.value.totalDia.energia)
        assertTrue(vm.estado.value.rascunho!!.escolhas.isEmpty())
    }
    @Test fun alternativaFicaForaDoDiaEMostraADiferenca() = runTest {
        val vm = vm(Dietas()); val ref = vm.principal(); vm.mudarMetas(MetasDieta(1000.0, 50.0, 20.0, 30.0))
        vm.adicionarGrupo(ref, arroz); vm.adicionarAlternativa(ref)
        val alt = vm.estado.value.rascunho!!.refeicoes.last(); assertEquals(ref, alt.alternativaDe)
        vm.adicionarGrupo(alt.id, vegetal)
        assertEquals(130.0, vm.estado.value.totalDia.energia); assertEquals(870.0, vm.estado.value.saldo!!.kcal)
        assertEquals(-120.0, vm.estado.value.diferencas.getValue(alt.id).kcal)
        vm.removerRefeicao(ref); assertTrue(vm.estado.value.rascunho!!.refeicoes.isEmpty())
    }
    @Test fun salvarValidaEEnviaPorcoesMetasEItensCertos() = runTest {
        val d = Dietas(); val vm = vm(d); val ref = vm.principal()
        vm.salvar(); runCurrent(); assertEquals(MensagemPlanejador.TITULO_OBRIGATORIO, vm.estado.value.erro)
        vm.mudarTitulo("Plano fictício"); vm.salvar(); runCurrent(); assertEquals(MensagemPlanejador.DIETA_VAZIA, vm.estado.value.erro)
        vm.adicionarGrupo(ref, arroz); vm.adicionarAVontade(ref, vegetal); vm.adicionarTexto(ref, "Chá")
        vm.salvar(); runCurrent(); assertEquals(MensagemPlanejador.NOME_REFEICAO_OBRIGATORIO, vm.estado.value.erro)
        vm.mudarNomeRefeicao(ref, "Desjejum"); vm.mudarMetas(MetasDieta(2000.0, 50.0, 30.0, 30.0)); vm.salvar(); runCurrent()
        assertEquals(MensagemPlanejador.METAS_INVALIDAS, vm.estado.value.erro)
        vm.mudarMetas(MetasDieta(2000.0, 50.0, 20.0, 30.0)); vm.mudarRodape(" Fonte: planilha "); vm.salvar(); runCurrent()
        val pedido = d.criadas.single(); assertEquals(listOf(PorcaoGrupo(arroz, 2)), pedido.porcoes); assertEquals("Fonte: planilha", pedido.rodape)
        val itens = pedido.refeicoes.single().itens
        assertEquals(1.0, itens[0].porcoes); assertEquals(arroz, itens[0].grupoId)
        assertEquals(true, itens[1].aVontade); assertNull(itens[1].porcoes); assertNull(itens[2].grupoId); assertEquals("Chá", itens[2].textoLivre)
        assertEquals(MensagemPlanejador.SALVA, vm.estado.value.aviso); assertEquals(1L, vm.estado.value.rascunho!!.versao)
        vm.mudarTitulo("Novo"); vm.salvar(); runCurrent(); assertNull(d.salvas.single().id); assertEquals(2L, vm.estado.value.rascunho!!.versao)
    }
    @Test fun alternativaSalvaComTipoEIdDaPrincipal() = runTest {
        val d = Dietas(); val vm = vm(d); val ref = vm.principal()
        vm.mudarTitulo("P"); vm.mudarNomeRefeicao(ref, "Jantar"); vm.adicionarGrupo(ref, arroz); vm.adicionarAlternativa(ref)
        val alt = vm.estado.value.rascunho!!.refeicoes.last().id; vm.adicionarAVontade(alt, vegetal); vm.salvar(); runCurrent()
        val (p, a) = d.criadas.single().refeicoes
        assertEquals(ref, p.id); assertNull(p.tipo); assertEquals("ALTERNATIVA", a.tipo); assertEquals(ref, a.alternativaDe); assertEquals("Jantar", a.nome)
    }
    @Test fun abrirDietaComAlimentoDoCatalogoSoNoMontador() = runTest {
        val vm = vm(Dietas())
        val item = ItemRefeicaoResposta(Uuid.random(), alimentoId = Uuid.random(), nomeAlimento = "Pão", porcoes = 1.0, totais = zero)
        val dieta = DietaResposta(Uuid.random(), paciente, Uuid.random(), "D", StatusDieta.RASCUNHO, null,
            listOf(RefeicaoResposta(Uuid.random(), "Café", kotlinx.datetime.LocalTime(7, 0), 0, listOf(item), zero)), zero, 1, Instant.parse("2026-10-26T12:00:00Z"), Instant.parse("2026-10-26T12:00:00Z"))
        assertFalse(vm.abrir(dieta)); assertEquals(MensagemPlanejador.SO_NO_MONTADOR, vm.estado.value.erro)
    }
    @Test fun abrirDietaPorPorcoesRestauraEscolhasItensEAlternativa() = runTest {
        val vm = vm(Dietas()); val principal = Uuid.random()
        val dieta = DietaResposta(Uuid.random(), paciente, Uuid.random(), "D", StatusDieta.RASCUNHO, null, listOf(
            RefeicaoResposta(principal, "Almoço", kotlinx.datetime.LocalTime(12, 0), 0, listOf(
                ItemRefeicaoResposta(Uuid.random(), grupoId = arroz, porcoes = 2.0, opcao = 1, totais = zero),
                ItemRefeicaoResposta(Uuid.random(), grupoId = vegetal, aVontade = true, totais = zero),
                ItemRefeicaoResposta(Uuid.random(), textoLivre = "Chá", aVontade = true, totais = zero)), zero),
            RefeicaoResposta(Uuid.random(), "Almoço", kotlinx.datetime.LocalTime(12, 0), 1, emptyList(), zero, tipo = "ALTERNATIVA", alternativaDe = principal)),
            zero, 3, Instant.parse("2026-10-26T12:00:00Z"), Instant.parse("2026-10-26T12:00:00Z"), porcoes = listOf(PorcaoGrupo(arroz, 1)), metas = MetasDieta(1000.0, 50.0, 20.0, 30.0))
        assertTrue(vm.abrir(dieta))
        val r = vm.estado.value.rascunho!!; assertEquals(3L, r.versao); assertEquals(3, r.refeicoes.first().itens.size); assertEquals(principal, r.refeicoes.last().alternativaDe)
        assertEquals(200.0, vm.estado.value.totalDia.energia); assertEquals(800.0, vm.estado.value.saldo!!.kcal)
    }
    @Test fun errosDaApiViramMensagens() = runTest {
        val d = Dietas(); val vm = vm(d); val ref = vm.principal()
        vm.mudarTitulo("P"); vm.mudarNomeRefeicao(ref, "C"); vm.adicionarGrupo(ref, arroz)
        d.falha = ErroApi("CONFLITO_VERSAO", "RN-SYN-02", emptyList(), 409, null, null); vm.salvar(); runCurrent()
        assertEquals(MensagemPlanejador.CONFLITO, vm.estado.value.erro)
        d.falha = ErroApi("PORCAO_INVALIDA", "RN-DIE-10", emptyList(), 422, null, null); vm.salvar(); runCurrent()
        assertEquals(MensagemPlanejador.LIMITE_EXCEDIDO, vm.estado.value.erro)
    }
}
