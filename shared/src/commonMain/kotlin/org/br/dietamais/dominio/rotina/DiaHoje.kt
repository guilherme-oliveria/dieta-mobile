package org.br.dietamais.dominio.rotina

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

enum class TipoDia { HOJE, ONTEM, OUTRO }

/** Como o seletor de dia do Hoje nomeia um dia (MOB-UX-06): "Hoje", "Ontem" ou data com dia da semana. */
fun tipoDia(dia: LocalDate, hoje: LocalDate?): TipoDia = when {
    hoje == null -> TipoDia.OUTRO
    dia == hoje -> TipoDia.HOJE
    dia == hoje.minus(DatePeriod(days = 1)) -> TipoDia.ONTEM
    else -> TipoDia.OUTRO
}

/** "09/11" (dia e mês com dois dígitos). */
fun diaMes(dia: LocalDate): String = "${dia.day.toString().padStart(2, '0')}/${dia.month.ordinal.plus(1).toString().padStart(2, '0')}"
