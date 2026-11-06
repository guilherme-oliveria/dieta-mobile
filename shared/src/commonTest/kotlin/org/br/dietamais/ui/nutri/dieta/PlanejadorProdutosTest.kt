package org.br.dietamais.ui.nutri.dieta

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlinx.datetime.LocalTime
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.*
import kotlin.test.*
import kotlin.time.Instant
import kotlin.uuid.Uuid

/** MOB-71: produto da nutri como item do planejador (gramas, `produtoId` no pedido). */
@OptIn(ExperimentalCoroutinesApi::class)
class PlanejadorProdutosTest {
    private val paciente = Uuid.parse("019a0f32-5000-7000-8000-0000000000aa")
    private val agora = Instant.parse("2026-11-05T12:00:00Z")
    private val zero = TotaisNutricionaisResposta(0.0, 0.0, 0.0, 0.0)
    private val whey = ProdutoResposta(Uuid.random(), "Whey Max", "Marca", medidaGramas = 30.0, nutrientes = mapOf("energia_kcal" to 400.0, "carboidrato_g" to 10.0, "proteina_g" to 80.0, "lipideos_g" to 5.0), versao = 1, atualizadoEm = agora)

    private class Dietas : PortaDietas {
        val criadas = mutableListOf<SalvarDietaRequisicao>()
        override suspend fun listar(pacienteId: Uuid) = emptyList<DietaResposta>()
        override suspend fun criar(pacienteId: Uuid, requisicao: SalvarDietaRequisicao, chave: Uuid): DietaResposta {
            criadas += requisicao
            return DietaResposta(requisicao.id!!, pacienteId, Uuid.random(), requisicao.titulo, StatusDieta.RASCUNHO, null, emptyList(), TotaisNutricionaisResposta(0.0, 0.0, 0.0, 0.0), 1,
                Instant.parse("2026-11-05T12:00:00Z"), Instant.parse("2026-11-05T12:00:00Z"))
        }
        override suspend fun salvar(id: Uuid, versao: Long, requisicao: SalvarDietaRequisicao) = error("não usado")
        override suspend fun publicar(id: Uuid, versao: Long) = error("não usado")
        override suspend fun duplicar(id: Uuid, chave: Uuid) = error("não usado")
    }
    private val porta = object : PortaPorcoes { override suspend fun obter() = PorcoesGruposResposta("Planilha fictícia", emptyMap(), emptyList()) }
    private val produtos = object : PortaProdutos {
        val buscas = mutableListOf<String>()
        override suspend fun listar(busca: String, ativo: Boolean, pagina: Int): PaginaProdutosResposta { buscas += busca; return PaginaProdutosResposta(listOf(whey), 0, 20, 1) }
        override suspend fun criar(requisicao: CriarProdutoRequisicao, chave: Uuid) = error("não usado")
        override suspend fun alterar(id: Uuid, versao: Long, requisicao: AtualizarProdutoRequisicao) = error("não usado")
    }
    private fun TestScope.vm(d: Dietas) = PlanejadorPorcoesViewModel(d, porta, StandardTestDispatcher(testScheduler), produtos = produtos).also { it.abrirPaciente(paciente); runCurrent(); it.nova() }

    @Test fun buscaProdutosPreencheOSeletor() = runTest {
        val vm = vm(Dietas()); assertTrue(vm.estado.value.produtosDisponiveis)
        vm.buscarProdutos(" whey "); runCurrent()
        assertEquals(listOf(whey), vm.estado.value.produtosBusca); assertEquals(listOf("whey"), produtos.buscas)
    }
    @Test fun produtoEntraEmGramasAndaDeDezEmDezESaiComoProdutoIdNoPedido() = runTest {
        val d = Dietas(); val vm = vm(d); val ref = vm.estado.value.rascunho!!.refeicoes.first().id
        vm.adicionarProduto(ref, whey)
        val item = vm.estado.value.rascunho!!.refeicoes.single().itens.single() as ItemPlano.DoProduto
        assertEquals(30.0, item.gramas); assertEquals(400.0, item.kcalPor100)
        vm.ajustarGramas(ref, item.chave, 2); assertEquals(50.0, (vm.estado.value.rascunho!!.refeicoes.single().itens.single() as ItemPlano.DoProduto).gramas)
        vm.ajustarGramas(ref, item.chave, -50); assertEquals(10.0, (vm.estado.value.rascunho!!.refeicoes.single().itens.single() as ItemPlano.DoProduto).gramas)
        vm.mudarTitulo("Com whey"); vm.mudarNomeRefeicao(ref, "Lanche"); vm.salvar(); runCurrent()
        val pedido = d.criadas.single().refeicoes.single().itens.single()
        assertEquals(whey.id, pedido.produtoId); assertEquals(10.0, pedido.gramas); assertNull(pedido.porcoes); assertNull(pedido.grupoId); assertNull(pedido.alimentoId)
    }
    @Test fun dietaComProdutoAbreNoPlanejador() = runTest {
        val vm = vm(Dietas())
        val dieta = DietaResposta(Uuid.random(), paciente, Uuid.random(), "Com whey", StatusDieta.RASCUNHO, null, listOf(RefeicaoResposta(Uuid.random(), "Lanche", LocalTime(10, 0), 0,
            listOf(ItemRefeicaoResposta(Uuid.random(), nomeAlimento = "Whey Max", gramas = 45.0, produtoId = whey.id, totais = zero)), zero)), zero, 2, agora, agora)
        assertTrue(usaPorcoes(dieta)); assertTrue(vm.abrir(dieta))
        val item = vm.estado.value.rascunho!!.refeicoes.single().itens.single() as ItemPlano.DoProduto
        assertEquals("Whey Max", item.nome); assertEquals(45.0, item.gramas); assertEquals(whey.id, item.produtoId)
    }
}
