package org.br.dietamais.dominio.avaliacao

import kotlin.test.*
import kotlinx.datetime.LocalDate
import kotlin.time.Instant
import org.br.dietamais.data.remoto.dto.*
import kotlin.uuid.Uuid

class FormularioInfantilTest {
    private val data = Instant.parse("2026-11-02T02:30:00Z") // ainda 01/11 em São Paulo

    @Test fun idadeUsaDataDeSaoPauloEMesesCompletos() {
        assertEquals(119, idadeAvaliacaoMeses(LocalDate(2016, 11, 2), data))
        assertEquals(120, idadeAvaliacaoMeses(LocalDate(2016, 11, 1), data))
        assertTrue(ehAvaliacaoInfantil(LocalDate(2008, 11, 2), data))
        assertFalse(ehAvaliacaoInfantil(LocalDate(2008, 11, 1), data))
        assertNull(idadeAvaliacaoMeses(LocalDate(2026, 11, 2), data))
        assertNull(idadeAvaliacaoMeses(null, data))
    }

    @Test fun lactenteAceitaCentimetrosEConverteParaMilimetrosSemPerderPrecisao() {
        val valores = mapOf(MedidaAvaliacao.PESO to "3,25", MedidaAvaliacao.ESTATURA to "48,5", MedidaAvaliacao.BRACO to "9,5")
        assertTrue(validarMedidas(valores, infantil = true).isEmpty())
        val servidor = medidasInfantisParaServidor(valores)
        assertEquals(0.485, lerDecimal(servidor[MedidaAvaliacao.ESTATURA].orEmpty()))
        assertEquals("3,25", servidor[MedidaAvaliacao.PESO])
        assertTrue(validarMedidas(valores).isNotEmpty())
        assertEquals(ErroMedida.CASAS_DECIMAIS, validarMedidas(valores + (MedidaAvaliacao.ESTATURA to "48,51"), infantil = true)[MedidaAvaliacao.ESTATURA])
    }

    @Test fun pesoAoNascerEmKgViraInteiroEmGramasEFatorNaoEInferido() {
        val f = FormularioInfantil(pesoNascerKg = "3,301", modoCurva = "ESCORE_Z", etniaSlaughter = "BRANCA")
        assertTrue(validarFormularioInfantil(f).isEmpty())
        val enviado = dadosInfantisParaEnviar(f)
        assertEquals(3301, enviado.pesoNascerGramas)
        assertNull(enviado.fatorAtividade)
        assertEquals("ESCORE_Z", enviado.modoCurva)
        assertEquals("BRANCA", enviado.etniaSlaughter)
    }

    @Test fun opcionaisVaziosNaoViraramZeroEInvalidosNaoSaoEnviados() {
        val vazio = dadosInfantisParaEnviar(FormularioInfantil())
        assertNull(vazio.pesoNascerGramas)
        assertNull(vazio.fatorAtividade)
        assertEquals(ErroMedida.FORA_DO_INTERVALO, validarFormularioInfantil(FormularioInfantil(pesoNascerKg = "0"))[CampoInfantil.PESO_NASCER])
        assertEquals(ErroMedida.CASAS_DECIMAIS, validarFormularioInfantil(FormularioInfantil(pesoNascerKg = "3,3001"))[CampoInfantil.PESO_NASCER])
        assertEquals(ErroMedida.FORA_DO_INTERVALO, validarFormularioInfantil(FormularioInfantil(fatorAtividade = "10"))[CampoInfantil.FATOR_ATIVIDADE])
        assertFailsWith<IllegalArgumentException> { dadosInfantisParaEnviar(FormularioInfantil(pesoNascerKg = "abc")) }
    }

    @Test fun compararEstaturasInfantisNaoApagaDiferencaDeMilimetros() {
        val id = Uuid.random()
        val a = AvaliacaoResposta(id, id, id, data, 10.0, 0.811, 15.2, ClassificacaoImc.NORMAL,
            versao = 1, criadoEm = data, atualizadoEm = data, faixaEtaria = FaixaEtariaAvaliacao.CRIANCA)
        val b = a.copy(id = Uuid.random(), estaturaM = 0.814, dataHora = data + kotlin.time.Duration.parse("24h"))
        val variacao = compararAvaliacoes(a, b).single { it.medida == MedidaAvaliacao.ESTATURA }
        assertEquals(81.1, variacao.antes)
        assertEquals(81.4, variacao.depois)
        assertEquals(0.3, variacao.delta)
    }
}
