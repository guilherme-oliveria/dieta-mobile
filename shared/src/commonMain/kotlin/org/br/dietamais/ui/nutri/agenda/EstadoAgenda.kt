package org.br.dietamais.ui.nutri.agenda

import kotlinx.datetime.LocalDate
import org.br.dietamais.data.remoto.dto.ConsultaResposta
import org.br.dietamais.data.remoto.dto.PacienteResposta
import org.br.dietamais.data.remoto.dto.StatusVinculo
import org.br.dietamais.dominio.agenda.ErroConsulta
import org.br.dietamais.dominio.agenda.FormularioConsulta
import kotlin.uuid.Uuid

enum class ModoAgenda { DIA, SEMANA }

enum class MensagemAgenda { SEM_CONEXAO, CONFLITO, ASSINATURA_INATIVA, VINCULO_ENCERRADO, NAO_ENCONTRADA, JA_CANCELADA, TAXA_EXCEDIDA, DATA_INVALIDA, ERRO, AGENDADA, REMARCADA, CANCELADA }

data class EstadoAgenda(val dia: LocalDate, val modo: ModoAgenda = ModoAgenda.DIA, val consultas: List<ConsultaResposta> = emptyList(), val pacientes: List<PacienteResposta> = emptyList(),
    val atualizando: Boolean = false, val enviando: Boolean = false, val semConexao: Boolean = false, val formulario: FormularioConsulta? = null,
    val errosFormulario: Set<ErroConsulta> = emptySet(), val conflitosAvisados: Int = 0, val cancelandoId: Uuid? = null,
    val erro: MensagemAgenda? = null, val aviso: MensagemAgenda? = null) {
    /** Quem pode receber consulta: vínculo convidado ou ativo (encerrado só lê, RN-VIN-05). */
    val pacientesAgendaveis: List<PacienteResposta> get() = pacientes.filter { it.status == StatusVinculo.CONVIDADO || it.status == StatusVinculo.ATIVO }.sortedBy { it.nome.lowercase() }
    fun nomeDo(pacienteId: Uuid): String? = pacientes.firstOrNull { it.id == pacienteId }?.nome
}
