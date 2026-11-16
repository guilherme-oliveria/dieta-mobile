package org.br.dietamais.dominio.pacientes

import kotlinx.datetime.TimeZone
import org.br.dietamais.data.remoto.dto.PacienteResposta
import org.br.dietamais.data.remoto.dto.StatusVinculo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlin.uuid.Uuid

class AtencaoTest {
    private val agora = Instant.parse("2026-11-05T15:00:00Z")
    private val utc = TimeZone.UTC
    private fun paciente(status: StatusVinculo = StatusVinculo.ATIVO) = PacienteResposta(Uuid.random(), Uuid.random(), "Fictício", status = status, versao = 1, criadoEm = agora, atualizadoEm = agora)

    @Test fun semResumoNaoMarcaNada() = assertEquals(emptySet(), atencaoDe(paciente(), null, agora, utc))

    @Test fun retornoHojeUsaODiaDoFuso() {
        val p = paciente()
        assertEquals(setOf(FiltroAtencao.RETORNO_HOJE), atencaoDe(p, ResumoPaciente(agora + 2.hours, agora - 10.days), agora, utc))
        assertEquals(emptySet(), atencaoDe(p, ResumoPaciente(agora + 1.days, agora - 10.days), agora, utc))
    }

    @Test fun avaliacaoVencidaDepoisDeNoventaDiasOuSemAvaliacao() {
        val p = paciente(); val retorno = agora + 3.days
        assertEquals(emptySet(), atencaoDe(p, ResumoPaciente(retorno, agora - 90.days), agora, utc))
        assertEquals(setOf(FiltroAtencao.AVALIACAO_VENCIDA), atencaoDe(p, ResumoPaciente(retorno, agora - 91.days), agora, utc))
        assertEquals(setOf(FiltroAtencao.AVALIACAO_VENCIDA), atencaoDe(p, ResumoPaciente(retorno, null), agora, utc))
    }

    @Test fun semRetornoSoParaAtivos() {
        val r = ResumoPaciente(null, agora - 5.days)
        assertEquals(setOf(FiltroAtencao.SEM_RETORNO), atencaoDe(paciente(), r, agora, utc))
        assertEquals(emptySet(), atencaoDe(paciente(StatusVinculo.ENCERRADO), r, agora, utc))
        assertEquals(emptySet(), atencaoDe(paciente(StatusVinculo.CONVIDADO), r, agora, utc))
    }

    @Test fun contagemEFiltroIgnoramRetornoHojeNaContagem() {
        val a = paciente(); val b = paciente(); val c = paciente()
        val resumos = mapOf(a.vinculoId to ResumoPaciente(agora + 1.hours, agora - 5.days), b.vinculoId to ResumoPaciente(null, agora - 5.days), c.vinculoId to ResumoPaciente(agora + 5.days, null))
        assertEquals(2, contarAtencao(listOf(a, b, c), resumos, agora, utc))
        assertEquals(listOf(a), filtrarPorAtencao(listOf(a, b, c), FiltroAtencao.RETORNO_HOJE, resumos, agora, utc))
        assertEquals(listOf(a, b, c), filtrarPorAtencao(listOf(a, b, c), null, resumos, agora, utc))
    }
}
