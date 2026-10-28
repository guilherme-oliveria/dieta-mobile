package org.br.dietamais.dominio.rotina

import kotlinx.datetime.LocalTime
import org.br.dietamais.data.remoto.dto.DietaSnapshotResposta
import org.br.dietamais.data.remoto.dto.RefeicaoResposta
import kotlin.uuid.Uuid

/**
 * Dieta em uso do paciente (RN-DIE-07): a escolhida, se ainda existir nos snapshots; senão a publicada mais recente.
 * O backend já resolve a sucessora; aqui só se evita tela vazia quando a seleção aponta para algo que não chegou.
 */
fun dietaVigente(snapshots: List<DietaSnapshotResposta>, emUsoId: Uuid?): DietaSnapshotResposta? {
    val maisRecentes = snapshots.sortedByDescending { it.publicadoEm }
    return emUsoId?.let { id -> maisRecentes.firstOrNull { it.dietaId == id } } ?: maisRecentes.firstOrNull()
}

/** Primeira refeição cujo horário ainda não passou; null quando o dia terminou. */
fun proximaRefeicao(refeicoes: List<RefeicaoResposta>, agora: LocalTime): RefeicaoResposta? =
    refeicoes.sortedWith(compareBy({ it.horario }, { it.ordem })).firstOrNull { it.horario >= agora }

fun refeicoesOrdenadas(refeicoes: List<RefeicaoResposta>): List<RefeicaoResposta> =
    refeicoes.sortedWith(compareBy({ it.horario }, { it.ordem }))
