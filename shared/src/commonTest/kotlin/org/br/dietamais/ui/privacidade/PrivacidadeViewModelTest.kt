package org.br.dietamais.ui.privacidade

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.br.dietamais.data.remoto.ErroApi
import org.br.dietamais.data.remoto.dto.ExportacaoResposta
import org.br.dietamais.data.repositorio.PortaPrivacidade
import kotlin.test.*
import kotlin.time.Instant
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class PrivacidadeViewModelTest {
    private fun exportacao(status: String, url: String? = null) = ExportacaoResposta(Uuid.random(), status, Instant.parse("2026-10-28T10:00:00Z"), url)
    private class Porta : PortaPrivacidade {
        var atual: ExportacaoResposta? = null; var falha: Exception? = null; var senhaRecebida: String? = null
        override suspend fun exportacao() = atual
        override suspend fun pedirExportacao(): ExportacaoResposta { falha?.let { throw it }; return ExportacaoResposta(Uuid.random(), "PENDENTE", Instant.parse("2026-10-28T10:00:00Z")).also { atual = it } }
        override suspend fun excluirConta(senha: String) { falha?.let { throw it }; senhaRecebida = senha }
        override suspend fun urlTermos() = "https://termos.exemplo"
    }
    private fun TestScope.vm(p: Porta, online: Boolean = true, exportacao: Boolean = true) = PrivacidadeViewModel(p, StandardTestDispatcher(testScheduler), flowOf(online), exportacao)
    private fun erro(codigo: String, status: Int) = ErroApi(codigo, null, emptyList(), status, null, null)

    @Test fun pedeExportacaoEMostraAviso() = runTest {
        val vm = vm(Porta()); advanceUntilIdle()
        vm.pedirExportacao(); advanceUntilIdle()
        assertEquals("PENDENTE", vm.estado.value.exportacao?.status); assertEquals(MensagemPrivacidade.PEDIDO_ENVIADO, vm.estado.value.aviso)
    }

    @Test fun exportacaoRecenteMostraMensagem() = runTest {
        val p = Porta(); val vm = vm(p); advanceUntilIdle()
        p.falha = erro("EXPORTACAO_EM_ANDAMENTO", 409); vm.pedirExportacao(); advanceUntilIdle()
        assertEquals(MensagemPrivacidade.EXPORTACAO_EM_ANDAMENTO, vm.estado.value.erro)
    }

    @Test fun soAbreLinkQuandoProntaENaoConsultaParaNutri() = runTest {
        val p = Porta(); p.atual = exportacao("PENDENTE", "https://x.exemplo/a"); val vm = vm(p); advanceUntilIdle()
        vm.abrirExportacao(); assertNull(vm.estado.value.urlParaAbrir)
        p.atual = exportacao("PRONTA", "https://x.exemplo/a"); vm.atualizarExportacao(); advanceUntilIdle()
        vm.abrirExportacao(); assertEquals("https://x.exemplo/a", vm.estado.value.urlParaAbrir)
        val nutri = vm(Porta().also { it.atual = exportacao("PRONTA") }, exportacao = false); advanceUntilIdle()
        assertNull(nutri.estado.value.exportacao)
    }

    @Test fun exclusaoExigeSenhaEMarcaContaExcluida() = runTest {
        val p = Porta(); val vm = vm(p); advanceUntilIdle()
        vm.pedirExclusao(); vm.confirmarExclusao(); advanceUntilIdle()
        assertEquals(MensagemPrivacidade.SENHA_OBRIGATORIA, vm.estado.value.erro); assertNull(p.senhaRecebida)
        vm.mudarSenha("segredo-fake"); vm.confirmarExclusao(); advanceUntilIdle()
        assertEquals("segredo-fake", p.senhaRecebida); assertTrue(vm.estado.value.contaExcluida); assertEquals("", vm.estado.value.senha)
    }

    @Test fun senhaErradaMantemODialogo() = runTest {
        val p = Porta(); val vm = vm(p); advanceUntilIdle()
        p.falha = erro("CREDENCIAIS_INVALIDAS", 401); vm.pedirExclusao(); vm.mudarSenha("errada"); vm.confirmarExclusao(); advanceUntilIdle()
        assertEquals(MensagemPrivacidade.SENHA_INCORRETA, vm.estado.value.erro); assertTrue(vm.estado.value.confirmandoExclusao); assertFalse(vm.estado.value.contaExcluida)
    }

    @Test fun semConexaoNaoExcluiNemExporta() = runTest {
        val p = Porta(); val vm = vm(p, online = false); advanceUntilIdle()
        vm.pedirExclusao(); vm.mudarSenha("x"); vm.confirmarExclusao(); advanceUntilIdle()
        assertNull(p.senhaRecebida); assertEquals(MensagemPrivacidade.SEM_CONEXAO, vm.estado.value.erro)
    }
}
