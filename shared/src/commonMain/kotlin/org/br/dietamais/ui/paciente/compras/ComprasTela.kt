package org.br.dietamais.ui.paciente.compras

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import org.br.dietamais.dominio.rotina.textoPeso
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource
import kotlin.uuid.Uuid

class AcoesCompras(val aoMudarDias: (Int) -> Unit, val aoMarcar: (Uuid, Boolean) -> Unit, val aoLimpar: () -> Unit, val aoCompartilhar: () -> Unit)

@Composable
fun ComprasTela(estado: EstadoCompras, acoes: AcoesCompras, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Dimens.paddingCard), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
        Text(stringResource(Res.string.aba_compras), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        when {
            !estado.carregado -> EstadoCarregando()
            !estado.temDieta -> EstadoVazio(mensagem = stringResource(Res.string.compras_sem_dieta))
            else -> {
                Text(stringResource(Res.string.compras_periodo), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                    listOf(3, 7, 14).forEach { d -> BotaoTexto(stringResource(Res.string.compras_dias, d.toString()), { acoes.aoMudarDias(d) }, selecionado = d == estado.dias) }
                }
                estado.grupos.forEach { g ->
                    CardDieta(Modifier.fillMaxWidth()) {
                        Text(g.nome ?: stringResource(Res.string.compras_itens), style = MaterialTheme.typography.titleMedium)
                        g.itens.forEach { i ->
                            val marcado = i.alimentoId in estado.marcados
                            // A linha toda é o controle (nome = texto do item); o Checkbox é só o desenho.
                            Row(Modifier.fillMaxWidth().heightIn(min = Dimens.alvoToque).toggleable(marcado, role = Role.Checkbox) { acoes.aoMarcar(i.alimentoId, it) },
                                verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = marcado, onCheckedChange = null)
                                Text("${i.nome} · ${textoPeso(i.gramas)}", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
                estado.aviso?.let { Text(stringResource(when (it) {
                    MensagemCompras.COPIADA -> Res.string.compras_copiada
                    MensagemCompras.ABERTA -> Res.string.compras_aberta
                    MensagemCompras.FALHOU -> Res.string.compras_falhou
                }), style = MaterialTheme.typography.bodySmall) }
                BotaoPrincipal(stringResource(Res.string.compras_compartilhar), acoes.aoCompartilhar)
                BotaoTexto(stringResource(Res.string.compras_limpar), acoes.aoLimpar, habilitado = estado.marcados.isNotEmpty())
            }
        }
    }
}
