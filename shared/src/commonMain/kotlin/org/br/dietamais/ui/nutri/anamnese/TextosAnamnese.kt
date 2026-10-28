package org.br.dietamais.ui.nutri.anamnese

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import org.br.dietamais.data.remoto.dto.OperadorCondicao
import org.br.dietamais.data.remoto.dto.TipoPergunta
import org.br.dietamais.dominio.anamnese.ErroModelo
import org.br.dietamais.dominio.anamnese.ErroResposta
import org.br.dietamais.recursos.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun textoMensagemAnamnese(mensagem: MensagemAnamnese): String = stringResource(when (mensagem) {
    MensagemAnamnese.SEM_CONEXAO -> Res.string.anamnese_msg_sem_conexao
    MensagemAnamnese.CONFLITO -> Res.string.anamnese_msg_conflito
    MensagemAnamnese.ASSINATURA_INATIVA -> Res.string.anamnese_msg_assinatura
    MensagemAnamnese.VINCULO_ENCERRADO -> Res.string.anamnese_msg_vinculo
    MensagemAnamnese.NAO_ENCONTRADO -> Res.string.anamnese_msg_nao_encontrado
    MensagemAnamnese.TAXA_EXCEDIDA -> Res.string.anamnese_msg_taxa
    MensagemAnamnese.MODELO_INVALIDO -> Res.string.anamnese_msg_modelo_invalido
    MensagemAnamnese.ERRO -> Res.string.anamnese_msg_erro
    MensagemAnamnese.MODELO_SALVO -> Res.string.anamnese_msg_modelo_salvo
    MensagemAnamnese.ENVIADO -> Res.string.anamnese_msg_enviado
    MensagemAnamnese.FECHADO -> Res.string.anamnese_msg_fechado
    MensagemAnamnese.PENDENTES -> Res.string.anamnese_msg_pendentes
    MensagemAnamnese.OBRIGATORIAS -> Res.string.anamnese_msg_obrigatorias
    MensagemAnamnese.ENTRADA_INVALIDA -> Res.string.anamnese_msg_entrada_invalida
    MensagemAnamnese.CONCLUIDO -> Res.string.anamnese_msg_concluido
})

private val mensagensDeSucesso = setOf(MensagemAnamnese.MODELO_SALVO, MensagemAnamnese.ENVIADO, MensagemAnamnese.CONCLUIDO)

@Composable
internal fun MensagensAnamnese(erro: MensagemAnamnese?, aviso: MensagemAnamnese?) {
    erro?.let {
        val sucesso = it in mensagensDeSucesso
        Text(textoMensagemAnamnese(it), color = if (sucesso) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.semantics { liveRegion = if (sucesso) LiveRegionMode.Polite else LiveRegionMode.Assertive })
    }
    aviso?.let { Text(textoMensagemAnamnese(it), color = MaterialTheme.colorScheme.primary, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
}

@Composable
fun rotuloTipo(tipo: TipoPergunta): String = stringResource(when (tipo) {
    TipoPergunta.TEXTO -> Res.string.anamnese_tipo_texto
    TipoPergunta.NUMERO -> Res.string.anamnese_tipo_numero
    TipoPergunta.ESCOLHA_UNICA -> Res.string.anamnese_tipo_escolha_unica
    TipoPergunta.MULTIPLA_ESCOLHA -> Res.string.anamnese_tipo_multipla
    TipoPergunta.ESCALA_1_5 -> Res.string.anamnese_tipo_escala
    TipoPergunta.HORARIO -> Res.string.anamnese_tipo_horario
    TipoPergunta.LISTA_POR_REFEICAO -> Res.string.anamnese_tipo_lista
    TipoPergunta.OUTRO -> Res.string.anamnese_tipo_outro
})

@Composable
fun rotuloOperador(operador: OperadorCondicao): String = stringResource(when (operador) {
    OperadorCondicao.IGUAL -> Res.string.anamnese_op_igual
    OperadorCondicao.DIFERENTE -> Res.string.anamnese_op_diferente
    OperadorCondicao.CONTEM -> Res.string.anamnese_op_contem
    OperadorCondicao.OUTRO -> Res.string.anamnese_tipo_outro
})

@Composable
fun textoErroModelo(erro: ErroModelo): String = stringResource(when (erro) {
    ErroModelo.NOME_VAZIO -> Res.string.anamnese_erro_nome_vazio
    ErroModelo.SEM_PERGUNTAS -> Res.string.anamnese_erro_sem_perguntas
    ErroModelo.BLOCO_SEM_TITULO -> Res.string.anamnese_erro_bloco_sem_titulo
    ErroModelo.PERGUNTA_SEM_TEXTO -> Res.string.anamnese_erro_pergunta_sem_texto
    ErroModelo.OPCOES_INSUFICIENTES -> Res.string.anamnese_erro_opcoes_insuficientes
    ErroModelo.OPCOES_REPETIDAS -> Res.string.anamnese_erro_opcoes_repetidas
    ErroModelo.CONDICAO_INCOMPLETA -> Res.string.anamnese_erro_condicao_incompleta
    ErroModelo.CONDICAO_INVALIDA -> Res.string.anamnese_erro_condicao_invalida
})

@Composable
fun textoErroResposta(erro: ErroResposta): String = stringResource(when (erro) {
    ErroResposta.OBRIGATORIA -> Res.string.anamnese_pac_erro_obrigatoria
    ErroResposta.INVALIDA -> Res.string.anamnese_pac_erro_invalida
})
