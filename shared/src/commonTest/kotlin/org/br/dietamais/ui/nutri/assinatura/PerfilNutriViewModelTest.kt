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
        var logo: Pair<ByteArray, String>? = null; var logoRemovida = false
        override suspend fun enviarLogo(bytes: ByteArray, tipo: String) { falha?.let { throw it }; logo = bytes to tipo }
        override suspend fun removerLogo() { falha?.let { throw it }; logoRemovida = true }
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

    private val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3)
    private val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 1, 2, 3)

    @Test fun logoPngEJpegSaemComOTipoCerto() = runTest {
        val p = Porta(perfil()); val vm = vm(p); advanceUntilIdle()
        vm.enviarLogo(png); advanceUntilIdle()
        assertEquals("image/png", p.logo?.second); assertEquals(MensagemPerfilNutri.LOGO_ENVIADA, vm.estado.value.aviso)
        vm.enviarLogo(jpeg); advanceUntilIdle()
        assertEquals("image/jpeg", p.logo?.second)
    }

    @Test fun logoQueNaoEImagemOuPassaDoLimiteNaoVaiParaOServidor() = runTest {
        val p = Porta(perfil()); val vm = vm(p); advanceUntilIdle()
        vm.enviarLogo(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9)); advanceUntilIdle()
        assertEquals(MensagemPerfilNutri.LOGO_INVALIDA, vm.estado.value.erro); assertNull(p.logo)
        vm.enviarLogo(png + ByteArray(LIMITE_LOGO)); advanceUntilIdle()
        assertEquals(MensagemPerfilNutri.LOGO_GRANDE, vm.estado.value.erro); assertNull(p.logo)
        vm.enviarLogo(png + ByteArray(LIMITE_LOGO - png.size)); advanceUntilIdle()
        assertNotNull(p.logo, "exatamente 512 KB ainda passa")
    }

    @Test fun removerLogoAvisaEErroDeApiViraMensagem() = runTest {
        val p = Porta(perfil()); val vm = vm(p); advanceUntilIdle()
        vm.removerLogo(); advanceUntilIdle()
        assertTrue(p.logoRemovida); assertEquals(MensagemPerfilNutri.LOGO_REMOVIDA, vm.estado.value.aviso)
        p.falha = ErroApi("ASSINATURA_INATIVA", null, emptyList(), 403, null, null)
        vm.enviarLogo(png); advanceUntilIdle()
        assertEquals(MensagemPerfilNutri.ASSINATURA_INATIVA, vm.estado.value.erro)
    }

    @Test fun tipoDaImagemPelosBytes() {
        assertEquals("image/png", tipoDaImagem(png)); assertEquals("image/jpeg", tipoDaImagem(jpeg))
        assertNull(tipoDaImagem(byteArrayOf())); assertNull(tipoDaImagem("GIF89a....".encodeToByteArray()))
    }
}
