package org.br.dietamais.dominio.painel

import kotlinx.datetime.TimeZone
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.ui.notificacoes.avisoFalso
import org.br.dietamais.ui.nutri.agenda.consultaFalsa
import org.br.dietamais.ui.nutri.agenda.pacienteFalso
import kotlin.test.*
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlin.uuid.Uuid

class PainelTest {
    private val fuso = TimeZone.of("America/Sao_Paulo")
    private val agora = Instant.parse("2026-10-28T15:00:00Z") // 12:00 locais
    private val ana = pacienteFalso("Ana Fictícia")
    private val bia = pacienteFalso("Bia Fictícia")

    @Test fun consultasDeHojeIgnoramCanceladasEOutrosDias() {
        val hoje = consultaFalsa(ana.id, agora + 2.hours)
        val cancelada = consultaFalsa(bia.id, agora + 3.hours).copy(status = StatusConsulta.CANCELADA)
        val amanha = consultaFalsa(ana.id, agora + 1.days)
        val r = montarPainel(listOf(amanha, cancelada, hoje), listOf(ana, bia), emptyList(), emptyList(), null, agora, fuso)
        assertEquals(listOf(hoje), r.consultasHoje)
    }

    @Test fun semCheckinVemDosAvisosNaoLidosSemRepetirPaciente() {
        val rota = "/pacientes/${ana.id}"
        val a1 = avisoFalso(rota, tipo = TipoAviso.PACIENTE_SEM_CHECKIN); val a2 = avisoFalso(rota, tipo = TipoAviso.PACIENTE_SEM_CHECKIN)
        val lido = avisoFalso("/pacientes/${bia.id}", lido = true, tipo = TipoAviso.PACIENTE_SEM_CHECKIN)
        val outro = avisoFalso(rota, tipo = TipoAviso.NOVA_DIETA); val semRota = avisoFalso(null, tipo = TipoAviso.PACIENTE_SEM_CHECKIN)
        val r = montarPainel(emptyList(), listOf(ana, bia), listOf(a1, a2, lido, outro, semRota), emptyList(), null, agora, fuso)
        assertEquals(listOf(ana.id), r.semCheckin.map { it.fichaId }); assertEquals("Ana Fictícia", r.semCheckin.single().nome)
    }

    @Test fun respondidosSoDosUltimosSeteDiasDoMaisNovoParaOMaisAntigo() {
        fun q(paciente: Uuid, respondido: Instant?, status: StatusQuestionario = StatusQuestionario.RESPONDIDO) = QuestionarioResposta(Uuid.random(), paciente, Uuid.random(), null, "Q",
            status = status, enviadoEm = agora - 20.days, respondidoEm = respondido, versao = 1, criadoEm = agora - 20.days, atualizadoEm = agora)
        val r = montarPainel(emptyList(), listOf(ana, bia), emptyList(), listOf(q(ana.id, agora - 2.days), q(bia.id, agora - 1.days), q(ana.id, agora - 9.days),
            q(bia.id, null, StatusQuestionario.PENDENTE)), null, agora, fuso)
        assertEquals(listOf(bia.id, ana.id), r.respondidos.map { it.fichaId })
    }

    @Test fun rotaDaFicha() {
        val id = Uuid.random()
        assertEquals(id, fichaDaRota("/pacientes/$id")); assertNull(fichaDaRota("/pacientes")); assertNull(fichaDaRota("/agenda")); assertNull(fichaDaRota(null))
        assertNull(fichaDaRota("/pacientes/x"))
    }
}
