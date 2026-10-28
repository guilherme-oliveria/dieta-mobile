package org.br.dietamais.ui.paciente.anamnese

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import org.br.dietamais.data.remoto.dto.PerguntaQuestionario
import org.br.dietamais.data.remoto.dto.TipoPergunta
import org.br.dietamais.dominio.anamnese.*
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.nutri.anamnese.textoErroResposta
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

private const val MAX_ITENS = 12

/** Um campo por tipo de pergunta. Recebe a digitação atual; sem ela, o chamador passa o valor já gravado convertido por [entradaInicial]. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CampoPergunta(pergunta: PerguntaQuestionario, entrada: EntradaResposta, erro: ErroResposta?, habilitado: Boolean, aoMudar: (EntradaResposta) -> Unit) {
    val marca = if (pergunta.obrigatoria) " " + stringResource(Res.string.anamnese_obrigatoria_marca) else ""
    val rotulo = pergunta.texto + marca
    val mensagemErro = erro?.let { textoErroResposta(it) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
        when (pergunta.tipo) {
            TipoPergunta.TEXTO -> CampoTexto(rotulo, (entrada as? EntradaResposta.Texto)?.texto.orEmpty(), { aoMudar(EntradaResposta.Texto(it.take(2000))) },
                erro = mensagemErro, habilitado = habilitado, singleLine = false)
            TipoPergunta.NUMERO -> CampoNumero(rotulo, (entrada as? EntradaResposta.Texto)?.texto.orEmpty(), { aoMudar(EntradaResposta.Texto(it.take(20))) }, erro = mensagemErro, habilitado = habilitado)
            TipoPergunta.HORARIO -> CampoHora(rotulo, (entrada as? EntradaResposta.Texto)?.texto.orEmpty(), { aoMudar(EntradaResposta.Texto(it.take(5))) }, erro = mensagemErro, habilitado = habilitado)
            TipoPergunta.ESCOLHA_UNICA -> {
                val escolhida = ((entrada as? EntradaResposta.Valor)?.valor as? JsonPrimitive)?.contentOrNull
                Text(rotulo, style = MaterialTheme.typography.labelLarge)
                pergunta.opcoes.orEmpty().forEach { opcao ->
                    Row(Modifier.fillMaxWidth().heightIn(min = Dimens.alvoToque).selectable(opcao == escolhida, enabled = habilitado, role = Role.RadioButton) { aoMudar(EntradaResposta.Valor(JsonPrimitive(opcao))) },
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                        RadioButton(opcao == escolhida, onClick = null, enabled = habilitado)
                        Text(opcao, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            TipoPergunta.MULTIPLA_ESCOLHA -> {
                val marcadas = ((entrada as? EntradaResposta.Valor)?.valor as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }.orEmpty()
                Text(rotulo, style = MaterialTheme.typography.labelLarge)
                pergunta.opcoes.orEmpty().forEach { opcao ->
                    val marcada = opcao in marcadas
                    Row(Modifier.fillMaxWidth().heightIn(min = Dimens.alvoToque).toggleable(marcada, enabled = habilitado, role = Role.Checkbox) { ligado ->
                        // mantém a ordem das opções do modelo, não a ordem dos toques
                        val novas = pergunta.opcoes.orEmpty().filter { if (it == opcao) ligado else it in marcadas }
                        aoMudar(EntradaResposta.Valor(if (novas.isEmpty()) null else jsonLista(novas)))
                    }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                        Checkbox(marcada, onCheckedChange = null, enabled = habilitado)
                        Text(opcao, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            TipoPergunta.ESCALA_1_5 -> {
                val nota = ((entrada as? EntradaResposta.Valor)?.valor as? JsonPrimitive)?.takeIf { !it.isString }?.intOrNull
                Text(rotulo, style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                    (1..5).forEach { n -> FilterChip(nota == n, { aoMudar(EntradaResposta.Valor(JsonPrimitive(n))) }, label = { Text(n.toString()) }, enabled = habilitado) }
                }
            }
            TipoPergunta.LISTA_POR_REFEICAO -> {
                val itens = (entrada as? EntradaResposta.Itens)?.itens.orEmpty()
                Text(rotulo, style = MaterialTheme.typography.labelLarge)
                itens.forEachIndexed { i, item ->
                    CardDieta(Modifier.fillMaxWidth()) {
                        CampoTexto(stringResource(Res.string.anamnese_refeicao), item.refeicao, { novo -> aoMudar(EntradaResposta.Itens(itens.toMutableList().also { it[i] = item.copy(refeicao = novo.take(60)) })) }, habilitado = habilitado)
                        CampoTexto(stringResource(Res.string.anamnese_descricao), item.descricao, { novo -> aoMudar(EntradaResposta.Itens(itens.toMutableList().also { it[i] = item.copy(descricao = novo.take(500)) })) }, habilitado = habilitado, singleLine = false)
                        CampoHora(stringResource(Res.string.anamnese_horario), item.horario.orEmpty(), { novo -> aoMudar(EntradaResposta.Itens(itens.toMutableList().also { it[i] = item.copy(horario = novo.take(5)) })) }, habilitado = habilitado)
                        BotaoTexto(stringResource(Res.string.anamnese_item_remover), { aoMudar(EntradaResposta.Itens(itens.filterIndexed { j, _ -> j != i })) }, habilitado = habilitado)
                    }
                }
                if (itens.size < MAX_ITENS) BotaoContornado(stringResource(Res.string.anamnese_item_adicionar), { aoMudar(EntradaResposta.Itens(itens + ItemRefeicao("", ""))) }, habilitado = habilitado)
            }
            TipoPergunta.OUTRO -> Unit
        }
        if (mensagemErro != null && pergunta.tipo in tiposSemErroNoCampo) Text(mensagemErro, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
    }
}

/** Os campos de texto mostram o erro neles mesmos; os demais tipos mostram embaixo. */
private val tiposSemErroNoCampo = setOf(TipoPergunta.ESCOLHA_UNICA, TipoPergunta.MULTIPLA_ESCOLHA, TipoPergunta.ESCALA_1_5, TipoPergunta.LISTA_POR_REFEICAO)
