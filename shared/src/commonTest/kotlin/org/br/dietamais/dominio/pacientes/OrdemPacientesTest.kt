package org.br.dietamais.dominio.pacientes

import kotlin.test.*
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlin.uuid.Uuid

class OrdemPacientesTest {
    private val agora = Instant.parse("2026-11-12T12:00:00Z")
    private class P(val id: Uuid, val nome: String)
    private fun p(nome: String) = P(Uuid.random(), nome)

    @Test fun retornoMaisProximoPrimeiroESemConsultaNoFimPorNome() {
        val ana = p("Ana"); val bia = p("Bia"); val caio = p("Caio"); val duda = p("duda"); val eva = p("Eva")
        val resumos = mapOf(
            bia.id to ResumoPaciente(proximaConsulta = agora + 1.days), caio.id to ResumoPaciente(proximaConsulta = agora + 5.days),
            eva.id to ResumoPaciente(), // sem consulta marcada
        )
        val ordem = ordenarPorProximoRetorno(listOf(ana, bia, caio, duda, eva), resumos, { it.id }, { it.nome })
        assertEquals(listOf("Bia", "Caio", "Ana", "duda", "Eva"), ordem.map { it.nome })
    }

    @Test fun empateDeDataDesempataPorNome() {
        val z = p("Zélia"); val a = p("Ana"); val dia = agora + 2.days
        val ordem = ordenarPorProximoRetorno(listOf(z, a), mapOf(z.id to ResumoPaciente(dia), a.id to ResumoPaciente(dia)), { it.id }, { it.nome })
        assertEquals(listOf("Ana", "Zélia"), ordem.map { it.nome })
    }

    @Test fun semResumosMantemTodosPorNome() {
        val b = p("Bia"); val a = p("Ana")
        assertEquals(listOf("Ana", "Bia"), ordenarPorProximoRetorno(listOf(b, a), emptyMap(), { it.id }, { it.nome }).map { it.nome })
    }
}
