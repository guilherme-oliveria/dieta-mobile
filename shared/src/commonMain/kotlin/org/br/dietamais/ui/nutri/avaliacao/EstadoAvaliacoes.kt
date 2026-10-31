package org.br.dietamais.ui.nutri.avaliacao

import org.br.dietamais.data.remoto.dto.AvaliacaoResposta
import org.br.dietamais.dominio.avaliacao.ErroMedida
import kotlinx.datetime.LocalDate
import org.br.dietamais.data.remoto.dto.CompartilhamentoAvaliacaoDto
import org.br.dietamais.data.remoto.dto.Sexo
import org.br.dietamais.dominio.avaliacao.MedidaAvaliacao
import org.br.dietamais.dominio.avaliacao.PassoAvaliacao
import org.br.dietamais.dominio.calculos.ProtocoloDobras
import org.br.dietamais.dominio.calculos.ProtocoloMassa
import kotlin.time.Instant
import kotlin.uuid.Uuid

enum class MensagemAvaliacao { SEM_CONEXAO, CONFLITO, ASSINATURA_INATIVA, VINCULO_ENCERRADO, JUSTIFICATIVA, NAO_ENCONTRADO, TAXA_EXCEDIDA, ERRO, SALVO }

/** `id`/`versao` nulos = nova avaliação. `exigeJustificativa` liga o campo quando a edição passa de 30 dias (RN-AVA-04). */
data class FormularioAvaliacao(val id: Uuid? = null, val versao: Long? = null, val dataHora: Instant? = null,
    val valores: Map<MedidaAvaliacao, String> = emptyMap(), val parecer: String = "", val compartilhada: Boolean = true,
    val justificativa: String = "", val exigeJustificativa: Boolean = false, val chave: Uuid? = null,
    val passo: PassoAvaliacao = PassoAvaliacao.MEDIDAS, val protocoloDobras: ProtocoloDobras = ProtocoloDobras.SETE,
    val protocoloMassa: ProtocoloMassa = ProtocoloMassa.MAGRO, val compartilhamento: CompartilhamentoAvaliacaoDto = CompartilhamentoAvaliacaoDto(),
    /** Parecer ainda não mexido pela nutri: ao chegar no resumo, entra o texto gerado, que ela pode editar. */
    val parecerPreenchidoAuto: Boolean = false)

/** `sexo` e `nascimento` do paciente só alimentam a prévia local; o servidor recalcula ao salvar (RN-CAL-01). */
data class EstadoAvaliacoes(val pacienteId: Uuid? = null, val sexo: Sexo? = null, val nascimento: LocalDate? = null, val somenteLeitura: Boolean = false, val itens: List<AvaliacaoResposta> = emptyList(), val atualizando: Boolean = false,
    val enviando: Boolean = false, val semConexao: Boolean = false, val formulario: FormularioAvaliacao? = null,
    val errosMedidas: Map<MedidaAvaliacao, ErroMedida> = emptyMap(), val justificativaFaltando: Boolean = false,
    val selecionadas: List<Uuid> = emptyList(), val comparando: Boolean = false, val erro: MensagemAvaliacao? = null,
    val aviso: MensagemAvaliacao? = null) {
    val comparacao: Pair<AvaliacaoResposta, AvaliacaoResposta>? get() {
        val a = itens.firstOrNull { it.id == selecionadas.getOrNull(0) } ?: return null
        val b = itens.firstOrNull { it.id == selecionadas.getOrNull(1) } ?: return null
        return if (a.dataHora <= b.dataHora) a to b else b to a
    }
}
