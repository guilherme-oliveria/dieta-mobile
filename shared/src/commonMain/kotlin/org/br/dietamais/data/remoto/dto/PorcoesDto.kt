package org.br.dietamais.data.remoto.dto

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

/** Uma porção do grupo (`GET /referencias/porcoes`, BE-47b); `nutrientes` é código → valor por porção e só traz o que o grupo informa. */
@Serializable
data class OpcaoPorcaoResposta(val opcao: Int, val kcal: Double, val quantidade: Double? = null, val unidade: String? = null,
    val medidaCaseira: String? = null, val guiaAlimentar: Boolean = false, val nutrientes: Map<String, Double> = emptyMap())
@Serializable
data class GrupoPorcoesResposta(val grupoId: Uuid, val nome: String, val opcoes: List<OpcaoPorcaoResposta>)
@Serializable
data class PorcoesGruposResposta(val fonte: String, val unidades: Map<String, String> = emptyMap(), val grupos: List<GrupoPorcoesResposta>)
