package org.br.dietamais.dominio.agenda

import kotlinx.datetime.*
import org.br.dietamais.data.remoto.dto.ConsultaResposta
import org.br.dietamais.data.remoto.dto.StatusConsulta
import org.br.dietamais.data.remoto.dto.TipoConsulta
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlin.uuid.Uuid

/** Mesma tolerância do backend (RN-AGE-01): relógio do aparelho pode estar atrasado. */
private val TOLERANCIA = 5.minutes

fun diaLocal(instante: Instant, fuso: TimeZone): LocalDate = instante.toLocalDateTime(fuso).date

fun consultasDoDia(consultas: List<ConsultaResposta>, dia: LocalDate, fuso: TimeZone): List<ConsultaResposta> =
    consultas.filter { diaLocal(it.inicio, fuso) == dia }.sortedBy { it.inicio }

/** Segunda-feira da semana de `dia`. */
fun inicioDaSemana(dia: LocalDate): LocalDate = dia.minus(dia.dayOfWeek.ordinal, DateTimeUnit.DAY)

fun diasDaSemana(dia: LocalDate): List<LocalDate> = inicioDaSemana(dia).let { segunda -> (0..6).map { segunda.plus(it, DateTimeUnit.DAY) } }

/** Dias da semana com a lista (possivelmente vazia) de consultas de cada um. */
fun consultasDaSemana(consultas: List<ConsultaResposta>, dia: LocalDate, fuso: TimeZone): List<Pair<LocalDate, List<ConsultaResposta>>> =
    diasDaSemana(dia).map { it to consultasDoDia(consultas, it, fuso) }

/** Próxima consulta ainda por vir (ou em andamento) que não foi cancelada. */
fun proximaConsulta(consultas: List<ConsultaResposta>, agora: Instant): ConsultaResposta? =
    consultas.filter { it.status == StatusConsulta.AGENDADA && it.fim > agora }.minByOrNull { it.inicio }

/** Consultas não canceladas que se sobrepõem ao intervalo [inicio, inicio+duração); `ignorar` exclui a que está sendo remarcada. */
fun conflitos(consultas: List<ConsultaResposta>, inicio: Instant, duracaoMin: Int, ignorar: Uuid? = null): List<ConsultaResposta> {
    val fim = inicio + duracaoMin.minutes
    return consultas.filter { it.status == StatusConsulta.AGENDADA && it.id != ignorar && it.inicio < fim && inicio < it.fim }
}

fun formatarData(data: LocalDate): String = "${data.day.toString().padStart(2, '0')}/${data.month.number.toString().padStart(2, '0')}/${data.year}"
fun formatarHora(instante: Instant, fuso: TimeZone): String = instante.toLocalDateTime(fuso).let { "${it.hour.toString().padStart(2, '0')}:${it.minute.toString().padStart(2, '0')}" }

/** Aceita dd/mm/aaaa (e com um dígito em dia/mês). */
fun lerData(texto: String): LocalDate? {
    val partes = texto.trim().split('/')
    if (partes.size != 3) return null
    val (d, m, a) = partes.map { it.trim().toIntOrNull() ?: return null }
    if (a !in 2000..2200) return null
    return try { LocalDate(a, m, d) } catch (_: IllegalArgumentException) { null }
}

/** Aceita HH:MM e H:MM. */
fun lerHora(texto: String): Pair<Int, Int>? {
    val partes = texto.trim().split(':')
    if (partes.size != 2) return null
    val h = partes[0].trim().toIntOrNull() ?: return null
    val m = partes[1].trim().toIntOrNull() ?: return null
    return if (h in 0..23 && m in 0..59 && partes[1].trim().length == 2) h to m else null
}

enum class ErroConsulta { PACIENTE, DATA, HORA, DURACAO, PASSADO }

/** `id`/`versao` nulos = consulta nova. Data e hora no fuso do aparelho. */
data class FormularioConsulta(val id: Uuid? = null, val versao: Long? = null, val pacienteId: Uuid? = null, val data: String = "", val hora: String = "",
    val duracao: String = "50", val tipo: TipoConsulta = TipoConsulta.RETORNO, val lugar: String = "", val chave: Uuid? = null)

data class ResultadoFormulario(val inicio: Instant?, val duracaoMin: Int?, val erros: Set<ErroConsulta>)

fun validarFormulario(f: FormularioConsulta, fuso: TimeZone, agora: Instant, exigePaciente: Boolean = true): ResultadoFormulario {
    val erros = mutableSetOf<ErroConsulta>()
    if (exigePaciente && f.pacienteId == null) erros += ErroConsulta.PACIENTE
    val data = lerData(f.data)
    val hora = lerHora(f.hora)
    val duracao = f.duracao.trim().toIntOrNull()?.takeIf { it in 5..480 }
    if (data == null) erros += ErroConsulta.DATA
    if (hora == null) erros += ErroConsulta.HORA
    if (duracao == null) erros += ErroConsulta.DURACAO
    val inicio = if (data != null && hora != null) data.atTime(hora.first, hora.second).toInstant(fuso) else null
    if (inicio != null && inicio < agora - TOLERANCIA) erros += ErroConsulta.PASSADO
    return ResultadoFormulario(inicio, duracao, erros)
}

fun formularioDe(consulta: ConsultaResposta, fuso: TimeZone) = FormularioConsulta(consulta.id, consulta.versao, consulta.pacienteId,
    formatarData(diaLocal(consulta.inicio, fuso)), formatarHora(consulta.inicio, fuso), consulta.duracaoMin.toString(), consulta.tipo, consulta.lugar.orEmpty())
