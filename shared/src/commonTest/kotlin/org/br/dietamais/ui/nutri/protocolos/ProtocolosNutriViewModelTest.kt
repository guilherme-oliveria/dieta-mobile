package org.br.dietamais.ui.nutri.protocolos

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.br.dietamais.data.remoto.ErroApi
import org.br.dietamais.data.remoto.FalhaComunicacao
import org.br.dietamais.data.remoto.dto.ProtocolosNutriResposta
import org.br.dietamais.data.remoto.dto.SalvarProtocolosNutriRequisicao
import org.br.dietamais.data.repositorio.PortaProtocolosNutri
import org.br.dietamais.dominio.calculos.ProtocoloDobras
import org.br.dietamais.dominio.calculos.ProtocoloMassa
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ProtocolosNutriViewModelTest {
    private class Porta(var atual: ProtocolosNutriResposta = ProtocolosNutriResposta("SETE", "MAGRO", 0)) : PortaProtocolosNutri {
        var falha: Exception? = null; var enviado: Pair<Long, SalvarProtocolosNutriRequisicao>? = null
        override suspend fun obter(): ProtocolosNutriResposta { falha?.let { throw it }; return atual }
        override suspend fun salvar(versao: Long, requisicao: SalvarProtocolosNutriRequisicao): ProtocolosNutriResposta {
            falha?.let { throw it }; enviado = versao to requisicao
            return ProtocolosNutriResposta(requisicao.dobras, requisicao.massa, versao + 1).also { atual = it }
        }
    }
    private fun TestScope.vm(p: Porta, online: Boolean = true) = ProtocolosNutriViewModel(p, StandardTestDispatcher(testScheduler), flowOf(online))

    @Test fun carregaOPadraoEOValorDesconhecidoCaiNoPadraoDoServidor() = runTest {
        val vm = vm(Porta(ProtocolosNutriResposta("TRES", "MUSCULAR", 3))); advanceUntilIdle()
        assertEquals(ProtocoloDobras.TRES, vm.estado.value.dobras); assertEquals(ProtocoloMassa.MUSCULAR, vm.estado.value.massa); assertEquals(3, vm.estado.value.versao)
        val outro = vm(Porta(ProtocolosNutriResposta("DEZ", "NOVO", 1))); advanceUntilIdle()
        assertEquals(ProtocoloDobras.SETE, outro.estado.value.dobras); assertEquals(ProtocoloMassa.MAGRO, outro.estado.value.massa)
    }

    @Test fun salvaComAVersaoEAvisa() = runTest {
        val p = Porta(); val vm = vm(p); advanceUntilIdle()
        vm.salvar(); advanceUntilIdle(); assertNull(p.enviado) // nada mudou
        vm.mudarDobras(ProtocoloDobras.TRES); vm.mudarMassa(ProtocoloMassa.MUSCULAR); vm.salvar(); advanceUntilIdle()
        assertEquals(0L to SalvarProtocolosNutriRequisicao("TRES", "MUSCULAR"), p.enviado)
        assertEquals(MensagemProtocolos.SALVO, vm.estado.value.aviso); assertEquals(1, vm.estado.value.versao); assertFalse(vm.estado.value.alterado)
    }

    @Test fun offlineNaoSalvaEConflitoRecarrega() = runTest {
        val p = Porta(); val offline = vm(p, online = false); advanceUntilIdle()
        offline.mudarDobras(ProtocoloDobras.TRES); offline.salvar(); advanceUntilIdle()
        assertNull(p.enviado); assertEquals(MensagemProtocolos.SEM_CONEXAO, offline.estado.value.erro)
        val vm = vm(p); advanceUntilIdle(); vm.mudarMassa(ProtocoloMassa.MUSCULAR)
        p.atual = ProtocolosNutriResposta("TRES", "MAGRO", 5)
        p.falha = ErroApi("CONFLITO_VERSAO", "RN-SYN-02", emptyList(), 409, null, null); vm.salvar(); advanceUntilIdle()
        assertEquals(MensagemProtocolos.CONFLITO, vm.estado.value.erro)
    }

    @Test fun padraoDoFormularioNuloSemConexaoOuComFalha() = runTest {
        val p = Porta(ProtocolosNutriResposta("TRES", "MUSCULAR", 2))
        assertEquals(ProtocoloDobras.TRES to ProtocoloMassa.MUSCULAR, protocolosPadraoDe(p)())
        p.falha = FalhaComunicacao(); assertNull(protocolosPadraoDe(p)())
    }
}
