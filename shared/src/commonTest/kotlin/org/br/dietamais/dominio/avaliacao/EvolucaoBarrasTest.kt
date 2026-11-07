package org.br.dietamais.dominio.avaliacao

import kotlinx.datetime.TimeZone
import kotlin.test.*
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class EvolucaoBarrasTest {
    private val fuso = TimeZone.of("America/Sao_Paulo")
    private fun ponto(data: String, v: Double) = PontoSerie(Instant.parse(data), v)

    @Test fun mostraAsMedidasEmOrdemCronologicaComRotuloDiaMes() {
        val pontos = listOf(ponto("2026-09-20T12:00:00Z", 3.0), ponto("2026-07-05T12:00:00Z", 1.0), ponto("2026-08-10T12:00:00Z", 2.0))
        val barras = barrasDaSerie(pontos, fuso)
        assertEquals(listOf("05/07", "10/08", "20/09"), barras.map { it.rotulo })
        assertEquals(listOf(1.0, 2.0, 3.0), barras.map { it.valor })
    }

    @Test fun comMuitosPontosEspacaPorIgualMantendoPrimeiroEUltimo() {
        val pontos = (0 until 26).map { PontoSerie(Instant.parse("2026-05-01T12:00:00Z") + (7 * it).days, it.toDouble()) }
        val barras = barrasDaSerie(pontos, fuso)
        assertEquals(12, barras.size)
        assertEquals(0.0, barras.first().valor)
        assertEquals(25.0, barras.last().valor)
        assertEquals("01/05", barras.first().rotulo)
        assertEquals(barras.map { it.valor }.sorted(), barras.map { it.valor })
        assertEquals(12, barras.map { it.rotulo }.toSet().size)
    }

    @Test fun usaOFusoLocalNaViradaDeMesESerieVaziaNaoQuebra() {
        assertEquals("30/09", barrasDaSerie(listOf(ponto("2026-10-01T01:00:00Z", 80.0)), fuso).single().rotulo) // 30/09 22:00 em São Paulo
        assertTrue(barrasDaSerie(emptyList(), fuso).isEmpty())
        assertEquals(2, barrasDaSerie(listOf(ponto("2026-01-10T12:00:00Z", 1.0), ponto("2026-02-10T12:00:00Z", 2.0), ponto("2026-03-10T12:00:00Z", 3.0)), fuso, maximo = 2).size)
    }
}
