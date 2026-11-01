package org.br.dietamais.ui.paciente.substituicao

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.br.dietamais.dominio.rotina.substitutosDoItem
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.nutri.dieta.formatar
import org.br.dietamais.ui.paciente.dieta.EstadoDietaPaciente
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource
import kotlin.uuid.Uuid

/** Troca de alimento (RN-SUB): substitutos do mesmo grupo na lista congelada, com a quantidade equivalente. Vira check-in TROCADO. */
@Composable
fun TrocaTela(estado: EstadoDietaPaciente, aoEscolher: (Uuid, Uuid?) -> Unit, aoConfirmar: () -> Unit, aoCancelar: () -> Unit) {
    val troca = estado.troca ?: return
    val dieta = estado.dieta ?: return
    val refeicao = dieta.refeicoes.firstOrNull { it.id == troca.refeicaoId } ?: return
    Dialog(onDismissRequest = aoCancelar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth().padding(Dimens.paddingCard), color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.large) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(Dimens.paddingCard), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
                Text(stringResource(Res.string.pac_troca_titulo), style = MaterialTheme.typography.titleLarge)
                Text(refeicao.nome, style = MaterialTheme.typography.titleMedium)
                refeicao.itens.filter { troca.itemId == null || it.id == troca.itemId }.forEach { item ->
                    val opcoes = substitutosDoItem(item, dieta.listaSubstituicao)
                    val escolhido = troca.escolhas[item.id]
                    Text(item.textoLivre ?: item.nomeAlimento.orEmpty(), style = MaterialTheme.typography.bodyLarge)
                    if (item.alimentoId == null && opcoes.isNotEmpty()) Text(stringResource(Res.string.pac_troca_medida_grupo), style = MaterialTheme.typography.bodySmall)
                    if (opcoes.isEmpty()) Text(stringResource(Res.string.pac_troca_sem_opcao), style = MaterialTheme.typography.bodySmall)
                    else {
                        if (escolhido != null) BotaoTexto(stringResource(Res.string.pac_troca_manter), { aoEscolher(item.id, null) })
                        opcoes.forEach { o ->
                            val marcado = o.alimento.id == escolhido
                            BotaoTexto(
                                (if (marcado) "✓ " else "") + if (item.alimentoId == null) stringResource(Res.string.pac_troca_opcao_grupo, o.alimento.nome)
                                else stringResource(Res.string.pac_troca_opcao, o.alimento.nome, o.alimento.medidaCaseira, formatar(o.gramas)),
                                { aoEscolher(item.id, o.alimento.id) })
                        }
                    }
                }
                BotaoPrincipal(stringResource(Res.string.pac_troca_confirmar), aoConfirmar, habilitado = troca.escolhas.isNotEmpty())
                BotaoTexto(stringResource(Res.string.pac_troca_cancelar), aoCancelar)
            }
        }
    }
}
