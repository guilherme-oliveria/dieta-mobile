package org.br.dietamais.dominio.avaliacao

import kotlinx.datetime.LocalDate
import org.br.dietamais.data.remoto.dto.Sexo
import org.br.dietamais.dominio.calculos.ProtocoloDobras
import kotlin.test.*

/** MOB-50: a prévia do formulário usa o mesmo motor dos vetores (caso V01 do PDF do Guilherme) e explica o que falta. */
class PreviaAvaliacaoTest {
    private val v01 = mapOf(
        MedidaAvaliacao.PESO to "102,5", MedidaAvaliacao.ESTATURA to "1,93", MedidaAvaliacao.CINTURA to "100",
        MedidaAvaliacao.DOBRA_TRICEPS to "20", MedidaAvaliacao.DOBRA_PEITORAL to "25", MedidaAvaliacao.DOBRA_AXILAR_MEDIA to "28",
        MedidaAvaliacao.DOBRA_SUBESCAPULAR to "30", MedidaAvaliacao.DOBRA_ABDOMINAL to "40", MedidaAvaliacao.DOBRA_SUPRAILIACA to "32", MedidaAvaliacao.DOBRA_COXA to "24",
        MedidaAvaliacao.PERCENTUAL_DESEJADO to "18",
    )
    private val dia = LocalDate(2026, 10, 28)
    private val nascimento = LocalDate(1997, 4, 28) // 29 anos e 6 meses

    @Test fun v01BateComOPdfDoGuilherme() {
        val p = previaAvaliacao(v01, Sexo.MASCULINO, nascimento, dia)
        val r = assertNotNull(p.resultado)
        assertEquals(29, p.idadeAnos); assertEquals(27.52, r.imc); assertEquals(1.0389, r.densidadeCorporal); assertEquals(26.48, r.percentualGordura)
        assertEquals(91.9, r.pesoParaPercentualDesejadoKg)
        assertEquals(listOf(FaltaDado.PUNHO, FaltaDado.DIAMETRO_PUNHO), p.faltas) // peso ósseo exige os diâmetros
    }

    @Test fun semDobrasAvisaEmVezDeZerar() {
        val p = previaAvaliacao(v01.filterKeys { it.secao != SecaoAvaliacao.DOBRAS }, Sexo.MASCULINO, nascimento, dia)
        assertNull(p.resultado?.percentualGordura); assertTrue(FaltaDado.DOBRAS in p.faltas)
    }

    @Test fun tresDobrasUsaAsDoSexo() {
        val tres = previaAvaliacao(v01, Sexo.MASCULINO, nascimento, dia, ProtocoloDobras.TRES)
        assertNotEquals(26.48, tres.resultado?.percentualGordura)
        assertEquals(setOf(MedidaAvaliacao.DOBRA_PEITORAL, MedidaAvaliacao.DOBRA_ABDOMINAL, MedidaAvaliacao.DOBRA_COXA), dobrasDoProtocolo(ProtocoloDobras.TRES, Sexo.MASCULINO))
        assertEquals(setOf(MedidaAvaliacao.DOBRA_TRICEPS, MedidaAvaliacao.DOBRA_SUPRAILIACA, MedidaAvaliacao.DOBRA_COXA), dobrasDoProtocolo(ProtocoloDobras.TRES, Sexo.FEMININO))
        assertEquals(7, dobrasDoProtocolo(ProtocoloDobras.SETE, null).size); assertEquals(7, dobrasDoProtocolo(ProtocoloDobras.TRES, null).size)
    }

    @Test fun faltaSexoNascimentoOuPesoNaoCalcula() {
        assertEquals(listOf(FaltaDado.PESO_ESTATURA), previaAvaliacao(emptyMap(), Sexo.MASCULINO, nascimento, dia).faltas)
        assertEquals(listOf(FaltaDado.SEXO), previaAvaliacao(v01, null, nascimento, dia).faltas)
        assertEquals(listOf(FaltaDado.SEXO), previaAvaliacao(v01, Sexo.NAO_INFORMADO, nascimento, dia).faltas)
        assertEquals(listOf(FaltaDado.NASCIMENTO), previaAvaliacao(v01, Sexo.MASCULINO, null, dia).faltas)
        assertNull(previaAvaliacao(v01, Sexo.MASCULINO, null, dia).resultado)
    }

    @Test fun menorDe18NaoCalculaPorEnquanto() {
        val p = previaAvaliacao(v01, Sexo.MASCULINO, LocalDate(2012, 1, 1), dia)
        assertNull(p.resultado); assertEquals(listOf(FaltaDado.FAIXA_ETARIA), p.faltas); assertEquals(14, p.idadeAnos)
    }

    @Test fun valorForaDoLimiteNaoEntraNaConta() {
        assertEquals(listOf(FaltaDado.PESO_ESTATURA), previaAvaliacao(v01 + (MedidaAvaliacao.PESO to "5"), Sexo.MASCULINO, nascimento, dia).faltas)
    }

    @Test fun montarRequisicaoLevaComplementaresEProtocolo() {
        val r = montarRequisicao(v01 + (MedidaAvaliacao.PUNHO to "18"), kotlin.time.Instant.parse("2026-10-28T12:00:00Z"), "", true,
            protocolo = org.br.dietamais.data.remoto.dto.ProtocoloAvaliacaoDto("TRES", "MUSCULAR"))
        assertEquals(18.0, r.complementares?.punhoCm); assertEquals(18.0, r.complementares?.percentualGorduraDesejado)
        assertEquals("TRES", r.protocolo?.dobras)
        assertNull(montarRequisicao(mapOf(MedidaAvaliacao.PESO to "70", MedidaAvaliacao.ESTATURA to "1,75"), kotlin.time.Instant.parse("2026-10-28T12:00:00Z"), "", true).complementares)
    }
}
