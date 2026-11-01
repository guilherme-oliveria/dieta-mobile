package org.br.dietamais.ui.nutri.dieta

import org.br.dietamais.data.remoto.dto.ModeloDietaResposta

enum class MensagemModelos { SEM_CONEXAO, NAO_ENCONTRADA, SOMENTE_LEITURA, CONFLITO, LIMITE, ASSINATURA_INATIVA, TAXA_EXCEDIDA, ERRO, DUPLICADO, APAGADO, SALVO }

enum class AbaModelos { SISTEMA, MEUS }

/** [apagar] = modelo à espera da confirmação de exclusão. */
data class EstadoModelos(
    val modelos: List<ModeloDietaResposta> = emptyList(), val aba: AbaModelos = AbaModelos.SISTEMA, val apagar: ModeloDietaResposta? = null,
    val carregando: Boolean = false, val enviando: Boolean = false, val semConexao: Boolean = false, val carregado: Boolean = false,
    val erro: MensagemModelos? = null, val aviso: MensagemModelos? = null,
) {
    val visiveis: List<ModeloDietaResposta> get() = modelos.filter { it.doSistema == (aba == AbaModelos.SISTEMA) }
    override fun toString() = "EstadoModelos[conteúdo clínico protegido]"
}
