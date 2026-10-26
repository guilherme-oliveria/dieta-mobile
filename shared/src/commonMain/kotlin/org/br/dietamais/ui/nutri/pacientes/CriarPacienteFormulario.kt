package org.br.dietamais.ui.nutri.pacientes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.KeyboardType
import org.br.dietamais.data.remoto.dto.Sexo
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

@Composable
fun CriarPacienteFormulario(estado: EstadoPacientes, aoCampo: (CampoPaciente, String) -> Unit, aoSexo: (Sexo?) -> Unit,
    aoSalvar: () -> Unit, aoCancelar: () -> Unit, aoAtualizar: () -> Unit = {}) {
    val form = estado.formulario ?: return
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(Dimens.margemCompacta),
        verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
        Text(stringResource(Res.string.pacientes_novo), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        estado.erro?.let { MensagemPaciente(it, alerta = true) }
        CampoTexto(stringResource(Res.string.pacientes_nome), form.nome, { aoCampo(CampoPaciente.NOME, it) }, erro = estado.erroCampo("nome"), habilitado = !estado.envioEmAndamento)
        CampoTexto(stringResource(Res.string.pacientes_email), form.email, { aoCampo(CampoPaciente.EMAIL, it) }, erro = estado.erroCampo("email"), habilitado = !estado.envioEmAndamento,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        CampoTexto(stringResource(Res.string.pacientes_nascimento), form.nascimento, { aoCampo(CampoPaciente.NASCIMENTO, it) }, erro = estado.erroCampo("dataNascimento"), habilitado = !estado.envioEmAndamento)
        Text(stringResource(Res.string.pacientes_sexo), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
            listOf(null, Sexo.FEMININO, Sexo.MASCULINO, Sexo.OUTRO).forEach { sexo ->
                FilterChip(selected = form.sexo == sexo, onClick = { aoSexo(sexo) }, enabled = !estado.envioEmAndamento, modifier = Modifier.heightIn(min = Dimens.alvoToque),
                    label = { Text(stringResource(when (sexo) {
                        Sexo.FEMININO -> Res.string.pacientes_feminino
                        Sexo.MASCULINO -> Res.string.pacientes_masculino
                        Sexo.OUTRO -> Res.string.pacientes_outro
                        else -> Res.string.pacientes_nao_informado
                    })) })
            }
        }
        CampoTexto(stringResource(Res.string.pacientes_telefone), form.telefone, { aoCampo(CampoPaciente.TELEFONE, it) }, erro = estado.erroCampo("telefone"), habilitado = !estado.envioEmAndamento,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
        CampoNumero(stringResource(Res.string.pacientes_altura), form.estatura, { aoCampo(CampoPaciente.ESTATURA, it) }, erro = estado.erroCampo("estaturaReferencia"), habilitado = !estado.envioEmAndamento)
        CampoTexto(stringResource(Res.string.pacientes_objetivo), form.objetivo, { aoCampo(CampoPaciente.OBJETIVO, it) }, erro = estado.erroCampo("objetivo"), habilitado = !estado.envioEmAndamento, singleLine = false)
        CampoTexto(stringResource(Res.string.pacientes_observacoes), form.observacoes, { aoCampo(CampoPaciente.OBSERVACOES, it) }, erro = estado.erroCampo("observacoes"), habilitado = !estado.envioEmAndamento, singleLine = false)
        BotaoPrincipal(stringResource(Res.string.pacientes_salvar), aoSalvar, Modifier.fillMaxWidth(), habilitado = !estado.semConexao, carregando = estado.envioEmAndamento)
        if (estado.semConexao) {
            Text(stringResource(Res.string.pacientes_erro_rede), style = MaterialTheme.typography.bodySmall)
            BotaoTexto(stringResource(Res.string.pacientes_atualizar), aoAtualizar, carregando = estado.atualizando)
        }
        BotaoTexto(stringResource(Res.string.pacientes_cancelar), aoCancelar, Modifier.fillMaxWidth())
    }
}
