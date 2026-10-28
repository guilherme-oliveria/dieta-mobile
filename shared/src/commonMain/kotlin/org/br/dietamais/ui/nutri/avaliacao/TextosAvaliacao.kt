package org.br.dietamais.ui.nutri.avaliacao

import androidx.compose.runtime.Composable
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.br.dietamais.data.remoto.dto.ClassificacaoImc
import org.br.dietamais.dominio.avaliacao.*
import org.br.dietamais.recursos.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

fun rotuloMedida(medida: MedidaAvaliacao): StringResource = when (medida) {
    MedidaAvaliacao.PESO -> Res.string.avaliacao_medida_peso
    MedidaAvaliacao.ESTATURA -> Res.string.avaliacao_medida_estatura
    MedidaAvaliacao.PESCOCO -> Res.string.avaliacao_medida_pescoco
    MedidaAvaliacao.TORAX -> Res.string.avaliacao_medida_torax
    MedidaAvaliacao.CINTURA -> Res.string.avaliacao_medida_cintura
    MedidaAvaliacao.ABDOMEN -> Res.string.avaliacao_medida_abdomen
    MedidaAvaliacao.QUADRIL -> Res.string.avaliacao_medida_quadril
    MedidaAvaliacao.BRACO -> Res.string.avaliacao_medida_braco
    MedidaAvaliacao.ANTEBRACO -> Res.string.avaliacao_medida_antebraco
    MedidaAvaliacao.COXA -> Res.string.avaliacao_medida_coxa
    MedidaAvaliacao.PANTURRILHA -> Res.string.avaliacao_medida_panturrilha
    MedidaAvaliacao.DOBRA_TRICEPS -> Res.string.avaliacao_medida_dobra_triceps
    MedidaAvaliacao.DOBRA_SUBESCAPULAR -> Res.string.avaliacao_medida_dobra_subescapular
    MedidaAvaliacao.DOBRA_PEITORAL -> Res.string.avaliacao_medida_dobra_peitoral
    MedidaAvaliacao.DOBRA_AXILAR_MEDIA -> Res.string.avaliacao_medida_dobra_axilar_media
    MedidaAvaliacao.DOBRA_SUPRAILIACA -> Res.string.avaliacao_medida_dobra_suprailiaca
    MedidaAvaliacao.DOBRA_ABDOMINAL -> Res.string.avaliacao_medida_dobra_abdominal
    MedidaAvaliacao.DOBRA_COXA -> Res.string.avaliacao_medida_dobra_coxa
    MedidaAvaliacao.PERCENTUAL_GORDURA -> Res.string.avaliacao_medida_percentual_gordura
    MedidaAvaliacao.PESO_GORDO -> Res.string.avaliacao_medida_peso_gordo
    MedidaAvaliacao.PESO_MAGRO -> Res.string.avaliacao_medida_peso_magro
}

fun rotuloSecao(secao: SecaoAvaliacao): StringResource = when (secao) {
    SecaoAvaliacao.BASICAS -> Res.string.avaliacao_secao_basicas
    SecaoAvaliacao.PERIMETROS -> Res.string.avaliacao_secao_perimetros
    SecaoAvaliacao.DOBRAS -> Res.string.avaliacao_secao_dobras
    SecaoAvaliacao.COMPOSICAO -> Res.string.avaliacao_secao_composicao
}

fun rotuloClassificacao(classificacao: ClassificacaoImc): StringResource = when (classificacao) {
    ClassificacaoImc.ABAIXO_DO_PESO -> Res.string.avaliacao_class_abaixo_do_peso
    ClassificacaoImc.NORMAL -> Res.string.avaliacao_class_normal
    ClassificacaoImc.SOBREPESO -> Res.string.avaliacao_class_sobrepeso
    ClassificacaoImc.OBESIDADE_I -> Res.string.avaliacao_class_obesidade_i
    ClassificacaoImc.OBESIDADE_II -> Res.string.avaliacao_class_obesidade_ii
    ClassificacaoImc.OBESIDADE_III -> Res.string.avaliacao_class_obesidade_iii
    ClassificacaoImc.OUTRO -> Res.string.avaliacao_class_outro
}

@Composable
fun textoErroMedida(erro: ErroMedida): String = stringResource(when (erro) {
    ErroMedida.OBRIGATORIA -> Res.string.avaliacao_erro_obrigatoria
    ErroMedida.INVALIDA -> Res.string.avaliacao_erro_invalida
    ErroMedida.FORA_DO_INTERVALO -> Res.string.avaliacao_erro_faixa
    ErroMedida.CASAS_DECIMAIS -> Res.string.avaliacao_erro_casas
})

@Composable
fun textoMensagemAvaliacao(mensagem: MensagemAvaliacao): String = stringResource(when (mensagem) {
    MensagemAvaliacao.SEM_CONEXAO -> Res.string.avaliacao_msg_sem_conexao
    MensagemAvaliacao.CONFLITO -> Res.string.avaliacao_msg_conflito
    MensagemAvaliacao.ASSINATURA_INATIVA -> Res.string.avaliacao_msg_assinatura
    MensagemAvaliacao.VINCULO_ENCERRADO -> Res.string.avaliacao_msg_vinculo
    MensagemAvaliacao.JUSTIFICATIVA -> Res.string.avaliacao_msg_justificativa
    MensagemAvaliacao.NAO_ENCONTRADO -> Res.string.avaliacao_msg_nao_encontrada
    MensagemAvaliacao.TAXA_EXCEDIDA -> Res.string.avaliacao_msg_taxa
    MensagemAvaliacao.ERRO -> Res.string.avaliacao_msg_erro
    MensagemAvaliacao.SALVO -> Res.string.avaliacao_msg_salvo
})

/** dd/mm/aaaa no fuso do aparelho. */
fun textoData(instante: Instant): String {
    val data = instante.toLocalDateTime(TimeZone.currentSystemDefault()).date
    return "${data.day.toString().padStart(2, '0')}/${data.month.ordinal.plus(1).toString().padStart(2, '0')}/${data.year}"
}

/** Variação com sinal e vírgula: +1,5 / -4 / 0. */
fun textoVariacao(delta: Double): String = when {
    delta > 0 -> "+" + textoDecimal(delta)
    else -> textoDecimal(delta)
}
