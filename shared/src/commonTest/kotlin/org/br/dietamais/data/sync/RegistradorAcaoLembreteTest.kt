package org.br.dietamais.data.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.br.dietamais.data.remoto.dto.CheckinResposta
import org.br.dietamais.data.remoto.dto.StatusCheckin
import org.br.dietamais.data.remoto.dto.TrocaCheckin
import org.br.dietamais.data.repositorio.DadosRotina
import org.br.dietamais.data.repositorio.PortaRotina
import org.br.dietamais.dominio.rotina.AcaoLembrete
import kotlin.test.*
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

/** MOB-18b: os botões Comi e Pulei da notificação gravam o check-in pela mesma porta da tela Hoje; Troquei só abre o app. */
class RegistradorAcaoLembreteTest {
    private class RotinaFalsa : PortaRotina {
        val dados = MutableStateFlow(DadosRotina())
        val gravados = mutableListOf<Triple<StatusCheckin, List<TrocaCheckin>, Instant>>()
        override fun observar(): Flow<DadosRotina> = dados
        override fun registrarCheckin(dietaId: Uuid, refeicaoId: Uuid, data: LocalDate, status: StatusCheckin, trocas: List<TrocaCheckin>, agora: Instant) {
            gravados += Triple(status, trocas, agora)
            dados.value = dados.value.copy(checkins = dados.value.checkins + CheckinResposta(Uuid.random(), Uuid.random(), dietaId, refeicaoId, data, status, trocas, agora))
        }
        override fun registrarAgua(ml: Int, agora: Instant) = error("não usado")
        override fun registrarPeso(pesoKg: Double, agora: Instant) = error("não usado")
    }

    private val agora = Instant.parse("2026-10-28T15:30:00Z")
    private val relogio = object : Clock { override fun now() = agora }
    private val dieta = Uuid.random(); private val refeicao = Uuid.random(); private val dia = LocalDate(2026, 10, 28)

    @Test fun comiGravaCheckinFeitoSemTrocas() = runTest {
        val rotina = RotinaFalsa()
        assertEquals(ResultadoAcaoLembrete.GRAVADO, RegistradorAcaoLembrete(rotina, relogio).registrar(dieta, refeicao, dia, AcaoLembrete.COMI))
        assertEquals(Triple(StatusCheckin.FEITO, emptyList<TrocaCheckin>(), agora), rotina.gravados.single())
    }

    @Test fun puleiGravaCheckinPulado() = runTest {
        val rotina = RotinaFalsa()
        assertEquals(ResultadoAcaoLembrete.GRAVADO, RegistradorAcaoLembrete(rotina, relogio).registrar(dieta, refeicao, dia, AcaoLembrete.PULEI))
        assertEquals(StatusCheckin.PULADO, rotina.gravados.single().first)
    }

    @Test fun troqueiNaoGravaNada() = runTest {
        val rotina = RotinaFalsa()
        assertEquals(ResultadoAcaoLembrete.SO_ABRE_O_APP, RegistradorAcaoLembrete(rotina, relogio).registrar(dieta, refeicao, dia, AcaoLembrete.TROQUEI))
        assertTrue(rotina.gravados.isEmpty())
    }

    @Test fun naoDuplicaOCheckinDaMesmaRefeicaoNoMesmoDia() = runTest {
        val rotina = RotinaFalsa()
        val registrador = RegistradorAcaoLembrete(rotina, relogio)
        registrador.registrar(dieta, refeicao, dia, AcaoLembrete.COMI)
        assertEquals(ResultadoAcaoLembrete.JA_REGISTRADO, registrador.registrar(dieta, refeicao, dia, AcaoLembrete.PULEI))
        assertEquals(1, rotina.gravados.size)
    }

    @Test fun outraRefeicaoOuOutroDiaGravaDeNovo() = runTest {
        val rotina = RotinaFalsa()
        val registrador = RegistradorAcaoLembrete(rotina, relogio)
        registrador.registrar(dieta, refeicao, dia, AcaoLembrete.COMI)
        assertEquals(ResultadoAcaoLembrete.GRAVADO, registrador.registrar(dieta, Uuid.random(), dia, AcaoLembrete.COMI))
        assertEquals(ResultadoAcaoLembrete.GRAVADO, registrador.registrar(dieta, refeicao, LocalDate(2026, 10, 29), AcaoLembrete.COMI))
        assertEquals(3, rotina.gravados.size)
    }
}
