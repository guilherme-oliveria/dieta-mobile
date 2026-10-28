package org.br.dietamais.data.remoto

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.br.dietamais.data.local.BancoDieta
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.sync.*
import kotlin.test.*
import kotlin.uuid.Uuid

class SyncLeituraSqlTest {
    private val usuario = Uuid.parse("019a0f32-5000-7000-8000-000000000001")
    private val contexto = ContextoLeituraSync(usuario, Perfil.NUTRICIONISTA, "epoca-ficticia-1")
    private fun pagina() = JsonContrato.decodeFromString<SyncResposta>(recursoContrato("puxarMudancas/resposta.json"))
    private fun vazia(cursor: String, mais: Boolean = false) = SyncResposta(cursor, mais, MudancasFase1Resposta(emptyList(), emptyList(), emptyList()))

    @Test fun aplicaTresColecoesECursorNaContaAtiva() = comBanco { banco ->
        val armazem = ArmazemLeituraSql(banco) { contexto }
        assertNull(armazem.cursor(contexto)); armazem.aplicar(contexto, pagina())
        assertEquals("3", armazem.cursor(contexto))
        assertEquals(pagina().mudancas.pacientes.single().dados, pacienteSalvo(banco))
        assertEquals(1, banco.registroSyncQueries.daSessao("vinculos").executeAsList().size)
        assertEquals(1, banco.registroSyncQueries.daSessao("nutris").executeAsList().size)
        banco.sessaoQueries.salvar(Uuid.random().toString(), "NUTRICIONISTA")
        assertTrue(banco.pacienteCacheQueries.daSessao().executeAsList().isEmpty())
        assertTrue(banco.registroSyncQueries.daSessao("vinculos").executeAsList().isEmpty())
    }

    @Test fun colecoesDaFase2e3SaoGuardadasInclusiveComIdDeEnvelopeDiferenteDoDto() = comBanco { banco ->
        val armazem = ArmazemLeituraSql(banco) { contexto }; armazem.cursor(contexto)
        val emUso = MudancaSyncResposta(Uuid.random(), 3, false, DietaEmUsoResposta(Uuid.random(), 2))
        val agua = AguaResposta(Uuid.random(), Uuid.random(), 250, kotlin.time.Instant.parse("2026-10-28T12:00:00Z"))
        val base = vazia("0").mudancas
        armazem.aplicar(contexto, vazia("9").copy(mudancas = base.copy(dietaEmUso = listOf(emUso), agua = listOf(MudancaSyncResposta(agua.id, 1, false, agua)))))
        assertEquals(1, banco.registroSyncQueries.daSessao("dietaEmUso").executeAsList().size)
        assertEquals(1, banco.registroSyncQueries.daSessao("agua").executeAsList().size)
        val errada = MudancaSyncResposta(Uuid.random(), 1, false, agua)
        assertFailsWith<RespostaInvalida> { armazem.aplicar(contexto, vazia("10").copy(mudancas = base.copy(agua = listOf(errada)))) }
    }

    @Test fun revisaoDaProjecaoNaoSubstituiVersaoClinicaNemImpedeNovaFichaComVersaoMenor() = comBanco { banco ->
        val armazem = ArmazemLeituraSql(banco) { contexto }; armazem.cursor(contexto)
        val original = pagina().mudancas.pacientes.single()
        val primeira = original.copy(versao = 100)
        armazem.aplicar(contexto, vazia("100").copy(mudancas = vazia("0").mudancas.copy(pacientes = listOf(primeira))))
        assertEquals(original.dados!!.versao, pacienteSalvo(banco).versao)
        val nova = original.copy(versao = 101, dados = original.dados.copy(vinculoId = Uuid.random(), versao = 0))
        armazem.aplicar(contexto, vazia("101").copy(mudancas = vazia("0").mudancas.copy(pacientes = listOf(nova))))
        assertEquals(nova.dados, pacienteSalvo(banco))
        armazem.aplicar(contexto, vazia("102").copy(mudancas = vazia("0").mudancas.copy(pacientes = listOf(primeira))))
        assertEquals(nova.dados, pacienteSalvo(banco))
    }

    @Test fun encerradoPermaneceETombstoneRemoveDadosSemPermitirRessurreicaoPorRevisaoAntiga() = comBanco { banco ->
        val armazem = ArmazemLeituraSql(banco) { contexto }; armazem.cursor(contexto)
        val original = pagina().mudancas.pacientes.single()
        val encerrado = original.copy(versao = 4, dados = original.dados!!.copy(status = StatusVinculo.ENCERRADO))
        armazem.aplicar(contexto, vazia("4").copy(mudancas = vazia("0").mudancas.copy(pacientes = listOf(encerrado))))
        assertEquals(StatusVinculo.ENCERRADO, pacienteSalvo(banco).status)
        val tombstone = original.copy(versao = 5, excluido = true, dados = null)
        armazem.aplicar(contexto, vazia("5").copy(mudancas = vazia("0").mudancas.copy(pacientes = listOf(tombstone))))
        assertTrue(banco.pacienteCacheQueries.daSessao().executeAsList().isEmpty())
        armazem.aplicar(contexto, vazia("6").copy(mudancas = vazia("0").mudancas.copy(pacientes = listOf(encerrado))))
        assertTrue(banco.pacienteCacheQueries.daSessao().executeAsList().isEmpty())
        val restaurado = encerrado.copy(versao = 7)
        armazem.aplicar(contexto, vazia("7").copy(mudancas = vazia("0").mudancas.copy(pacientes = listOf(restaurado))))
        assertEquals(StatusVinculo.ENCERRADO, pacienteSalvo(banco).status)
    }

