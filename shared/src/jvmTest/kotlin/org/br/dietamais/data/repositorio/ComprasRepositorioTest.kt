package org.br.dietamais.data.repositorio

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.br.dietamais.data.local.BancoDieta
import kotlin.test.*
import kotlin.uuid.Uuid

/** MOB-UX-07: itens marcados da lista de compras ficam no aparelho, por conta, e o "Limpar marcados" apaga o guardado. */
class ComprasRepositorioTest {
    private fun comBanco(bloco: suspend (BancoDieta) -> Unit) = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try { BancoDieta.Schema.create(driver); bloco(BancoDieta(driver)) } finally { driver.close() }
    }

    @Test fun marcadosVoltamAoReabrirOCasoDeUsoELimparApagaOGuardado() = comBanco { banco ->
        banco.sessaoQueries.salvar(Uuid.random().toString(), "PACIENTE")
        val arroz = Uuid.random(); val ovo = Uuid.random()
        ComprasRepositorio(banco).apply { marcar(arroz, true); marcar(ovo, true); marcar(ovo, false) }
        // outra instância sobre o mesmo banco = app reaberto
        val reaberto = ComprasRepositorio(banco)
        assertEquals(setOf(arroz), reaberto.observarMarcados().first())
        reaberto.limpar()
        assertTrue(ComprasRepositorio(banco).observarMarcados().first().isEmpty())
    }

    @Test fun marcasDeUmaContaNaoAparecemEmOutra() = comBanco { banco ->
        val item = Uuid.random()
        banco.sessaoQueries.salvar(Uuid.random().toString(), "PACIENTE")
        ComprasRepositorio(banco).marcar(item, true)
        banco.sessaoQueries.salvar(Uuid.random().toString(), "PACIENTE")
        assertTrue(ComprasRepositorio(banco).observarMarcados().first().isEmpty())
    }
}
