package org.br.dietamais.dominio.rotina

import kotlinx.datetime.*
import org.br.dietamais.data.remoto.dto.RefeicaoResposta
import kotlin.time.Instant
import kotlin.uuid.Uuid

/** Janela de silêncio (RN-NOT-03); pode cruzar a meia-noite. Início = fim não silencia nada. */
data class JanelaSilencio(val ativo: Boolean = true, val inicio: LocalTime = LocalTime(22, 0), val fim: LocalTime = LocalTime(7, 0)) {
    fun contem(hora: LocalTime): Boolean = when {
        !ativo || inicio == fim -> false
        inicio < fim -> hora >= inicio && hora < fim
        else -> hora >= inicio || hora < fim
    }
}

/** Notificação local de refeição (RN-ROT-05). [refeicao] é o nome; o texto final é montado na camada de apresentação. */
data class Lembrete(val chave: String, val quando: Instant, val refeicaoId: Uuid, val refeicao: String,
    /** Dia da refeição e dieta de origem: preenchidos para que a notificação registre o check-in (MOB-18b). */
    val dia: LocalDate? = null, val dietaId: Uuid? = null)

/** Limite do iOS: 64 notificações locais pendentes. */
const val LIMITE_LEMBRETES = 64

/**
 * Agenda um lembrete por refeição/dia para os próximos [dias] dias, no horário da refeição. Ignora o que já passou,
 * o que cai no silêncio e a refeição de hoje que já tem check-in ([feitasHoje]). Devolve no máximo [limite], em ordem.
 */
fun planejarLembretes(refeicoes: List<RefeicaoResposta>, agora: Instant, fuso: TimeZone, silencio: JanelaSilencio = JanelaSilencio(),
    feitasHoje: Set<Uuid> = emptySet(), dias: Int = 7, limite: Int = LIMITE_LEMBRETES): List<Lembrete> {
    val hoje = agora.toLocalDateTime(fuso).date
    return (0 until dias).flatMap { delta ->
        val dia = hoje.plus(DatePeriod(days = delta))
        refeicoes.mapNotNull { r ->
            val quando = LocalDateTime(dia, r.horario).toInstant(fuso)
            when {
                quando <= agora -> null
                delta == 0 && r.id in feitasHoje -> null
                silencio.contem(r.horario) -> null
                else -> Lembrete("${r.id}-$dia", quando, r.id, r.nome, dia)
            }
        }
    }.sortedBy { it.quando }.take(limite)
}

/** Botões da notificação de refeição. `TROQUEI` só abre o app: a troca precisa escolher o alimento (RN-ROT). */
enum class AcaoLembrete { COMI, TROQUEI, PULEI }

data class RotulosAcoes(val comi: String, val troquei: String, val pulei: String)

/** Textos já montados (sem dado clínico) da notificação local; [corpo] recebe o nome da refeição. */
data class TextosLembrete(val titulo: String, val corpo: (String) -> String, val acoes: RotulosAcoes)
