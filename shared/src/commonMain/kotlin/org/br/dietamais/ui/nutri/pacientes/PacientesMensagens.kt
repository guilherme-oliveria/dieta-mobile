package org.br.dietamais.ui.nutri.pacientes

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import org.br.dietamais.data.remoto.dto.StatusVinculo
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.ChipStatus
import org.br.dietamais.ui.comum.EstiloStatus
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

@Composable
fun textoMensagemPaciente(mensagem: MensagemPacientes): String = stringResource(when (mensagem) {
    MensagemPacientes.OBRIGATORIO -> Res.string.pacientes_erro_obrigatorio
    MensagemPacientes.EMAIL_INVALIDO -> Res.string.pacientes_erro_email
    MensagemPacientes.DATA_INVALIDA -> Res.string.pacientes_erro_data
    MensagemPacientes.ALTURA_INVALIDA -> Res.string.pacientes_erro_altura
    MensagemPacientes.CAMPO_INVALIDO -> Res.string.pacientes_erro_campo
    MensagemPacientes.SEM_CONEXAO -> Res.string.pacientes_erro_rede
    MensagemPacientes.LIMITE_ATINGIDO -> Res.string.pacientes_limite_texto
    MensagemPacientes.ASSINATURA_INATIVA -> Res.string.pacientes_erro_assinatura
    MensagemPacientes.EMAIL_NAO_CONFIRMADO -> Res.string.pacientes_erro_email_nao_confirmado
    MensagemPacientes.VINCULO_JA_EXISTE -> Res.string.pacientes_erro_vinculo
    MensagemPacientes.TAXA_EXCEDIDA -> Res.string.pacientes_erro_taxa
    MensagemPacientes.CONVITES_INDISPONIVEIS -> Res.string.pacientes_convites_indisponiveis
    MensagemPacientes.CONSENTIMENTO_INDISPONIVEL -> Res.string.pacientes_consentimento_indisponivel
    MensagemPacientes.NAO_ENCONTRADO -> Res.string.pacientes_erro_nao_encontrado
    MensagemPacientes.ERRO_PADRAO -> Res.string.pacientes_erro_generico
    MensagemPacientes.COMPARTILHAMENTO_FALHOU -> Res.string.pacientes_compartilhar_erro
    MensagemPacientes.COMPARTILHAMENTO_ABERTO -> Res.string.pacientes_compartilhar_aberto
    MensagemPacientes.CONVITE_COPIADO -> Res.string.pacientes_convite_copiado
    MensagemPacientes.PACIENTE_CRIADO -> Res.string.pacientes_criado
})

@Composable
fun MensagemPaciente(mensagem: MensagemPacientes, alerta: Boolean = false) {
    Text(textoMensagemPaciente(mensagem), style = MaterialTheme.typography.bodySmall,
        color = if (alerta) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite })
}

@Composable
fun StatusPaciente(status: StatusVinculo) = ChipStatus(
    when (status) { StatusVinculo.ATIVO -> EstiloStatus.FEITA; StatusVinculo.ENCERRADO -> EstiloStatus.ALERTA; else -> EstiloStatus.DEPOIS },
    texto = stringResource(when (status) {
        StatusVinculo.CONVIDADO -> Res.string.pacientes_convidado
        StatusVinculo.ATIVO -> Res.string.pacientes_ativo
        StatusVinculo.ENCERRADO -> Res.string.pacientes_encerrado
        StatusVinculo.OUTRO -> Res.string.pacientes_status_desconhecido
    }))

@Composable
fun EstadoPacientes.erroCampo(campo: String) = errosCampos[campo]?.let { textoMensagemPaciente(it) }
