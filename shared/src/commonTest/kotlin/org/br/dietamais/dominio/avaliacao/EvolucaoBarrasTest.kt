package org.br.dietamais.dominio.avaliacao

import kotlinx.datetime.TimeZone
import kotlin.test.*
import kotlin.time.Instant

class EvolucaoBarrasTest {
    private val fuso = TimeZone.of("America/Sao_Paulo")
    private fun ponto(data: String, v: Double) = PontoSerie(Instant.parse(data), v)

    @Test fun pegaAsUltimasSeisEmOrdemCronologicaComRotuloMesAno() {
        val pontos = (1..8).map { ponto("2026-0${it}-15T12:00:00Z", 100.0 - it) }.reversed()
        val barras = barrasDaSerie(pontos, fuso)
        assertEquals(listOf("03/26", "04/26", "05/26", "06/26", "07/26", "08/26"), barras.map { it.rotulo })
        assertEquals(listOf(97.0, 96.0, 95.0, 94.0, 93.0, 92.0), barras.map { it.valor })
    }

    @Test fun usaOFusoLocalNaViradaDeMesESerieVaziaNaoQuebra() {
        assertEquals("09/26", barrasDaSerie(listOf(ponto("2026-10-01T01:00:00Z", 80.0)), fuso).single().rotulo) // 30/09 22:00 em São Paulo
        assertTrue(barrasDaSerie(emptyList(), fuso).isEmpty())
        assertEquals(2, barrasDaSerie(listOf(ponto("2026-01-10T12:00:00Z", 1.0), ponto("2026-02-10T12:00:00Z", 2.0), ponto("2026-03-10T12:00:00Z", 3.0)), fuso, maximo = 2).size)
    }
}
