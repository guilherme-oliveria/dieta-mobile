package org.br.dietamais.ui.nutri.anamnese

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import org.br.dietamais.data.remoto.dto.ModeloQuestionarioResposta
import org.br.dietamais.data.remoto.dto.QuestionarioResposta
import org.br.dietamais.data.remoto.dto.StatusQuestionario
import org.br.dietamais.dominio.anamnese.*
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.nutri.avaliacao.textoData
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource
import kotlin.uuid.Uuid

data class AcoesAnamneseNutri(val aoEnviar: (Uuid) -> Unit = {}, val aoLer: (Uuid) -> Unit = {}, val aoAtualizar: () -> Unit = {}, val aoGerenciar: (Boolean) -> Unit = {},
    val aoNovoModelo: () -> Unit = {}, val aoEditarModelo: (Uuid) -> Unit = {}, val aoFecharLeitura: () -> Unit = {})

/** Aba Anamnese da ficha: modelos para enviar e questionários do paciente (MOB-24). */
@Composable
fun AnamneseNutriTela(estado: EstadoAnamneseNutri, acoes: AcoesAnamneseNutri) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = Dimens.espacoMedio), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
        if (estado.semConexao) FaixaOffline()
        MensagensAnamnese(estado.erro, estado.aviso)
        Text(stringResource(Res.string.anamnese_enviar_titulo), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        if (estado.modelos.isEmpty()) Text(stringResource(Res.string.anamnese_sem_modelos), style = MaterialTheme.typography.bodyMedium)
        estado.modelos.forEach { LinhaModelo(it, stringResource(Res.string.anamnese_enviar), !estado.enviando && !estado.semConexao) { acoes.aoEnviar(it.id) } }
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno), verticalAlignment = Alignment.CenterVertically) {
            BotaoTexto(stringResource(Res.string.anamnese_gerenciar_modelos), { acoes.aoGerenciar(true) })
            BotaoTexto(stringResource(Res.string.anamnese_atualizar), acoes.aoAtualizar, habilitado = !estado.atualizando && !estado.semConexao)
        }
        Text(stringResource(Res.string.anamnese_questionarios_titulo), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        when {
            estado.atualizando && estado.questionarios.isEmpty() -> EstadoCarregando()
            estado.questionarios.isEmpty() -> EstadoVazio(mensagem = stringResource(Res.string.anamnese_vazio))
            else -> estado.questionarios.forEach { CartaoQuestionario(it) { acoes.aoLer(it.id) } }
        }
    }
}

@Composable
private fun LinhaModelo(modelo: ModeloQuestionarioResposta, rotuloAcao: String, habilitado: Boolean, aoAcionar: () -> Unit) {
    CardDieta(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMinimo)) {
                Text(modelo.nome, style = MaterialTheme.typography.titleSmall)
                if (modelo.padrao) ChipStatus(EstiloStatus.INFO, texto = stringResource(Res.string.anamnese_modelo_padrao))
            }
            BotaoContornado(rotuloAcao, aoAcionar, habilitado = habilitado)
        }
    }
}

@Composable
private fun CartaoQuestionario(q: QuestionarioResposta, aoLer: () -> Unit) {
    CardDieta(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMinimo)) {
                Text(q.nome, style = MaterialTheme.typography.titleSmall)
                Text(stringResource(Res.string.anamnese_enviado_em, textoData(q.enviadoEm)), style = MaterialTheme.typography.bodySmall)
                ChipStatus(if (q.status == StatusQuestionario.RESPONDIDO) EstiloStatus.FEITA else EstiloStatus.INFO,
                    texto = stringResource(if (q.status == StatusQuestionario.RESPONDIDO) Res.string.anamnese_status_respondido else Res.string.anamnese_status_pendente))
            }
            BotaoContornado(stringResource(Res.string.anamnese_ver_respostas), aoLer)
        }
    }
}

/** Respostas do paciente em leitura; perguntas condicionais que não apareceram para ele ficam de fora. */
@Composable
fun RespostasAnamneseTela(questionario: QuestionarioResposta, aoVoltar: () -> Unit) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = Dimens.espacoMedio), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
        BotaoTexto(stringResource(Res.string.anamnese_voltar), aoVoltar)
        Text(questionario.nome, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        ChipStatus(if (questionario.status == StatusQuestionario.RESPONDIDO) EstiloStatus.FEITA else EstiloStatus.INFO,
            texto = stringResource(if (questionario.status == StatusQuestionario.RESPONDIDO) Res.string.anamnese_status_respondido else Res.string.anamnese_status_pendente))
        passos(questionario.blocos, questionario.respostas).forEach { passo ->
            Text(passo.titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            passo.perguntas.forEach { p ->
                CardDieta(Modifier.fillMaxWidth()) {
                    Text(p.texto, style = MaterialTheme.typography.labelLarge)
                    Text(textoDaResposta(p, questionario.respostas[p.chave]) ?: stringResource(Res.string.anamnese_sem_resposta), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
fun ModelosAnamneseTela(estado: EstadoAnamneseNutri, acoes: AcoesAnamneseNutri) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = Dimens.espacoMedio), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
        BotaoTexto(stringResource(Res.string.anamnese_voltar), { acoes.aoGerenciar(false) })
        Text(stringResource(Res.string.anamnese_modelos_titulo), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        if (estado.semConexao) FaixaOffline()
        MensagensAnamnese(estado.erro, estado.aviso)
        BotaoPrincipal(stringResource(Res.string.anamnese_novo_modelo), acoes.aoNovoModelo, habilitado = !estado.semConexao)
        estado.modelos.forEach { LinhaModelo(it, stringResource(Res.string.anamnese_editar), !estado.semConexao) { acoes.aoEditarModelo(it.id) } }
    }
}

@Composable
fun AnamneseNutriRota(viewModel: AnamneseNutriViewModel, pacienteId: Uuid) {
    val estado by viewModel.estado.collectAsState()
    androidx.compose.runtime.LaunchedEffect(pacienteId) { viewModel.abrir(pacienteId) }
    val acoes = AcoesAnamneseNutri(viewModel::enviar, viewModel::ler, viewModel::recarregar, viewModel::gerenciarModelos, viewModel::novoModelo, viewModel::editarModelo, viewModel::fecharLeitura)
    val lendo = estado.lendo
    when {
        lendo != null -> RespostasAnamneseTela(lendo, viewModel::fecharLeitura)
        estado.editor != null -> ModeloAnamneseTela(estado, AcoesEditorModelo(viewModel::mudarNome, viewModel::mudarBloco, viewModel::adicionarBloco, viewModel::removerBloco,
            viewModel::adicionarPergunta, viewModel::removerPergunta, viewModel::moverPergunta, viewModel::mudarPergunta, viewModel::ligarCondicao, viewModel::desligarCondicao,
            viewModel::salvarModelo, viewModel::fecharEditor))
        estado.gerenciandoModelos -> ModelosAnamneseTela(estado, acoes)
        else -> AnamneseNutriTela(estado, acoes)
    }
}
