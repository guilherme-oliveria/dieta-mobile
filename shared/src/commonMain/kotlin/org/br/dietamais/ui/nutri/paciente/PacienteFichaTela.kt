package org.br.dietamais.ui.nutri.paciente

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.nutri.pacientes.*
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

@Composable
fun PacienteFichaTela(estado: EstadoPacientes, aoAba: (AbaFicha) -> Unit, aoVoltar: () -> Unit, aoConvidar: () -> Unit,
    abaConteudo: @Composable (AbaFicha, PacienteResposta) -> Unit = { _, _ -> }) {
    val paciente = estado.selecionado
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Dimens.margemCompacta), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
            BotaoTexto(stringResource(Res.string.pacientes_voltar), aoVoltar)
            if (paciente == null) {
                if (estado.atualizando) EstadoCarregando() else EstadoVazio(mensagem = stringResource(Res.string.pacientes_erro_nao_encontrado))
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
                    AvatarPaciente(paciente.nome)
                    Text(paciente.nome, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f).semantics { heading() })
                }
                StatusPaciente(paciente.status)
                if (estado.semConexao) FaixaOffline()
                estado.erro?.let { MensagemPaciente(it, alerta = true) }
                estado.aviso?.let { MensagemPaciente(it) }
                @Suppress("DEPRECATION")
                ScrollableTabRow(selectedTabIndex = estado.abaFicha.ordinal, edgePadding = Dimens.nenhum) {
                    AbaFicha.entries.forEach { aba -> Tab(selected = estado.abaFicha == aba, onClick = { aoAba(aba) },
                        text = { Text(stringResource(when (aba) {
                            AbaFicha.RESUMO -> Res.string.pacientes_resumo
                            AbaFicha.AVALIACOES -> Res.string.pacientes_avaliacoes
                            AbaFicha.DIETA -> Res.string.aba_dieta
                            AbaFicha.LISTA -> Res.string.pacientes_lista
                            AbaFicha.ANAMNESE -> Res.string.pacientes_anamnese
                        })) }) }
                }
                if (estado.abaFicha == AbaFicha.LISTA || estado.abaFicha == AbaFicha.DIETA || estado.abaFicha == AbaFicha.AVALIACOES) abaConteudo(estado.abaFicha, paciente)
                else if (estado.abaFicha != AbaFicha.RESUMO) EstadoVazio()
                else CardDieta(Modifier.fillMaxWidth()) {
                    DadoFicha(stringResource(Res.string.auth_email), paciente.email)
                    DadoFicha(stringResource(Res.string.pacientes_nascimento), paciente.dataNascimento?.let { "${it.day.toString().padStart(2, '0')}/${(it.month.ordinal + 1).toString().padStart(2, '0')}/${it.year}" })
                    DadoFicha(stringResource(Res.string.pacientes_sexo), stringResource(when (paciente.sexo) {
                        Sexo.FEMININO -> Res.string.pacientes_feminino
                        Sexo.MASCULINO -> Res.string.pacientes_masculino
                        Sexo.OUTRO -> Res.string.pacientes_outro
                        else -> Res.string.pacientes_nao_informado
                    }))
                    DadoFicha(stringResource(Res.string.pacientes_telefone), paciente.telefone)
                    DadoFicha(stringResource(Res.string.pacientes_altura), paciente.estaturaReferencia?.toString()?.replace('.', ','))
                    DadoFicha(stringResource(Res.string.pacientes_objetivo), paciente.objetivo)
                    DadoFicha(stringResource(Res.string.pacientes_observacoes), paciente.observacoes)
                }
                if (paciente.status == StatusVinculo.CONVIDADO) BotaoPrincipal(stringResource(Res.string.pacientes_gerar_convite), aoConvidar,
                    Modifier.fillMaxWidth(), habilitado = !estado.semConexao, carregando = estado.envioEmAndamento)
            }
        }
    }
}

@Composable
private fun DadoFicha(rotulo: String, valor: String?) {
    Text(rotulo, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(valor ?: stringResource(Res.string.pacientes_nao_informado), style = MaterialTheme.typography.bodyMedium)
}
