package org.br.dietamais.ui.nutri.agenda

import androidx.compose.runtime.Composable
import kotlinx.datetime.DayOfWeek
import org.br.dietamais.data.remoto.dto.TipoConsulta
import org.br.dietamais.dominio.agenda.ErroConsulta
import org.br.dietamais.recursos.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun textoMensagemAgenda(mensagem: MensagemAgenda): String = stringResource(when (mensagem) {
    MensagemAgenda.SEM_CONEXAO -> Res.string.agenda_msg_sem_conexao
    MensagemAgenda.CONFLITO -> Res.string.agenda_msg_conflito
    MensagemAgenda.ASSINATURA_INATIVA -> Res.string.agenda_msg_assinatura
    MensagemAgenda.VINCULO_ENCERRADO -> Res.string.agenda_msg_vinculo
    MensagemAgenda.NAO_ENCONTRADA -> Res.string.agenda_msg_nao_encontrada
    MensagemAgenda.JA_CANCELADA -> Res.string.agenda_msg_ja_cancelada
    MensagemAgenda.TAXA_EXCEDIDA -> Res.string.agenda_msg_taxa
    MensagemAgenda.DATA_INVALIDA -> Res.string.agenda_msg_data_invalida
    MensagemAgenda.ERRO -> Res.string.agenda_msg_erro
    MensagemAgenda.AGENDADA -> Res.string.agenda_msg_agendada
    MensagemAgenda.REMARCADA -> Res.string.agenda_msg_remarcada
    MensagemAgenda.CANCELADA -> Res.string.agenda_msg_cancelada
})

@Composable
fun rotuloTipoConsulta(tipo: TipoConsulta): String = stringResource(when (tipo) {
    TipoConsulta.PRIMEIRA -> Res.string.agenda_tipo_primeira
    TipoConsulta.RETORNO -> Res.string.agenda_tipo_retorno
    TipoConsulta.ONLINE -> Res.string.agenda_tipo_online
    TipoConsulta.OUTRO -> Res.string.agenda_tipo_outro
})

@Composable
fun textoErroConsulta(erro: ErroConsulta): String = stringResource(when (erro) {
    ErroConsulta.PACIENTE -> Res.string.agenda_erro_paciente
    ErroConsulta.DATA -> Res.string.agenda_erro_data
    ErroConsulta.HORA -> Res.string.agenda_erro_hora
    ErroConsulta.DURACAO -> Res.string.agenda_erro_duracao
    ErroConsulta.PASSADO -> Res.string.agenda_erro_passado
})

@Composable
fun nomeDiaSemana(dia: DayOfWeek): String = stringResource(when (dia) {
    DayOfWeek.MONDAY -> Res.string.agenda_dia_seg
    DayOfWeek.TUESDAY -> Res.string.agenda_dia_ter
    DayOfWeek.WEDNESDAY -> Res.string.agenda_dia_qua
    DayOfWeek.THURSDAY -> Res.string.agenda_dia_qui
    DayOfWeek.FRIDAY -> Res.string.agenda_dia_sex
    DayOfWeek.SATURDAY -> Res.string.agenda_dia_sab
    DayOfWeek.SUNDAY -> Res.string.agenda_dia_dom
})
