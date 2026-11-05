package org.br.dietamais.ui.nutri.agenda

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import kotlinx.datetime.TimeZone
import org.br.dietamais.data.remoto.ErroApi
import org.br.dietamais.data.remoto.FalhaComunicacao
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.PortaFinanceiro
import kotlin.test.*
import kotlin.time.Instant
import kotlin.uuid.Uuid

private class PortaFinanceiroFalsa(var atual: FinanceiroConsultaResposta? = null, var falha: Exception? = null) : PortaFinanceiro {
    val salvos = mutableListOf<Pair<Long, AtualizarFinanceiroConsultaRequisicao>>()
    var janela: Pair<Instant, Instant>? = null
    override suspend fun da(consulta: Uuid) = atual ?: FinanceiroConsultaResposta(consulta, ComparecimentoConsulta.PENDENTE)
    override suspend fun salvar(consulta: Uuid, versao: Long, requisicao: AtualizarFinanceiroConsultaRequisicao): FinanceiroConsultaResposta {
        falha?.let { throw it }
        salvos += versao to requisicao
        return FinanceiroConsultaResposta(consulta, requisicao.comparecimento, requisicao.valor, requisicao.formaPagamento, requisicao.pago, versao + 1)
    }
    override suspend fun resumo(de: Instant, ate: Instant): ResumoFinanceiroResposta { janela = de to ate; return ResumoFinanceiroResposta(de, ate, consultas = 3, recebido = 200.0) }
}

@OptIn(ExperimentalCoroutinesApi::class)
class FinanceiroViewModelTest {
    private val fuso = TimeZone.of("America/Sao_Paulo")
    private val consulta = consultaFalsa(Uuid.random(), agoraFalso)
    private fun TestScope.vm(p: PortaFinanceiroFalsa, online: Boolean = true) = FinanceiroViewModel(p, StandardTestDispatcher(testScheduler), flowOf(online), { agoraFalso }, { fuso })

    @Test fun abreCarregaOFinanceiroDaConsultaEMostraOFormulario() = runTest {
        val p = PortaFinanceiroFalsa(FinanceiroConsultaResposta(consulta.id, ComparecimentoConsulta.COMPARECEU, 150.5, FormaPagamento.PIX, false, 3))
        val vm = vm(p); vm.abrir(consulta, "Ana"); advanceUntilIdle()
        val e = vm.estado.value
        assertTrue(e.aberto); assertEquals("Ana", e.paciente); assertEquals(3, e.versao); assertEquals("150,50", e.formulario.valor); assertFalse(e.carregando)
    }

    @Test fun salvaComAVersaoLidaEAtualizaOResumoDoMes() = runTest {
        val p = PortaFinanceiroFalsa(); val vm = vm(p); vm.abrir(consulta, null); advanceUntilIdle()
        vm.mudarComparecimento(ComparecimentoConsulta.COMPARECEU); vm.mudarValor("200"); vm.mudarForma(FormaPagamento.PIX); vm.mudarPago(true); vm.salvar(); advanceUntilIdle()
        assertEquals(listOf(0L to AtualizarFinanceiroConsultaRequisicao(ComparecimentoConsulta.COMPARECEU, 200.0, FormaPagamento.PIX, true)), p.salvos)
        val e = vm.estado.value
        assertFalse(e.aberto); assertEquals(MensagemFinanceiro.SALVO, e.aviso); assertEquals(3, e.resumo?.consultas); assertNotNull(p.janela)
        // outubro de 2026 em São Paulo (UTC-3): de 01/10 03:00Z a 01/11 03:00Z... o mês corrente do relógio falso
        assertTrue(p.janela!!.second > p.janela!!.first)
    }

    @Test fun naoEnviaPagoSemFormaENemChamaAApi() = runTest {
        val p = PortaFinanceiroFalsa(); val vm = vm(p); vm.abrir(consulta, null); advanceUntilIdle()
        vm.mudarComparecimento(ComparecimentoConsulta.COMPARECEU); vm.mudarValor("100"); vm.mudarPago(true); vm.salvar(); advanceUntilIdle()
        assertTrue(p.salvos.isEmpty()); assertEquals(setOf(org.br.dietamais.dominio.agenda.ErroFinanceiro.PAGO_SEM_FORMA), vm.estado.value.erros); assertEquals(MensagemFinanceiro.DADOS_INVALIDOS, vm.estado.value.erro)
        assertTrue(vm.estado.value.aberto)
    }

    @Test fun traduzOsErrosDoServidorPeloCodigo() = runTest {
        suspend fun TestScope.tentar(falha: Exception): MensagemFinanceiro? {
            val p = PortaFinanceiroFalsa(falha = falha); val vm = vm(p); vm.abrir(consulta, null); advanceUntilIdle()
            vm.mudarComparecimento(ComparecimentoConsulta.FALTOU); vm.salvar(); advanceUntilIdle(); return vm.estado.value.erro
        }
        fun api(codigo: String, status: Int) = ErroApi(codigo, null, emptyList(), status, null, null)
        assertEquals(MensagemFinanceiro.CONFLITO, tentar(api("CONFLITO_VERSAO", 409)))
        assertEquals(MensagemFinanceiro.CANCELADA, tentar(api("CONSULTA_CANCELADA", 422)))
        assertEquals(MensagemFinanceiro.VINCULO_ENCERRADO, tentar(api("ACESSO_NEGADO", 403)))
        assertEquals(MensagemFinanceiro.SEM_CONEXAO, tentar(FalhaComunicacao()))
    }

    @Test fun semConexaoNaoEnviaEFecharLimpa() = runTest {
        val p = PortaFinanceiroFalsa(); val vm = vm(p, online = false); advanceUntilIdle()
        assertTrue(vm.estado.value.semConexao)
        vm.abrir(consulta, null); advanceUntilIdle(); vm.fechar(); assertFalse(vm.estado.value.aberto)
    }
}