    @Test fun itemInvalidoDesfazTodaPaginaECursor() = comBanco { banco ->
        val armazem = ArmazemLeituraSql(banco) { contexto }; armazem.cursor(contexto); armazem.aplicar(contexto, pagina())
        val antes = pacienteSalvo(banco)
        val alterada = pagina().mudancas.pacientes.single().let { it.copy(versao = 10, dados = it.dados!!.copy(nome = "Mudança fictícia")) }
        val invalido = pagina().mudancas.vinculos.single().copy(id = Uuid.random(), versao = 11)
        assertFailsWith<RespostaInvalida> { armazem.aplicar(contexto, SyncResposta("11", false,
            pagina().mudancas.copy(pacientes = listOf(alterada), vinculos = listOf(invalido)))) }
        assertEquals(antes, pacienteSalvo(banco)); assertEquals("3", armazem.cursor(contexto))
    }

    @Test fun outraEpocaDaMesmaContaNaoPodeAplicarRespostaAnterior() = comBanco { banco ->
        var atual = contexto
        val armazem = ArmazemLeituraSql(banco) { atual }; armazem.cursor(contexto)
        atual = contexto.copy(operacao = "epoca-ficticia-2")
        assertFailsWith<SessaoAlterada> { armazem.aplicar(contexto, pagina()) }
        assertTrue(banco.pacienteCacheQueries.daSessao().executeAsList().isEmpty())
        assertNull(banco.cursorSyncQueries.buscar(usuario.toString(), "NUTRICIONISTA").executeAsOneOrNull())
    }

    @Test fun bootstrapParcialPreservaLegadoEReconciliaNoFimSemApagarCriacaoOnlineConcorrente() = comBanco { banco ->
        val base = pagina().mudancas.pacientes.single().dados!!
        val antigo = base.copy(id = Uuid.random(), vinculoId = Uuid.random(), nome = "Legado fictício")
        salvar(banco, base); salvar(banco, antigo)
        var armazem = ArmazemLeituraSql(banco) { contexto }; armazem.cursor(contexto)
        armazem.aplicar(contexto, pagina().copy(temMais = true))
        assertEquals(2, banco.pacienteCacheQueries.daSessao().executeAsList().size)
        val criadoDuranteCarga = base.copy(id = Uuid.random(), vinculoId = Uuid.random(), nome = "Criação online fictícia")
        salvar(banco, criadoDuranteCarga)
        armazem = ArmazemLeituraSql(banco) { contexto }
        assertEquals("3", armazem.cursor(contexto))
        armazem.aplicar(contexto, vazia("4"))
        val finais = banco.pacienteCacheQueries.daSessao().executeAsList().map { JsonContrato.decodeFromString<PacienteResposta>(it).id }.toSet()
        assertEquals(setOf(base.id, criadoDuranteCarga.id), finais)
    }

    @Test fun pacienteRecebeVinculosENutrisMasNaoFichaPrivada() = comBanco(Perfil.PACIENTE) { banco ->
        val paciente = contexto.copy(perfil = Perfil.PACIENTE)
        val armazem = ArmazemLeituraSql(banco) { paciente }; armazem.cursor(paciente)
        assertFailsWith<RespostaInvalida> { armazem.aplicar(paciente, pagina()) }
        armazem.aplicar(paciente, pagina().copy(mudancas = pagina().mudancas.copy(pacientes = emptyList())))
        assertEquals(1, banco.registroSyncQueries.daSessao("vinculos").executeAsList().size)
        assertTrue(banco.pacienteCacheQueries.daSessao().executeAsList().isEmpty())
    }

    private fun pacienteSalvo(banco: BancoDieta) = JsonContrato.decodeFromString<PacienteResposta>(banco.pacienteCacheQueries.daSessao().executeAsOne())
    private fun salvar(banco: BancoDieta, paciente: PacienteResposta) = banco.pacienteCacheQueries.salvar(usuario.toString(), "NUTRICIONISTA",
        paciente.vinculoId.toString(), paciente.id.toString(), JsonContrato.encodeToString(paciente))
    private fun comBanco(perfil: Perfil = Perfil.NUTRICIONISTA, bloco: (BancoDieta) -> Unit) {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
            BancoDieta.Schema.create(driver)
            val banco = BancoDieta(driver); banco.sessaoQueries.salvar(usuario.toString(), perfil.name)
            bloco(banco)
        }
    }
}
