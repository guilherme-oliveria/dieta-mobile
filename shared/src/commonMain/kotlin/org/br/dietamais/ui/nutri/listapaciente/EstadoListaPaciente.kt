package org.br.dietamais.ui.nutri.listapaciente

import org.br.dietamais.data.remoto.dto.*
import kotlin.uuid.Uuid

enum class MensagemLista { SEM_CONEXAO, CONFLITO, ASSINATURA_INATIVA, NAO_ENCONTRADO, ERRO, SALVO }

data class EstadoListaPaciente(
    val pacienteId: Uuid? = null, val versao: Long = 0, val grupos: List<GrupoAlimentoResposta> = emptyList(),
    val efetiva: List<AlimentoResposta> = emptyList(), val opcoes: List<AlimentoResposta> = emptyList(),
    val busca: String = "", val pendente: Boolean = false, val carregando: Boolean = false, val enviando: Boolean = false,
    val semConexao: Boolean = false, val erro: MensagemLista? = null, val aviso: MensagemLista? = null,
)
