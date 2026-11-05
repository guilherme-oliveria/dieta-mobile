package org.br.dietamais.dominio.agenda

import org.br.dietamais.data.remoto.dto.*
import kotlin.test.*
import kotlin.uuid.Uuid

class FinanceiroConsultaTest {
    @Test fun leValorComVirgulaOuPontoERecusaOResto() {
        assertEquals(150.0, lerValorReais("150")); assertEquals(150.5, lerValorReais("150,5")); assertEquals(150.5, lerValorReais(" 150.50 "))
        assertNull(lerValorReais("")); assertNull(lerValorReais("-1")); assertNull(lerValorReais("12,345")); assertNull(lerValorReais("abc")); assertNull(lerValorReais("123456789"))
    }

    @Test fun formataReaisComMilharEDuasCasas() {
        assertEquals("R$ 0,00", formatarReais(0.0)); assertEquals("R$ 150,50", formatarReais(150.5)); assertEquals("R$ 1.234,56", formatarReais(1234.56)); assertEquals("R$ 1.000.000,00", formatarReais(1_000_000.0))
        assertEquals("250", textoDeValor(250.0)); assertEquals("150,50", textoDeValor(150.5)); assertEquals("0,05", textoDeValor(0.05))
    }

    @Test fun pagoExigeValorFormaEComparecimento() {
        assertEquals(emptySet(), erroDeFinanceiro(FormularioFinanceiro()))
        assertEquals(emptySet(), erroDeFinanceiro(FormularioFinanceiro(ComparecimentoConsulta.FALTOU, "", null, false)))
        assertEquals(setOf(ErroFinanceiro.PAGO_SEM_VALOR, ErroFinanceiro.PAGO_SEM_FORMA), erroDeFinanceiro(FormularioFinanceiro(ComparecimentoConsulta.COMPARECEU, "", null, true)))
        assertEquals(setOf(ErroFinanceiro.PAGO_SEM_COMPARECIMENTO), erroDeFinanceiro(FormularioFinanceiro(ComparecimentoConsulta.PENDENTE, "100", FormaPagamento.PIX, true)))
        assertEquals(setOf(ErroFinanceiro.VALOR_INVALIDO), erroDeFinanceiro(FormularioFinanceiro(ComparecimentoConsulta.COMPARECEU, "dez", FormaPagamento.PIX, true)))
        assertEquals(emptySet(), erroDeFinanceiro(FormularioFinanceiro(ComparecimentoConsulta.COMPARECEU, "100", FormaPagamento.PIX, true)))
    }

    @Test fun montaARequisicaoEVoltaParaOFormulario() {
        val f = FormularioFinanceiro(ComparecimentoConsulta.COMPARECEU, "150,5", FormaPagamento.CARTAO, true)
        assertEquals(AtualizarFinanceiroConsultaRequisicao(ComparecimentoConsulta.COMPARECEU, 150.5, FormaPagamento.CARTAO, true), requisicaoDeFinanceiro(f))
        val volta = formularioDeFinanceiro(FinanceiroConsultaResposta(Uuid.random(), ComparecimentoConsulta.COMPARECEU, 150.5, FormaPagamento.CARTAO, true, 2))
        assertEquals(FormularioFinanceiro(ComparecimentoConsulta.COMPARECEU, "150,50", FormaPagamento.CARTAO, true), volta)
        assertEquals(FormularioFinanceiro(), formularioDeFinanceiro(FinanceiroConsultaResposta(Uuid.random(), ComparecimentoConsulta.OUTRO)))
    }
}
