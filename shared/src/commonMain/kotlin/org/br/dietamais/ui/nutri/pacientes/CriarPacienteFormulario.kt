package org.br.dietamais.ui.nutri.pacientes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.KeyboardType
import org.br.dietamais.data.remoto.dto.Sexo
import org.br.dietamais.dominio.pacientes.*
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

@Composable
fun CriarPacienteFormulario(estado: EstadoPacientes, aoCampo: (CampoPaciente, String) -> Unit, aoSexo: (Sexo?) -> Unit,
    aoSalvar: () -> Unit, aoCancelar: () -> Unit, aoAtualizar: () -> Unit = {},
    aoAmpliado: ((FormularioCadastroAmpliado) -> FormularioCadastroAmpliado) -> Unit = {}) {
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
                ChipEscolha(selected = form.sexo == sexo, onClick = { aoSexo(sexo) }, enabled = !estado.envioEmAndamento, modifier = Modifier.heightIn(min = Dimens.alvoToque),
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
        SecaoCadastroAmpliado(estado, form.ampliado, aoAmpliado)
        BotaoPrincipal(stringResource(Res.string.pacientes_salvar), aoSalvar, Modifier.fillMaxWidth(), carregando = estado.envioEmAndamento)
        BotaoTexto(stringResource(Res.string.pacientes_cancelar), aoCancelar, Modifier.fillMaxWidth())
    }
}

/** Mais dados do cadastro (MOB-78, RN-VIN-08): escolaridade, estado civil, profissão, endereço, convênio, indicação e motivo; tudo opcional. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SecaoCadastroAmpliado(estado: EstadoPacientes, f: FormularioCadastroAmpliado, muda: ((FormularioCadastroAmpliado) -> FormularioCadastroAmpliado) -> Unit) {
    var aberta by remember { mutableStateOf(false) }
    val livre = !estado.envioEmAndamento
    BotaoContornado(stringResource(if (aberta) Res.string.pacientes_ampliado_fechar else Res.string.pacientes_ampliado_abrir), { aberta = !aberta }, Modifier.fillMaxWidth())
    if (!aberta) return
    Text(stringResource(Res.string.pacientes_ampliado_descricao), style = MaterialTheme.typography.bodySmall)
    Text(stringResource(Res.string.pacientes_escolaridade), style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
        SUGESTOES_ESCOLARIDADE.forEach { op -> ChipEscolha(f.escolaridade == op, { muda { it.copy(escolaridade = if (it.escolaridade == op) "" else op) } }, enabled = livre, modifier = Modifier.heightIn(min = Dimens.alvoToque), label = { Text(op) }) }
    }
    CampoTexto(stringResource(Res.string.pacientes_escolaridade_outra), f.escolaridade, { v -> muda { it.copy(escolaridade = v.take(60)) } }, habilitado = livre)
    Text(stringResource(Res.string.pacientes_estado_civil), style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
        SUGESTOES_ESTADO_CIVIL.forEach { op -> ChipEscolha(f.estadoCivil == op, { muda { it.copy(estadoCivil = if (it.estadoCivil == op) "" else op) } }, enabled = livre, modifier = Modifier.heightIn(min = Dimens.alvoToque), label = { Text(op) }) }
    }
    CampoTexto(stringResource(Res.string.pacientes_profissao), f.profissao, { v -> muda { it.copy(profissao = v.take(100)) } }, habilitado = livre)
    CampoTexto(stringResource(Res.string.pacientes_convenio), f.convenio, { v -> muda { it.copy(convenio = v.take(100)) } }, habilitado = livre)
    CampoTexto(stringResource(Res.string.pacientes_indicacao), f.indicacao, { v -> muda { it.copy(indicacao = v.take(150)) } }, habilitado = livre)
    CampoTexto(stringResource(Res.string.pacientes_motivo), f.motivoConsulta, { v -> muda { it.copy(motivoConsulta = v.take(2000)) } }, habilitado = livre, singleLine = false)
    Text(stringResource(Res.string.pacientes_endereco), style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
    CampoTexto(stringResource(Res.string.pacientes_cep), f.cep, { v -> muda { it.copy(cep = v.take(9)) } }, erro = estado.erroCampo("cep"), habilitado = livre, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
    CampoTexto(stringResource(Res.string.pacientes_logradouro), f.logradouro, { v -> muda { it.copy(logradouro = v.take(150)) } }, habilitado = livre)
    CampoTexto(stringResource(Res.string.pacientes_numero), f.numero, { v -> muda { it.copy(numero = v.take(20)) } }, habilitado = livre)
    CampoTexto(stringResource(Res.string.pacientes_complemento), f.complemento, { v -> muda { it.copy(complemento = v.take(80)) } }, habilitado = livre)
    CampoTexto(stringResource(Res.string.pacientes_bairro), f.bairro, { v -> muda { it.copy(bairro = v.take(80)) } }, habilitado = livre)
    CampoTexto(stringResource(Res.string.pacientes_cidade), f.cidade, { v -> muda { it.copy(cidade = v.take(80)) } }, habilitado = livre)
    CampoTexto(stringResource(Res.string.pacientes_uf), f.uf, { v -> muda { it.copy(uf = v.take(2)) } }, erro = estado.erroCampo("uf"), habilitado = livre)
}
