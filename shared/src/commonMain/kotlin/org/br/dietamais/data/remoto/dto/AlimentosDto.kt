package org.br.dietamais.data.remoto.dto

import kotlinx.serialization.Serializable
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Serializable
data class GrupoAlimentoResposta(val id: Uuid, val nome: String, val kcalPorPorcao: Double,
    val carboidratosPorPorcao: Double, val proteinasPorPorcao: Double, val gordurasPorPorcao: Double)
@Serializable
data class GruposAlimentosResposta(val itens: List<GrupoAlimentoResposta>)
@Serializable
data class AlimentoResposta(val id: Uuid, val catalogoId: Uuid? = null, val grupoId: Uuid, val nome: String,
    val medidaCaseira: String, val gramasPorPorcao: Double, val ativo: Boolean, val versao: Long, val atualizadoEm: Instant)
@Serializable
data class PaginaAlimentosResposta(val itens: List<AlimentoResposta>, val pagina: Int, val tamanho: Int, val total: Long)
@Serializable
data class CriarAlimentoRequisicao(val id: Uuid? = null, val grupoId: Uuid, val nome: String,
    val medidaCaseira: String, val gramasPorPorcao: Double)
@Serializable
data class AtualizarAlimentoRequisicao(val grupoId: Uuid, val nome: String, val medidaCaseira: String,
    val gramasPorPorcao: Double, val ativo: Boolean)
