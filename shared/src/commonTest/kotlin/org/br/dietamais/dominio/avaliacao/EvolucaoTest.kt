package org.br.dietamais.dominio.avaliacao

import org.br.dietamais.data.remoto.dto.*
import kotlin.test.*
import kotlin.time.Instant
import kotlin.uuid.Uuid

class EvolucaoTest {
    private fun av(data: String, peso: Double, cintura: Double? = null) = AvaliacaoResposta(Uuid.random(), Uuid.random(), Uuid.random(), Instant.parse(data), peso, 1.7,
        imcDe(peso, 1.7), classificarImc(imcDe(peso, 1.7)), PerimetrosAvaliacao(cintura = cintura), versao = 1, criadoEm = Instant.parse(data), atualizadoEm = Instant.parse(data))
    private fun peso(data: String, kg: Double) = PesoResposta(Uuid.random(), Uuid.random(), kg, Instant.parse(data))

    @Test fun seriesSaemEmOrdemCronologicaEOmitemAsVazias() {
        val series = construirSeries(listOf(av("2026-10-01T00:00:00Z", 70.0, 80.0), av("2026-08-01T00:00:00Z", 74.0), av("2026-09-01T00:00:00Z", 72.0, 82.0)),
            listOf(peso("2026-09-20T00:00:00Z", 71.0), peso("2026-09-10T00:00:00Z", 71.5)))
        assertEquals(listOf(74.0, 72.0, 70.0), series.getValue(SerieEvolucao.PESO).map { it.valor })
        assertEquals(listOf(82.0, 80.0), series.getValue(SerieEvolucao.CINTURA).map { it.valor })
        assertEquals(listOf(71.5, 71.0), series.getValue(SerieEvolucao.PESO_EM_CASA).map { it.valor })
        assertFalse(SerieEvolucao.PERCENTUAL_GORDURA in series); assertTrue(SerieEvolucao.IMC in series)
    }

    @Test fun recortaPeloIntervaloContadoDeTras() {
        val pontos = listOf("2025-01-01T00:00:00Z", "2026-06-01T00:00:00Z", "2026-09-30T00:00:00Z").map { PontoSerie(Instant.parse(it), 1.0) }
        val agora = Instant.parse("2026-10-28T00:00:00Z")
        assertEquals(1, recortar(pontos, IntervaloEvolucao.TRES_MESES, agora).size)
        assertEquals(2, recortar(pontos, IntervaloEvolucao.UM_ANO, agora).size)
        assertEquals(3, recortar(pontos, IntervaloEvolucao.TUDO, agora).size)
    }

    @Test fun variacaoPrecisaDeDoisPontos() {
        assertNull(variacaoTotal(listOf(PontoSerie(Instant.parse("2026-01-01T00:00:00Z"), 70.0))))
        assertEquals(-2.5, variacaoTotal(listOf(PontoSerie(Instant.parse("2026-01-01T00:00:00Z"), 72.5), PontoSerie(Instant.parse("2026-02-01T00:00:00Z"), 70.0))))
    }
}
