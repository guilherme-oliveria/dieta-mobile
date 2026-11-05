package org.br.dietamais.dominio.pacientes

import org.br.dietamais.data.remoto.dto.CadastroAmpliadoDto
import org.br.dietamais.data.remoto.dto.EnderecoDto
import org.br.dietamais.data.remoto.JsonContrato
import kotlin.test.*

class CadastroAmpliadoTest {
    @Test fun tudoVazioNaoMandaNada() {
        assertNull(montarCadastroAmpliado(FormularioCadastroAmpliado()))
        assertNull(montarCadastroAmpliado(FormularioCadastroAmpliado(profissao = "   ", cep = " ")))
    }

    @Test fun montaSoOQueFoiPreenchidoComUfEmMaiusculasECepSoComDigitos() {
        val dto = montarCadastroAmpliado(FormularioCadastroAmpliado(profissao = " Professora ", logradouro = "Rua A", numero = "10", uf = "sp", cep = "01310-100", motivoConsulta = "Emagrecer"))
        assertEquals(CadastroAmpliadoDto(profissao = "Professora", motivoConsulta = "Emagrecer", endereco = EnderecoDto(logradouro = "Rua A", numero = "10", uf = "SP", cep = "01310100")), dto)
        assertNull(dto?.endereco?.cidade)
    }

    @Test fun validaUfECep() {
        assertEquals(emptySet(), errosDoCadastroAmpliado(FormularioCadastroAmpliado()))
        assertEquals(emptySet(), errosDoCadastroAmpliado(FormularioCadastroAmpliado(uf = "rj", cep = "20040-020")))
        assertEquals(setOf(ErroCadastroAmpliado.UF, ErroCadastroAmpliado.CEP), errosDoCadastroAmpliado(FormularioCadastroAmpliado(uf = "R", cep = "123")))
        assertEquals(setOf(ErroCadastroAmpliado.UF), errosDoCadastroAmpliado(FormularioCadastroAmpliado(uf = "12")))
    }

    @Test fun enderecoEmUmaLinha() {
        assertNull(enderecoEmUmaLinha(null)); assertNull(enderecoEmUmaLinha(EnderecoDto()))
        assertEquals("Rua A, 10 - ap 2 - Centro - Santos/SP - CEP 11010-000", enderecoEmUmaLinha(EnderecoDto("Rua A", "10", "ap 2", "Centro", "Santos", "SP", "11010000")))
        assertEquals("Santos/SP", enderecoEmUmaLinha(EnderecoDto(cidade = "Santos", uf = "SP")))
    }

    @Test fun naoMostraDadosPessoaisNoToStringEIgnoraCampoDesconhecido() {
        assertFalse(CadastroAmpliadoDto(profissao = "Segredo").toString().contains("Segredo")); assertFalse(EnderecoDto(logradouro = "Segredo").toString().contains("Segredo"))
        val lido = JsonContrato.decodeFromString<CadastroAmpliadoDto>("""{"profissao":"Professora","novoCampo":1,"endereco":{"uf":"SP","outro":2}}""")
        assertEquals("Professora", lido.profissao); assertEquals("SP", lido.endereco?.uf)
    }
}
