package org.br.dietamais.ui.nutri.assinatura

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.br.dietamais.data.remoto.ErroApi
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.PortaAssinatura
import kotlin.test.*
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class AssinaturaViewModelTest {
    private val basico = PlanoResposta("BASICO_20", "Básico", 29.9, 20)
    private fun assinatura(status: StatusAssinatura = StatusAssinatura.TESTE, pagamento: Boolean = true) =
        AssinaturaResposta(Uuid.random(), PlanoResposta("TESTE", "Teste", 0.0, 5), status, 2, null, null, pagamento, listOf(basico))
    private class Porta(var atual: AssinaturaResposta) : PortaAssinatura {
        var falha: Exception? = null; var url = "https://pagamento.exemplo/abc"; var pedidos = mutableListOf<String>()
        override suspend fun obter() = atual
        override suspend fun pedirCheckout(plano: String): CheckoutResposta { falha?.let { throw it }; pedidos += plano; return CheckoutResposta(url) }
    }
    private fun TestScope.vm(p: Porta, online: Boolean = true) = AssinaturaViewModel(p, StandardTestDispatcher(testScheduler), flowOf(online))

    @Test fun carregaEPedeCheckoutDeUmPlanoOferecido() = runTest {
        val p = Porta(assinatura()); val vm = vm(p); advanceUntilIdle()
        assertTrue(vm.estado.value.carregado)
        vm.assinar("BASICO_20"); advanceUntilIdle()
        assertEquals("https://pagamento.exemplo/abc", vm.estado.value.urlCheckout); vm.urlAberta(); assertNull(vm.estado.value.urlCheckout)
    }

    @Test fun ignoraPlanoForaDaListaOuPagamentoIndisponivel() = runTest {
        val p = Porta(assinatura()); val vm = vm(p); advanceUntilIdle()
        vm.assinar("OUTRO"); advanceUntilIdle(); assertTrue(p.pedidos.isEmpty())
        p.atual = assinatura(pagamento = false); vm.recarregar(); advanceUntilIdle()
        vm.assinar("BASICO_20"); advanceUntilIdle(); assertTrue(p.pedidos.isEmpty())
    }

    @Test fun recusaUrlSemHttps() = runTest {
        val p = Porta(assinatura()); p.url = "http://inseguro.exemplo"; val vm = vm(p); advanceUntilIdle()
        vm.assinar("BASICO_20"); advanceUntilIdle()
        assertNull(vm.estado.value.urlCheckout); assertEquals(MensagemAssinatura.ERRO, vm.estado.value.erro)
    }

    @Test fun jaAtivaRecarregaEMostraAviso() = runTest {
        val p = Porta(assinatura()); val vm = vm(p); advanceUntilIdle()
        p.falha = ErroApi("ASSINATURA_JA_ATIVA", null, emptyList(), 409, null, null); p.atual = assinatura(StatusAssinatura.ATIVA)
        vm.assinar("BASICO_20"); advanceUntilIdle()
        assertEquals(MensagemAssinatura.JA_ATIVA, vm.estado.value.erro); assertEquals(StatusAssinatura.ATIVA, vm.estado.value.assinatura?.status)
    }

    @Test fun somenteLeituraEAtrasoSaoDerivadosDoStatus() = runTest {
        val p = Porta(assinatura(StatusAssinatura.CANCELADA)); val vm = vm(p); advanceUntilIdle()
        assertTrue(vm.estado.value.somenteLeitura)
        p.atual = assinatura(StatusAssinatura.EM_ATRASO); vm.recarregar(); advanceUntilIdle()
        assertTrue(vm.estado.value.emAtraso); assertFalse(vm.estado.value.somenteLeitura)
    }

    @Test fun semConexaoNaoChamaAApi() = runTest {
        val vm = vm(Porta(assinatura()), online = false); advanceUntilIdle()
        assertFalse(vm.estado.value.carregado); assertEquals(MensagemAssinatura.SEM_CONEXAO, vm.estado.value.erro)
    }

    @Test fun formataPreco() { assertEquals("R$ 29,90", preco(29.9)); assertEquals("R$ 100,05", preco(100.05)) }
}
