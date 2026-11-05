package org.br.dietamais.dominio.pacientes

import org.br.dietamais.data.remoto.dto.*
import kotlin.test.*
import kotlin.time.Instant
import kotlin.uuid.Uuid

class ListaPacientesTest {
    private val t = Instant.parse("2026-11-05T00:00:00Z")
    private fun p(nome: String, tel: String? = null, status: StatusVinculo = StatusVinculo.ATIVO) =
        PacienteResposta(Uuid.random(), Uuid.random(), nome, telefone = tel, status = status, versao = 1, criadoEm = t, atualizadoEm = t)

    private val todos = listOf(p("Ângela Souza", "(11) 98888-1234"), p("Bruno Lima", "21 97777-0000"), p("Carla Mendes", status = StatusVinculo.CONVIDADO), p("Édson Rocha"), p("1Teste"))

    @Test fun buscaPorNomeIgnoraAcentoECaixa() {
        assertEquals(listOf("Ângela Souza"), filtrarPacientes(todos, "angela", null).map { it.nome })
        assertEquals(listOf("Édson Rocha"), filtrarPacientes(todos, "EDSON", null).map { it.nome })
    }
    @Test fun buscaPorTelefoneUsaSoOsDigitos() {
        assertEquals(listOf("Bruno Lima"), filtrarPacientes(todos, "97777", null).map { it.nome })
        assertEquals(listOf("Ângela Souza"), filtrarPacientes(todos, "(11) 98888", null).map { it.nome })
    }
    @Test fun poucosDigitosNaoCasamComTelefone() {
        assertTrue(filtrarPacientes(todos, "21", null).isEmpty())
    }
    @Test fun filtroDeStatusSeCombinaComABusca() {
        assertEquals(listOf("Carla Mendes"), filtrarPacientes(todos, "", StatusVinculo.CONVIDADO).map { it.nome })
        assertTrue(filtrarPacientes(todos, "bruno", StatusVinculo.CONVIDADO).isEmpty())
    }
    @Test fun agrupaPorInicialSemAcentoComOutrosPorUltimo() {
        val g = agruparPorInicial(todos)
        assertEquals(listOf('A', 'B', 'C', 'E', INICIAL_OUTROS), g.map { it.inicial })
        assertEquals(listOf("Ângela Souza"), g[0].pacientes.map { it.nome })
    }
    @Test fun ordenaDentroDoGrupo() {
        val g = agruparPorInicial(listOf(p("Ana Paula"), p("Álvaro"), p("Adriana")))
        assertEquals(listOf("Adriana", "Álvaro", "Ana Paula"), g.single().pacientes.map { it.nome })
    }
}
