package org.br.dietamais.ui.paciente.perfil

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlinx.datetime.LocalTime
import org.br.dietamais.data.remoto.ErroApi
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class PerfilPacienteViewModelTest {
    private val padrao = PreferenciasNotificacaoResposta(
        listOf(PreferenciaTipo(TipoNotificacao.NOVA_DIETA, true), PreferenciaTipo(TipoNotificacao.CONSULTA_AMANHA, true)),
        SilencioNotificacao(true, LocalTime(22, 0), LocalTime(7, 0)), 0)

    private class Prefs(var servidor: PreferenciasNotificacaoResposta) : PortaPreferencias {
        val cache = MutableStateFlow<PreferenciasNotificacaoResposta?>(null); val lembretes = MutableStateFlow(true)
        var falha: Exception? = null; var enviado: AtualizarPreferenciasRequisicao? = null
        override fun observar() = cache
        override fun observarLembretes() = lembretes
        override fun definirLembretes(ativo: Boolean) { lembretes.value = ativo }
        override suspend fun atualizar() = servidor.also { cache.value = it }
        override suspend fun salvar(requisicao: AtualizarPreferenciasRequisicao): PreferenciasNotificacaoResposta {
            falha?.let { throw it }; enviado = requisicao
            return PreferenciasNotificacaoResposta(requisicao.tipos, requisicao.silencio, servidor.versao + 1).also { servidor = it; cache.value = it }
        }
    }
    private class Cal : PortaCalendario {
        var gerados = 0; var revogados = 0
        override suspend fun gerar() = CalendarioTokenResposta("t", "https://x/cal/t.ics", "webcal://x/cal/t.ics").also { gerados++ }
        override suspend fun revogar() { revogados++ }
    }
    private val semVinculos = object : PortaVinculosLocais { override fun observarVinculos() = flowOf(emptyList<VinculoResposta>()) }

    private fun TestScope.vm(prefs: Prefs, cal: Cal = Cal(), online: Boolean = true) =
        PerfilPacienteViewModel(prefs, semVinculos, cal, StandardTestDispatcher(testScheduler), flowOf(online))

    @Test fun carregaDoServidorEEditaSemPerderAoRecarregarOCache() = runTest {
        val vm = vm(Prefs(padrao)); advanceUntilIdle()
        assertEquals(2, vm.estado.value.tipos.size); assertEquals("22:00", vm.estado.value.inicio); assertFalse(vm.estado.value.alterado)
        vm.mudarTipo(TipoNotificacao.NOVA_DIETA, false); assertTrue(vm.estado.value.alterado)
        assertFalse(vm.estado.value.tipos.first { it.tipo == TipoNotificacao.NOVA_DIETA }.ativo)
    }

    @Test fun salvaEEnviaVersaoNova() = runTest {
        val prefs = Prefs(padrao); val vm = vm(prefs); advanceUntilIdle()
        vm.mudarSilencioAtivo(false); vm.mudarInicio("23:00"); vm.salvar(); advanceUntilIdle()
        assertEquals(false, prefs.enviado?.silencio?.ativo); assertEquals(LocalTime(23, 0), prefs.enviado?.silencio?.inicio)
        assertEquals(1, vm.estado.value.versao); assertEquals(MensagemPerfil.SALVO, vm.estado.value.aviso); assertFalse(vm.estado.value.alterado)
    }

    @Test fun validaHorarios() = runTest {
        val prefs = Prefs(padrao); val vm = vm(prefs); advanceUntilIdle()
        vm.mudarInicio("25:00"); vm.salvar(); advanceUntilIdle()
        assertEquals(MensagemPerfil.HORARIO_INVALIDO, vm.estado.value.erro)
        vm.mudarInicio("07:00"); vm.mudarFim("07:00"); vm.salvar(); advanceUntilIdle()
        assertEquals(MensagemPerfil.SILENCIO_IGUAL, vm.estado.value.erro); assertNull(prefs.enviado)
    }

    @Test fun conflitoDeVersaoViraMensagem() = runTest {
        val prefs = Prefs(padrao); val vm = vm(prefs); advanceUntilIdle()
        prefs.falha = ErroApi("CONFLITO_VERSAO", null, emptyList(), 409, null, null)
        vm.mudarTipo(TipoNotificacao.NOVA_DIETA, false); vm.salvar(); advanceUntilIdle()
        assertEquals(MensagemPerfil.CONFLITO, vm.estado.value.erro)
    }

    @Test fun lembreteLocalFuncionaOfflineELinkExigeRede() = runTest {
        val prefs = Prefs(padrao); val cal = Cal(); val vm = vm(prefs, cal, online = false); advanceUntilIdle()
        vm.mudarLembretes(false); advanceUntilIdle()
        assertFalse(vm.estado.value.lembretes)
        vm.gerarLink(); advanceUntilIdle()
        assertEquals(0, cal.gerados); assertEquals(MensagemPerfil.SEM_CONEXAO, vm.estado.value.erro)
    }

    @Test fun geraERevogaOLink() = runTest {
        val cal = Cal(); val vm = vm(Prefs(padrao), cal); advanceUntilIdle()
        vm.gerarLink(); advanceUntilIdle(); assertNotNull(vm.estado.value.calendario)
        vm.revogarLink(); advanceUntilIdle(); assertNull(vm.estado.value.calendario); assertEquals(1, cal.revogados)
    }
}
