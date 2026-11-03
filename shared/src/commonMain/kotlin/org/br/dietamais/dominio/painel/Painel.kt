package org.br.dietamais.dominio.painel

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.dominio.agenda.consultasDoDia
import org.br.dietamais.dominio.agenda.diaLocal
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlin.uuid.Uuid

data class ItemPainel(val fichaId: Uuid, val nome: String?, val quando: Instant? = null)

data class ResumoPainel(val consultasHoje: List<ConsultaResposta>, val semCheckin: List<ItemPainel>, val respondidos: List<ItemPainel>,
    val assinatura: AssinaturaResumoResposta?) {
    val vazio: Boolean get() = consultasHoje.isEmpty() && semCheckin.isEmpty() && respondidos.isEmpty()
}

/** Janela dos questionários respondidos que ainda merecem atenção da nutri. */
val JANELA_RESPONDIDOS = 7.days

/**
 * Painel da nutri (MOB-28), só com o que já está no banco local. "Sem check-in há 3 dias" vem dos avisos de BE-24 ainda não
 * lidos (o servidor decide o critério, RN-NOT-01); ler o aviso tira o paciente do painel.
 */
fun montarPainel(consultas: List<ConsultaResposta>, pacientes: List<PacienteResposta>, avisos: List<AvisoResposta>,
    questionarios: List<QuestionarioResposta>, assinatura: AssinaturaResumoResposta?, agora: Instant, fuso: TimeZone): ResumoPainel {
    val nomes = pacientes.associate { it.id to it.nome }
    val hoje = diaLocal(agora, fuso)
    val doDia = consultasDoDia(consultas, hoje, fuso).filter { it.status == StatusConsulta.AGENDADA }
    val semCheckin = avisos.filter { it.tipo == TipoAviso.PACIENTE_SEM_CHECKIN && !it.lido }
        .mapNotNull { aviso -> fichaDaRota(aviso.rota)?.let { ItemPainel(it, nomes[it], aviso.criadoEm) } }
        .distinctBy { it.fichaId }
    val respondidos = questionarios.filter { it.status == StatusQuestionario.RESPONDIDO && it.respondidoEm != null && agora - it.respondidoEm <= JANELA_RESPONDIDOS }
        .sortedByDescending { it.respondidoEm }.map { ItemPainel(it.pacienteId, nomes[it.pacienteId], it.respondidoEm) }
    return ResumoPainel(doDia, semCheckin, respondidos, assinatura)
}

/** `/pacientes/{fichaId}` -> id da ficha. */
fun fichaDaRota(rota: String?): Uuid? {
    val partes = rota?.split('/')?.filter { it.isNotEmpty() } ?: return null
    return if (partes.size == 2 && partes[0] == "pacientes") runCatching { Uuid.parse(partes[1]) }.getOrNull() else null
}
