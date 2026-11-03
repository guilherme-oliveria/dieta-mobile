package org.br.dietamais.ui.nutri.anamnese

import org.br.dietamais.data.remoto.dto.ModeloQuestionarioResposta
import org.br.dietamais.data.remoto.dto.QuestionarioResposta
import org.br.dietamais.dominio.anamnese.EditorModelo
import org.br.dietamais.dominio.anamnese.ValidacaoModelo
import kotlin.uuid.Uuid

enum class MensagemAnamnese { SEM_CONEXAO, CONFLITO, ASSINATURA_INATIVA, VINCULO_ENCERRADO, NAO_ENCONTRADO, TAXA_EXCEDIDA, MODELO_INVALIDO, ERRO, MODELO_SALVO, ENVIADO,
    FECHADO, PENDENTES, OBRIGATORIAS, ENTRADA_INVALIDA, CONCLUIDO }

data class EstadoAnamneseNutri(val pacienteId: Uuid? = null, val modelos: List<ModeloQuestionarioResposta> = emptyList(), val questionarios: List<QuestionarioResposta> = emptyList(),
    val atualizando: Boolean = false, val enviando: Boolean = false, val semConexao: Boolean = false, val gerenciandoModelos: Boolean = false,
    val editor: EditorModelo? = null, val validacao: ValidacaoModelo = ValidacaoModelo(), val lendoId: Uuid? = null,
    val erro: MensagemAnamnese? = null, val aviso: MensagemAnamnese? = null) {
    val lendo: QuestionarioResposta? get() = questionarios.firstOrNull { it.id == lendoId }
}
