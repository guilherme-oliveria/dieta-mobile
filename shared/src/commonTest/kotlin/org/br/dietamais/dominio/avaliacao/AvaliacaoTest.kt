package org.br.dietamais.dominio.avaliacao

import org.br.dietamais.data.remoto.dto.*
import kotlin.test.*
import kotlin.time.Instant
import kotlin.uuid.Uuid

class AvaliacaoTest {
    private fun avaliacao(quando: String, peso: Double, cintura: Double? = null, gordura: Double? = null) = AvaliacaoResposta(
        Uuid.random(), Uuid.random(), Uuid.random(), Instant.parse(quando), peso, 1.75, imcDe(peso, 1.75), classificarImc(imcDe(peso, 1.75)),
        PerimetrosAvaliacao(cintura = cintura), composicao = ComposicaoAvaliacao(percentualGordura = gordura), versao = 1,
        criadoEm = Instant.parse(quando), atualizadoEm = Instant.parse(quando))

    @Test fun imcTemDuasCasasEClassificaPelaOms() {
        assertEquals(22.86, imcDe(70.0, 1.75))
        assertEquals(ClassificacaoImc.ABAIXO_DO_PESO, classificarImc(18.49))
        assertEquals(ClassificacaoImc.NORMAL, classificarImc(18.5))
        assertEquals(ClassificacaoImc.SOBREPESO, classificarImc(25.0))
        assertEquals(ClassificacaoImc.OBESIDADE_I, classificarImc(30.0))
        assertEquals(ClassificacaoImc.OBESIDADE_II, classificarImc(35.0))
        assertEquals(ClassificacaoImc.OBESIDADE_III, classificarImc(40.0))
    }

    @Test fun validaObrigatoriosIntervalosECasas() {
        val erros = validarMedidas(mapOf(MedidaAvaliacao.PESO to "19,9", MedidaAvaliacao.CINTURA to "9", MedidaAvaliacao.DOBRA_TRICEPS to "81",
            MedidaAvaliacao.QUADRIL to "abc", MedidaAvaliacao.COXA to "50,123"))
        assertEquals(ErroMedida.FORA_DO_INTERVALO, erros[MedidaAvaliacao.PESO])
        assertEquals(ErroMedida.OBRIGATORIA, erros[MedidaAvaliacao.ESTATURA])
        assertEquals(ErroMedida.FORA_DO_INTERVALO, erros[MedidaAvaliacao.CINTURA])
        assertEquals(ErroMedida.FORA_DO_INTERVALO, erros[MedidaAvaliacao.DOBRA_TRICEPS])
        assertEquals(ErroMedida.INVALIDA, erros[MedidaAvaliacao.QUADRIL])
        assertEquals(ErroMedida.CASAS_DECIMAIS, erros[MedidaAvaliacao.COXA])
        assertTrue(validarMedidas(mapOf(MedidaAvaliacao.PESO to "70,5", MedidaAvaliacao.ESTATURA to "1.75", MedidaAvaliacao.COXA to "50,10")).isEmpty())
    }

    @Test fun montaRequisicaoComVirgulaEOmiteVazios() {
        val r = montarRequisicao(mapOf(MedidaAvaliacao.PESO to "70,5", MedidaAvaliacao.ESTATURA to "1,75", MedidaAvaliacao.CINTURA to "80"),
            Instant.parse("2026-10-01T10:00:00Z"), "  ", true, " ")
        assertEquals(70.5, r.pesoKg); assertEquals(1.75, r.estaturaM); assertEquals(80.0, r.perimetros.cintura)
        assertNull(r.perimetros.quadril); assertNull(r.parecer); assertNull(r.justificativa); assertEquals(true, r.compartilhada)
    }

    @Test fun textoDecimalUsaVirgulaSemZerosSobrando() {
        assertEquals("70", textoDecimal(70.0)); assertEquals("70,5", textoDecimal(70.5)); assertEquals("22,86", textoDecimal(22.857)); assertEquals("1,05", textoDecimal(1.05))
    }

    @Test fun comparaDuasAvaliacoesDaMaisAntigaParaAMaisNova() {
        val nova = avaliacao("2026-10-20T10:00:00Z", 68.0, cintura = 78.0, gordura = 22.0)
        val antiga = avaliacao("2026-09-20T10:00:00Z", 72.0, cintura = 82.0)
        val v = compararAvaliacoes(nova, antiga)
        assertTrue(v.first().ehImc)
        val peso = v.first { it.medida == MedidaAvaliacao.PESO }
        assertEquals(72.0, peso.antes); assertEquals(68.0, peso.depois); assertEquals(-4.0, peso.delta)
        assertEquals(-4.0, v.first { it.medida == MedidaAvaliacao.CINTURA }.delta)
        assertNull(v.firstOrNull { it.medida == MedidaAvaliacao.PERCENTUAL_GORDURA }) // só existe em uma delas
    }
}
