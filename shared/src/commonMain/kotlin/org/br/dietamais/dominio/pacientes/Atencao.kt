package org.br.dietamais.dominio.pacientes

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import org.br.dietamais.data.remoto.dto.PacienteResposta
import org.br.dietamais.data.remoto.dto.StatusVinculo
import kotlin.time.Instant
import kotlin.uuid.Uuid

/** Dias sem avaliação a partir dos quais ela conta como vencida (ART-P1-01). [SUPOSIÇÃO] valor inicial; ajustar com a nutri. */
const val DIAS_AVALIACAO_VENCIDA = 90

/** O que a lista sabe de cada paciente além da ficha (BE-66): próxima consulta e data da última avaliação. */
data class ResumoPaciente(val proximaConsulta: Instant? = null, val ultimaAvaliacaoEm: Instant? = null,
    /** Peso, IMC e classificação da última avaliação, para o cartão "Última avaliação" da ficha (MOB-64c). */
    val ultimaAvaliacao: org.br.dietamais.data.remoto.dto.UltimaAvaliacaoResumoDto? = null)

enum class FiltroAtencao { RETORNO_HOJE, AVALIACAO_VENCIDA, SEM_RETORNO }

/** Filtros de atenção em que o paciente se encaixa. Só pacientes ativos entram em "vencida" e "sem retorno". */
fun atencaoDe(paciente: PacienteResposta, resumo: ResumoPaciente?, agora: Instant, fuso: TimeZone): Set<FiltroAtencao> {
    if (resumo == null) return emptySet()
    val hoje = agora.toLocalDateTime(fuso).date
    val r = mutableSetOf<FiltroAtencao>()
    if (resumo.proximaConsulta != null && resumo.proximaConsulta.toLocalDateTime(fuso).date == hoje) r += FiltroAtencao.RETORNO_HOJE
    if (paciente.status == StatusVinculo.ATIVO) {
        val ultima = resumo.ultimaAvaliacaoEm
        if (ultima == null || ultima.toLocalDateTime(fuso).date.daysUntil(hoje) > DIAS_AVALIACAO_VENCIDA) r += FiltroAtencao.AVALIACAO_VENCIDA
        if (resumo.proximaConsulta == null) r += FiltroAtencao.SEM_RETORNO
    }
    return r
}

/** Pacientes que precisam de atenção: avaliação vencida ou sem retorno marcado. */
fun contarAtencao(pacientes: List<PacienteResposta>, resumos: Map<Uuid, ResumoPaciente>, agora: Instant, fuso: TimeZone): Int =
    pacientes.count { p -> atencaoDe(p, resumos[p.vinculoId], agora, fuso).any { it != FiltroAtencao.RETORNO_HOJE } }

fun filtrarPorAtencao(pacientes: List<PacienteResposta>, filtro: FiltroAtencao?, resumos: Map<Uuid, ResumoPaciente>, agora: Instant, fuso: TimeZone): List<PacienteResposta> =
    if (filtro == null) pacientes else pacientes.filter { filtro in atencaoDe(it, resumos[it.vinculoId], agora, fuso) }
