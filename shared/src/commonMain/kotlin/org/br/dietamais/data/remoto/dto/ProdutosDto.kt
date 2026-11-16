package org.br.dietamais.data.remoto.dto

import kotlinx.serialization.Serializable
import kotlin.time.Instant
import kotlin.uuid.Uuid

/** Produto ou suplemento da nutri (BE-48b, RN-ALI-05). `nutrientes` são por 100 g (ou 100 mL), com as chaves de `/referencias/produtos` (`energia_kcal`, `carboidrato_g`, `proteina_g`, `lipideos_g` e micronutrientes). */
@Serializable
data class ProdutoResposta(val id: Uuid, val nome: String, val marca: String? = null, val classificacao: String? = null, val apresentacao: String? = null,
    val embalagem: String? = null, val sabores: String? = null, val medidaCaseira: String? = null, val medidaGramas: Double? = null, val observacao: String? = null,
    val nutrientes: Map<String, Double> = emptyMap(), val ativo: Boolean = true, val versao: Long, val atualizadoEm: Instant) {
    override fun toString() = "ProdutoResposta[$id]"
}
@Serializable
data class PaginaProdutosResposta(val itens: List<ProdutoResposta>, val pagina: Int, val tamanho: Int, val total: Long)

@Serializable
data class CriarProdutoRequisicao(val id: Uuid? = null, val nome: String, val marca: String? = null, val classificacao: String? = null,
    val apresentacao: String? = null, val embalagem: String? = null, val sabores: String? = null, val medidaCaseira: String? = null,
    val medidaGramas: Double? = null, val observacao: String? = null, val nutrientes: Map<String, Double>)
@Serializable
data class AtualizarProdutoRequisicao(val nome: String, val marca: String? = null, val classificacao: String? = null, val apresentacao: String? = null,
    val embalagem: String? = null, val sabores: String? = null, val medidaCaseira: String? = null, val medidaGramas: Double? = null,
    val observacao: String? = null, val nutrientes: Map<String, Double>, val ativo: Boolean = true)
