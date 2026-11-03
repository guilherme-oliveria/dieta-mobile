package org.br.dietamais.ui.nutri.alimentos

import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.sync.novoIdLocal
import kotlin.uuid.Uuid

enum class CampoAlimento { NOME, MEDIDA, GRAMAS }
enum class MensagemAlimentos { OBRIGATORIO, POSITIVO, CAMPO_INVALIDO, SEM_CONEXAO, INDISPONIVEL, CONFLITO, ASSINATURA_INATIVA, TAXA_EXCEDIDA, ERRO, SALVO }
data class FormularioAlimento(val id: Uuid = novoIdLocal(), val versao: Long? = null, val grupoId: Uuid? = null,
    val nome: String = "", val medida: String = "", val gramas: String = "", val ativo: Boolean = true, val chave: Uuid = novoIdLocal())
data class EstadoAlimentos(val grupos: List<GrupoAlimentoResposta> = emptyList(), val lista: List<AlimentoResposta> = emptyList(),
    val busca: String = "", val grupoId: Uuid? = null, val ativo: Boolean = true, val atualizando: Boolean = false,
    val enviando: Boolean = false, val semConexao: Boolean = false, val formulario: FormularioAlimento? = null,
    val desativar: AlimentoResposta? = null, val conflito: Boolean = false, val erro: MensagemAlimentos? = null,
    val aviso: MensagemAlimentos? = null, val errosCampos: Map<String, MensagemAlimentos> = emptyMap())
