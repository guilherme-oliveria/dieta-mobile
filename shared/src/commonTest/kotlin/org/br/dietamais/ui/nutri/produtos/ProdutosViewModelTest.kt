package org.br.dietamais.ui.nutri.produtos

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.br.dietamais.data.remoto.ErroApi
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.PortaProdutos
import org.br.dietamais.ui.nutri.alimentos.MensagemAlimentos
import kotlin.test.*
import kotlin.time.Instant
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class ProdutosViewModelTest {
    private val agora = Instant.parse("2026-11-05T12:00:00Z")
    private fun produto(nome: String, ativo: Boolean = true, versao: Long = 1) = ProdutoResposta(Uuid.random(), nome, "Marca", "Whey", medidaCaseira = "1 scoop", medidaGramas = 30.0,
        nutrientes = mapOf("energia_kcal" to 400.0, "carboidrato_g" to 10.0, "proteina_g" to 80.0, "lipideos_g" to 5.0, "calcio_mg" to 120.0), ativo = ativo, versao = versao, atualizadoEm = agora)

    private class Fake(var itens: List<ProdutoResposta>) : PortaProdutos {
        val buscas = mutableListOf<Triple<String, Boolean, Int>>(); val criados = mutableListOf<CriarProdutoRequisicao>(); val alterados = mutableListOf<Triple<Uuid, Long, AtualizarProdutoRequisicao>>()
        var falha: Exception? = null
        override suspend fun listar(busca: String, ativo: Boolean, pagina: Int): PaginaProdutosResposta {
            falha?.let { throw it }; buscas += Triple(busca, ativo, pagina)
            val f = itens.filter { it.ativo == ativo && it.nome.contains(busca, true) }
            return PaginaProdutosResposta(f.drop(pagina * 20).take(20), pagina, 20, f.size.toLong())
        }
        override suspend fun criar(requisicao: CriarProdutoRequisicao, chave: Uuid): ProdutoResposta {
            falha?.let { throw it }; criados += requisicao
            return ProdutoResposta(requisicao.id!!, requisicao.nome, nutrientes = requisicao.nutrientes, versao = 1, atualizadoEm = Instant.parse("2026-11-05T12:00:00Z")).also { itens = itens + it }
        }
        override suspend fun alterar(id: Uuid, versao: Long, requisicao: AtualizarProdutoRequisicao): ProdutoResposta {
            falha?.let { throw it }; alterados += Triple(id, versao, requisicao)
            val novo = itens.first { it.id == id }.copy(nome = requisicao.nome, ativo = requisicao.ativo, nutrientes = requisicao.nutrientes, versao = versao + 1)
            itens = itens.map { if (it.id == id) novo else it }; return novo
        }
    }
    private fun TestScope.vm(f: Fake) = ProdutosViewModel(f, StandardTestDispatcher(testScheduler))

    @Test fun listaBuscaEAbaDeInativos() = runTest {
        val f = Fake(listOf(produto("Whey"), produto("Barra"), produto("Antigo", ativo = false))); val vm = vm(f)
        vm.carregar(); runCurrent(); assertEquals(listOf("Whey", "Barra"), vm.estado.value.lista.map { it.nome })
        vm.mudarBusca("bar"); runCurrent(); assertEquals(listOf("Barra"), vm.estado.value.lista.map { it.nome })
        vm.mudarBusca(""); vm.mudarAtivo(false); runCurrent(); assertEquals(listOf("Antigo"), vm.estado.value.lista.map { it.nome })
        assertTrue(f.buscas.last().let { !it.second })
    }
    @Test fun criarValidaOsQuatroMacrosEEnviaPorCemGramas() = runTest {
        val f = Fake(emptyList()); val vm = vm(f); vm.novo()
        vm.salvar(); runCurrent()
        assertEquals(MensagemAlimentos.OBRIGATORIO, vm.estado.value.errosCampos["nome"]); assertEquals(MensagemAlimentos.OBRIGATORIO, vm.estado.value.errosCampos["energia_kcal"])
        assertTrue(f.criados.isEmpty())
        vm.mudarCampo(CampoProduto.NOME, "Whey Max"); vm.mudarCampo(CampoProduto.ENERGIA, "400,5"); vm.mudarCampo(CampoProduto.CARBOIDRATO, "10")
        vm.mudarCampo(CampoProduto.PROTEINA, "80"); vm.mudarCampo(CampoProduto.LIPIDIO, "-1"); vm.salvar()
        assertEquals(MensagemAlimentos.CAMPO_INVALIDO, vm.estado.value.errosCampos["lipideos_g"])
        vm.mudarCampo(CampoProduto.LIPIDIO, "5"); vm.mudarCampo(CampoProduto.MEDIDA_GRAMAS, "30"); vm.salvar(); runCurrent()
        val c = f.criados.single()
        assertEquals(mapOf("energia_kcal" to 400.5, "carboidrato_g" to 10.0, "proteina_g" to 80.0, "lipideos_g" to 5.0), c.nutrientes); assertEquals(30.0, c.medidaGramas); assertNotNull(c.id)
        assertNull(vm.estado.value.formulario); assertEquals(MensagemAlimentos.SALVO, vm.estado.value.aviso); assertFalse(vm.estado.value.enviando)
    }
    @Test fun editarPreservaMicronutrientesEEnviaAVersao() = runTest {
        val p = produto("Whey", versao = 3); val f = Fake(listOf(p)); val vm = vm(f); vm.carregar(); runCurrent()
        vm.editar(p); vm.mudarCampo(CampoProduto.NOME, "Whey Novo"); vm.salvar(); runCurrent()
        val (id, versao, corpo) = f.alterados.single()
        assertEquals(p.id, id); assertEquals(3L, versao); assertEquals("Whey Novo", corpo.nome); assertEquals(120.0, corpo.nutrientes["calcio_mg"]); assertTrue(corpo.ativo)
    }
    @Test fun desativarPedeConfirmacaoEMandaAtivoFalso() = runTest {
        val p = produto("Whey"); val f = Fake(listOf(p)); val vm = vm(f); vm.carregar(); runCurrent()
        vm.pedirDesativacao(p); assertEquals(p, vm.estado.value.desativar); assertTrue(f.alterados.isEmpty())
        vm.confirmarDesativacao(); runCurrent()
        assertFalse(f.alterados.single().third.ativo); assertNull(vm.estado.value.desativar); assertTrue(vm.estado.value.lista.isEmpty())
    }
    @Test fun conflitoDeVersaoBloqueiaEOferecerDescartar() = runTest {
        val p = produto("Whey"); val f = Fake(listOf(p)); val vm = vm(f); vm.carregar(); runCurrent()
        vm.editar(p); f.falha = ErroApi("CONFLITO_VERSAO", null, emptyList(), 409, null, null); vm.salvar(); runCurrent()
        assertTrue(vm.estado.value.conflito); assertEquals(MensagemAlimentos.CONFLITO, vm.estado.value.erro)
        f.falha = null; vm.descartarEAtualizar(); runCurrent()
        assertNull(vm.estado.value.formulario); assertFalse(vm.estado.value.conflito)
    }
}
