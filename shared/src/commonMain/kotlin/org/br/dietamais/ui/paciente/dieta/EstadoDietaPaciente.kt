package org.br.dietamais.ui.paciente.dieta

import org.br.dietamais.data.remoto.dto.DietaSnapshotResposta
import org.br.dietamais.data.remoto.dto.RefeicaoResposta
import kotlin.uuid.Uuid

enum class MensagemDietaPaciente { SEM_CONEXAO, CONFLITO, ERRO }

data class EstadoDietaPaciente(
    val carregado: Boolean = false, val dieta: DietaSnapshotResposta? = null, val refeicoes: List<RefeicaoResposta> = emptyList(),
    val proxima: RefeicaoResposta? = null, val alternativas: List<DietaSnapshotResposta> = emptyList(),
    val versaoSelecao: Long = 0, val semConexao: Boolean = false, val enviando: Boolean = false, val erro: MensagemDietaPaciente? = null,
    val refeicaoAberta: Uuid? = null,
) { override fun toString() = "EstadoDietaPaciente[conteúdo clínico protegido]" }
