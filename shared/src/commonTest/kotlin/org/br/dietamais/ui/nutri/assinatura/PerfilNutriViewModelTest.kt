package org.br.dietamais.ui.nutri.assinatura

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.br.dietamais.data.remoto.ErroApi
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.PortaPerfilNutri
import kotlin.test.*
import kotlin.time.Instant
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class PerfilNutriViewModelTest {
    private fun perfil(versao: Long = 0, nome: String = "Ana") = PerfilNutriResposta(Uuid.random(), nome, "CRN-3 12345", null, null, versao, Instant.parse("2026-01-01T00:00:00Z"))
    private class Porta(var atual: PerfilNutriResposta) : PortaPerfilNutri {
        var falha: Exception? = null; var enviado: AtualizarPerfilNutriRequisicao? = null; var versaoEnviada: Long? = null
        override suspend fun obter() = atual
        override suspend fun salvar(versao: Long, requisicao: AtualizarPerfilNutriRequisicao): PerfilNutriResposta {
            falha?.let { throw it }; enviado = requisicao; versaoEnviada = versao
            return atual.copy(nome = requisicao.nome, crn = requisicao.crn, telefone = requisicao.telefone, orientacaoPadrao = requisicao.orientacaoPadrao, versao = versao + 1).also { atual = it }
        }
    }
    private fun TestScope.vm(p: Porta, online: Boolean = true) = PerfilNutriViewModel(p, StandardTestDispatcher(testScheduler), flowOf(online))

    @Test fun carregaEnviaComVersaoENormalizaVazios() = runTest {
        val p = Porta(perfil(3)); val vm = vm(p); advanceUntilIdle()
        assertTrue(vm.estado.value.carregado); assertEquals("Ana", vm.estado.value.nome)
        vm.mudarTelefone("  "); vm.mudarOrientacao("Beba água"); vm.salvar(); advanceUntilIdle()
        assertEquals(3L, p.versaoEnviada); assertNull(p.enviado?.telefone); assertEquals("Beba água", p.enviado?.orientacaoPadrao)
        assertEquals(4L, vm.estado.value.versao); assertEquals(MensagemPerfilNutri.SALVO, vm.estado.value.aviso)
    }

    @Test fun validaNomeECrnAntesDeEnviar() = runTest {
        val p = Porta(perfil()); val vm = vm(p); advanceUntilIdle()
        vm.mudarNome(" "); vm.mudarCrn("12345"); vm.salvar(); advanceUntilIdle()
        assertEquals(setOf("nome", "crn"), vm.estado.value.errosCampos.keys); assertNull(p.enviado)
    }

    @Test fun conflitoRecarregaOServidor() = runTest {
        val p = Porta(perfil(5, "Outra")); val vm = vm(p); advanceUntilIdle()
        p.falha = ErroApi("CONFLITO_VERSAO", null, emptyList(), 409, null, null)
        vm.mudarNome("Ana Maria"); vm.salvar(); advanceUntilIdle()
        assertEquals(MensagemPerfilNutri.CONFLITO, vm.estado.value.erro); assertEquals("Outra", vm.estado.value.nome)
    }

    @Test fun semConexaoNaoChamaAApi() = runTest {
        val p = Porta(perfil()); val vm = vm(p, online = false); advanceUntilIdle()
        assertFalse(vm.estado.value.carregado); assertEquals(MensagemPerfilNutri.SEM_CONEXAO, vm.estado.value.erro)
    }
}
