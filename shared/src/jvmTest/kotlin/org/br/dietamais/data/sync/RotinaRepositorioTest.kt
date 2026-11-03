package org.br.dietamais.data.sync

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.br.dietamais.data.local.BancoDieta
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.RotinaRepositorio
import kotlin.test.*
import kotlin.time.Instant
import kotlin.uuid.Uuid

class RotinaRepositorioTest {
    private val usuario = Uuid.parse("019a0f32-5000-7000-8000-000000000003")
    private val agora = Instant.parse("2026-10-28T09:00:00Z")

    private fun comBanco(bloco: suspend (BancoDieta) -> Unit) = runTest {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
            BancoDieta.Schema.create(driver)
            val banco = BancoDieta(driver); banco.sessaoQueries.salvar(usuario.toString(), "PACIENTE"); bloco(banco)
        }
    }

    @Test fun registroLocalEMutacaoNascemJuntosComOMesmoId() = comBanco { banco ->
        val repo = RotinaRepositorio(banco)
        val dieta = Uuid.random(); val refeicao = Uuid.random(); val item = Uuid.random(); val alimento = Uuid.random()
        repo.registrarCheckin(dieta, refeicao, LocalDate(2026, 10, 28), StatusCheckin.TROCADO, listOf(TrocaCheckin(item, alimento)), agora)
        repo.registrarAgua(250, agora); repo.registrarPeso(72.5, agora)
        val dados = repo.observar().first()
        assertEquals(StatusCheckin.TROCADO, dados.checkins.single().status); assertEquals(250, dados.agua.single().ml); assertEquals(72.5, dados.pesos.single().pesoKg)
        val fila = FilaMutacoes(banco).pendentes()
        assertEquals(listOf("checkins", "agua", "pesos"), fila.map { it.entidade })
        assertTrue(fila.all { it.operacao == OperacaoMutacao.CRIAR })
        assertEquals(dados.checkins.single().id, fila[0].id); assertEquals(fila[0].id.toString(), fila[0].dados["id"]?.toString()?.trim('"'))
    }

    @Test fun valoresInvalidosNaoGravamNada() = comBanco { banco ->
        val repo = RotinaRepositorio(banco)
        assertFailsWith<IllegalArgumentException> { repo.registrarAgua(0, agora) }
        assertFailsWith<IllegalArgumentException> { repo.registrarPeso(10.0, agora) }
        assertFailsWith<IllegalArgumentException> { repo.registrarCheckin(Uuid.random(), Uuid.random(), LocalDate(2026, 10, 28), StatusCheckin.FEITO, listOf(TrocaCheckin(Uuid.random(), Uuid.random())), agora) }
        assertTrue(FilaMutacoes(banco).pendentes().isEmpty())
    }
}
