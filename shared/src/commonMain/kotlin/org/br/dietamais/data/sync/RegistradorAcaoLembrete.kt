package org.br.dietamais.data.sync

import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import org.br.dietamais.data.remoto.dto.StatusCheckin
import org.br.dietamais.data.repositorio.PortaRotina
import org.br.dietamais.dominio.rotina.AcaoLembrete
import kotlin.time.Clock
import kotlin.uuid.Uuid

enum class ResultadoAcaoLembrete { GRAVADO, JA_REGISTRADO, SO_ABRE_O_APP }

/**
 * Botões "Comi" e "Pulei" da notificação (MOB-18b): grava o check-in local e na fila pela mesma porta da tela Hoje (RN-SYN-01), sem abrir
 * o app. "Troquei" não grava nada: a troca exige escolher o alimento, então só abre o app. Não duplica um check-in que já existe.
 */
class RegistradorAcaoLembrete(private val rotina: PortaRotina, private val relogio: Clock = Clock.System) {
    suspend fun registrar(dietaId: Uuid, refeicaoId: Uuid, dia: LocalDate, acao: AcaoLembrete): ResultadoAcaoLembrete {
        val status = when (acao) {
            AcaoLembrete.COMI -> StatusCheckin.FEITO
            AcaoLembrete.PULEI -> StatusCheckin.PULADO
            AcaoLembrete.TROQUEI -> return ResultadoAcaoLembrete.SO_ABRE_O_APP
        }
        val jaTem = rotina.observar().first().checkins.any { it.dietaId == dietaId && it.refeicaoId == refeicaoId && it.data == dia }
        if (jaTem) return ResultadoAcaoLembrete.JA_REGISTRADO
        rotina.registrarCheckin(dietaId, refeicaoId, dia, status, emptyList(), relogio.now())
        return ResultadoAcaoLembrete.GRAVADO
    }
}
