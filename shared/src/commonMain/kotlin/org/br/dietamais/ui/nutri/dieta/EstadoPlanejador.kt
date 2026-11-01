package org.br.dietamais.ui.nutri.dieta

import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.sync.novoIdLocal
import org.br.dietamais.dominio.dieta.TotaisPorcoes
import kotlin.uuid.Uuid

enum class MensagemPlanejador {
    TITULO_OBRIGATORIO, NOME_REFEICAO_OBRIGATORIO, HORARIO_INVALIDO, METAS_INVALIDAS, DIETA_VAZIA, LIMITE_EXCEDIDO, SEM_CONEXAO, CONFLITO,
    ASSINATURA_INATIVA, NAO_EDITAVEL, NAO_ENCONTRADA, TAXA_EXCEDIDA, ERRO, SALVA, SO_NO_MONTADOR, SEM_PORCOES,
}

/** Um item da refeição: porção do grupo (quantidade em passos de 0,5), "à vontade" (sem kcal) ou texto livre (fora do catálogo). */
sealed interface ItemPlano {
    val chave: Uuid
    data class DoGrupo(override val chave: Uuid = novoIdLocal(), val grupoId: Uuid, val porcoes: Double = 1.0) : ItemPlano
    data class AVontade(override val chave: Uuid = novoIdLocal(), val grupoId: Uuid) : ItemPlano
    data class TextoLivre(override val chave: Uuid = novoIdLocal(), val texto: String) : ItemPlano
}

/** [alternativaDe] preenchido = refeição ALTERNATIVA (RN-DIE-11): fica fora do total do dia. */
data class RefeicaoPlano(val id: Uuid = novoIdLocal(), val nome: String = "", val horario: String = "08:00",
    val itens: List<ItemPlano> = emptyList(), val alternativaDe: Uuid? = null) { val alternativa get() = alternativaDe != null }

/** [escolhas] = porção escolhida uma vez por grupo (RN-DIE-10). [metas] nula = sem alvo definido. */
data class RascunhoPlano(
    val dietaId: Uuid = novoIdLocal(), val versao: Long? = null, val status: StatusDieta = StatusDieta.RASCUNHO,
    val titulo: String = "", val orientacao: String = "", val rodape: String = "",
    val metas: MetasDieta? = null, val escolhas: Map<Uuid, Int> = emptyMap(), val refeicoes: List<RefeicaoPlano> = emptyList(),
    val chave: Uuid = novoIdLocal(), val alterado: Boolean = false,
) { val editavel get() = status == StatusDieta.RASCUNHO }

/** Alternativa menos principal (RN-DIE-11). */
data class DiferencaPlano(val kcal: Double, val carboidrato: Double, val proteina: Double, val lipidio: Double)

data class EstadoPlanejador(
    val pacienteId: Uuid? = null, val referencia: PorcoesGruposResposta? = null, val rascunho: RascunhoPlano? = null,
    val totaisRefeicao: Map<Uuid, TotaisPorcoes> = emptyMap(), val diferencas: Map<Uuid, DiferencaPlano> = emptyMap(),
    val totalDia: TotaisPorcoes = TotaisPorcoes(0.0, 0.0, 0.0, 0.0, emptyMap()), val saldo: SaldoMetasResposta? = null,
    val refeicaoAtiva: Uuid? = null, val carregando: Boolean = false, val enviando: Boolean = false, val semConexao: Boolean = false,
    val erro: MensagemPlanejador? = null, val aviso: MensagemPlanejador? = null,
) { override fun toString() = "EstadoPlanejador[conteúdo clínico protegido]" }
