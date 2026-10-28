package org.br.dietamais.data.remoto.dto

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class MudancaSyncResposta<T>(val id: Uuid, val versao: Long, val excluido: Boolean, val dados: T? = null)

@Serializable
data class MudancasFase1Resposta(val pacientes: List<MudancaSyncResposta<PacienteResposta>>,
    val vinculos: List<MudancaSyncResposta<VinculoResposta>>, val nutris: List<MudancaSyncResposta<NutricionistaResumoResposta>>,
    val grupos: List<MudancaSyncResposta<GrupoAlimentoResposta>> = emptyList(),
    val alimentos: List<MudancaSyncResposta<AlimentoResposta>> = emptyList(),
    val listas: List<MudancaSyncResposta<ListaSubstituicaoResposta>> = emptyList(),
    val dietas: List<MudancaSyncResposta<DietaResposta>> = emptyList(),
    val snapshots: List<MudancaSyncResposta<DietaSnapshotResposta>> = emptyList(),
    val dietaEmUso: List<MudancaSyncResposta<DietaEmUsoResposta>> = emptyList(),
    val checkins: List<MudancaSyncResposta<CheckinResposta>> = emptyList(),
    val agua: List<MudancaSyncResposta<AguaResposta>> = emptyList(),
    val pesos: List<MudancaSyncResposta<PesoResposta>> = emptyList(),
    val avaliacoes: List<MudancaSyncResposta<AvaliacaoResposta>> = emptyList()) {
    val total get() = pacientes.size + vinculos.size + nutris.size + grupos.size + alimentos.size + listas.size + dietas.size +
        snapshots.size + dietaEmUso.size + checkins.size + agua.size + pesos.size + avaliacoes.size
}

@Serializable
data class SyncResposta(val cursor: String, val temMais: Boolean, val mudancas: MudancasFase1Resposta)
