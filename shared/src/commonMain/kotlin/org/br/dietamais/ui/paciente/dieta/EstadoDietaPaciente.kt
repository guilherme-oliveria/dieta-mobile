package org.br.dietamais.ui.paciente.dieta

import kotlinx.datetime.LocalDate
import org.br.dietamais.data.remoto.dto.DietaSnapshotResposta
import org.br.dietamais.data.remoto.dto.RefeicaoResposta
import org.br.dietamais.data.remoto.dto.StatusCheckin
import kotlin.uuid.Uuid
import org.br.dietamais.dominio.rotina.*

enum class MensagemDietaPaciente { SEM_CONEXAO, CONFLITO, ERRO }

/** Troca em andamento: [escolhas] mapeia itemId do snapshot -> alimento substituto escolhido (RN-SUB). */
data class EstadoTroca(val refeicaoId: Uuid, val escolhas: Map<Uuid, Uuid> = emptyMap(), val itemId: Uuid? = null)

data class EstadoDietaPaciente(
    val carregado: Boolean = false, val dieta: DietaSnapshotResposta? = null, val refeicoes: List<RefeicaoResposta> = emptyList(),
    val proxima: RefeicaoResposta? = null, val alternativas: List<DietaSnapshotResposta> = emptyList(),
    val versaoSelecao: Long = 0, val semConexao: Boolean = false, val enviando: Boolean = false, val erro: MensagemDietaPaciente? = null,
    val refeicaoAberta: Uuid? = null,
    val hoje: LocalDate? = null, val dia: LocalDate? = null, val dias: List<LocalDate> = emptyList(),
    val checkins: Map<Uuid, StatusCheckin> = emptyMap(), val aguaMl: Int = 0, val metaAguaMl: Int = 2000, val troca: EstadoTroca? = null,
    val listaAberta: Boolean = false, val gruposLista: List<GrupoListaPaciente> = emptyList(), val buscaLista: String = "",
    val grupoListaFiltro: Uuid? = null, val grupoListaAberto: Uuid? = null,
) {
    val gruposListaVisiveis get() = filtrarListaPaciente(gruposLista, buscaLista, grupoListaFiltro)
    val ehHoje get() = dia != null && dia == hoje
    override fun toString() = "EstadoDietaPaciente[conteúdo clínico protegido]"
}
