package org.br.dietamais.ui.nutri.anamnese

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import org.br.dietamais.data.remoto.dto.OperadorCondicao
import org.br.dietamais.data.remoto.dto.TipoPergunta
import org.br.dietamais.dominio.anamnese.*
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource
import kotlin.uuid.Uuid

data class AcoesEditorModelo(val aoNome: (String) -> Unit = {}, val aoTituloBloco: (Int, String) -> Unit = { _, _ -> }, val aoAdicionarBloco: () -> Unit = {},
    val aoRemoverBloco: (Int) -> Unit = {}, val aoAdicionarPergunta: (Int) -> Unit = {}, val aoRemoverPergunta: (Uuid) -> Unit = {}, val aoMover: (Uuid, Int) -> Unit = { _, _ -> },
    val aoMudarPergunta: (Uuid, (PerguntaEditavel) -> PerguntaEditavel) -> Unit = { _, _ -> }, val aoLigarCondicao: (Uuid, Uuid) -> Unit = { _, _ -> },
    val aoDesligarCondicao: (Uuid) -> Unit = {}, val aoSalvar: () -> Unit = {}, val aoCancelar: () -> Unit = {})

/** Editor de modelo (RN-ANA-01/02): blocos, perguntas, tipos, opções e condição de exibição. */
@Composable
fun ModeloAnamneseTela(estado: EstadoAnamneseNutri, acoes: AcoesEditorModelo) {
    val editor = estado.editor ?: return
    val habilitado = !estado.enviando && !estado.semConexao
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = Dimens.larguraLeitura).fillMaxWidth().verticalScroll(rememberScrollState()).padding(Dimens.margemCompacta),
                verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
                Text(stringResource(if (editor.id == null) Res.string.anamnese_editor_titulo_novo else Res.string.anamnese_editor_titulo_editar),
                    style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
                if (estado.semConexao) FaixaOffline()
                MensagensAnamnese(estado.erro, estado.aviso)
                CampoTexto(stringResource(Res.string.anamnese_nome_modelo), editor.nome, acoes.aoNome, erro = estado.validacao.nome?.let { textoErroModelo(it) }, habilitado = habilitado)
                editor.blocos.forEachIndexed { indice, bloco ->
                    CardDieta(Modifier.fillMaxWidth()) {
                        CampoTexto(stringResource(Res.string.anamnese_bloco_titulo), bloco.titulo, { acoes.aoTituloBloco(indice, it) },
                            erro = estado.validacao.blocos[indice]?.let { textoErroModelo(it) }, habilitado = habilitado)
                        bloco.perguntas.forEach { pergunta ->
                            EditorPergunta(editor, pergunta, estado.validacao.perguntas[pergunta.id], habilitado, acoes)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                            BotaoContornado(stringResource(Res.string.anamnese_pergunta_adicionar), { acoes.aoAdicionarPergunta(indice) }, habilitado = habilitado)
                            if (editor.blocos.size > 1) BotaoTexto(stringResource(Res.string.anamnese_bloco_remover), { acoes.aoRemoverBloco(indice) }, habilitado = habilitado)
                        }
                    }
                }
                BotaoContornado(stringResource(Res.string.anamnese_bloco_adicionar), acoes.aoAdicionarBloco, habilitado = habilitado)
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                    BotaoPrincipal(stringResource(Res.string.anamnese_salvar_modelo), acoes.aoSalvar, habilitado = habilitado, carregando = estado.enviando)
                    BotaoTexto(stringResource(Res.string.anamnese_cancelar), acoes.aoCancelar, habilitado = !estado.enviando)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditorPergunta(editor: EditorModelo, pergunta: PerguntaEditavel, erro: ErroModelo?, habilitado: Boolean, acoes: AcoesEditorModelo) {
    val id = pergunta.id
    val anteriores = editor.anteriores(id)
    val referencia = anteriores.firstOrNull { it.id == pergunta.condicaoPergunta }
    Column(Modifier.fillMaxWidth().padding(top = Dimens.espacoMedio), verticalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
        HorizontalDivider()
        CampoTexto(stringResource(Res.string.anamnese_pergunta_texto), pergunta.texto, { novo -> acoes.aoMudarPergunta(id) { it.copy(texto = novo.take(300)) } },
            erro = erro?.takeIf { it == ErroModelo.PERGUNTA_SEM_TEXTO }?.let { textoErroModelo(it) }, habilitado = habilitado, singleLine = false)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
            tiposEditaveis.forEach { tipo ->
                FilterChip(pergunta.tipo == tipo, { acoes.aoMudarPergunta(id) { it.copy(tipo = tipo) } }, label = { Text(rotuloTipo(tipo)) }, enabled = habilitado)
            }
        }
        if (tipoTemOpcoes(pergunta.tipo)) CampoTexto(stringResource(Res.string.anamnese_pergunta_opcoes), pergunta.opcoes, { novo -> acoes.aoMudarPergunta(id) { it.copy(opcoes = novo.take(2000)) } },
            erro = erro?.takeIf { it == ErroModelo.OPCOES_INSUFICIENTES || it == ErroModelo.OPCOES_REPETIDAS }?.let { textoErroModelo(it) }, habilitado = habilitado, singleLine = false)
        LinhaSwitch(stringResource(Res.string.anamnese_pergunta_obrigatoria), pergunta.obrigatoria, habilitado) { novo -> acoes.aoMudarPergunta(id) { it.copy(obrigatoria = novo) } }
        LinhaSwitch(stringResource(Res.string.anamnese_condicao), pergunta.condicaoPergunta != null, habilitado && anteriores.isNotEmpty()) { ligado ->
            if (ligado) acoes.aoLigarCondicao(id, anteriores.first().id) else acoes.aoDesligarCondicao(id)
        }
        if (referencia != null) {
            Text(stringResource(Res.string.anamnese_condicao_pergunta), style = MaterialTheme.typography.labelLarge)
            anteriores.forEach { candidata ->
                Row(Modifier.fillMaxWidth().heightIn(min = Dimens.alvoToque).selectable(candidata.id == referencia.id, enabled = habilitado, role = Role.RadioButton) { acoes.aoLigarCondicao(id, candidata.id) },
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                    RadioButton(candidata.id == referencia.id, onClick = null, enabled = habilitado)
                    Text(candidata.texto, style = MaterialTheme.typography.bodyMedium)
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                operadoresPara(referencia).forEach { op ->
                    FilterChip(pergunta.condicaoOperador == op, { acoes.aoMudarPergunta(id) { it.copy(condicaoOperador = op) } }, label = { Text(rotuloOperador(op)) }, enabled = habilitado)
                }
            }
            val opcoesReferencia = opcoesDe(referencia)
            if (opcoesReferencia.isEmpty()) CampoTexto(stringResource(Res.string.anamnese_condicao_valor), pergunta.condicaoValor, { novo -> acoes.aoMudarPergunta(id) { it.copy(condicaoValor = novo.take(200)) } },
                habilitado = habilitado)
            else FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                opcoesReferencia.forEach { opcao -> FilterChip(pergunta.condicaoValor == opcao, { acoes.aoMudarPergunta(id) { it.copy(condicaoValor = opcao) } }, label = { Text(opcao) }, enabled = habilitado) }
            }
            erro?.takeIf { it == ErroModelo.CONDICAO_INCOMPLETA || it == ErroModelo.CONDICAO_INVALIDA }?.let {
                Text(textoErroModelo(it), color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
            BotaoTexto(stringResource(Res.string.anamnese_pergunta_subir), { acoes.aoMover(id, -1) }, habilitado = habilitado)
            BotaoTexto(stringResource(Res.string.anamnese_pergunta_descer), { acoes.aoMover(id, 1) }, habilitado = habilitado)
            BotaoTexto(stringResource(Res.string.anamnese_pergunta_remover), { acoes.aoRemoverPergunta(id) }, habilitado = habilitado)
        }
    }
}

@Composable
private fun LinhaSwitch(rotulo: String, ligado: Boolean, habilitado: Boolean, aoMudar: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = Dimens.alvoToque).toggleable(ligado, enabled = habilitado, role = Role.Switch, onValueChange = aoMudar),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
        Text(rotulo, Modifier.weight(1f))
        Switch(ligado, onCheckedChange = null, enabled = habilitado)
    }
}
