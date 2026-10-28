package org.br.dietamais.ui.nutri.alimentos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.theme.*
import org.jetbrains.compose.resources.stringResource
import kotlin.uuid.Uuid

@Composable
fun AlimentosTela(estado: EstadoAlimentos, aoBusca: (String) -> Unit = {}, aoGrupo: (Uuid?) -> Unit = {},
    aoAtivo: (Boolean) -> Unit = {}, aoNovo: () -> Unit = {}, aoEditar: (AlimentoResposta) -> Unit = {},
    aoDesativar: (AlimentoResposta) -> Unit = {}, aoAtualizar: () -> Unit = {}) {
    val margem = Dimens.margemTela(currentWindowAdaptiveInfoV2().windowSizeClass)
    val habilitado = !estado.enviando && !estado.atualizando
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(Modifier.fillMaxSize().testTag("lista-alimentos"), contentPadding = PaddingValues(margem),
            verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
                    Text(stringResource(Res.string.aba_alimentos), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                        BotaoPrincipal(stringResource(Res.string.alimentos_novo), aoNovo, habilitado = habilitado && estado.grupos.isNotEmpty())
                        BotaoTexto(stringResource(Res.string.sync_atualizar), aoAtualizar, carregando = estado.atualizando, habilitado = !estado.enviando)
                    }
                    if (estado.semConexao) FaixaOffline()
                    MensagensAlimentos(estado)
                    CampoTexto(stringResource(Res.string.alimentos_busca), estado.busca, aoBusca)
                    GruposAlimentos(estado.grupos, estado.grupoId, aoGrupo, todos = true)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                        FilterChip(estado.ativo, { aoAtivo(true) }, label = { Text(stringResource(Res.string.alimentos_ativos)) }, modifier = Modifier.heightIn(min = Dimens.alvoToque))
                        FilterChip(!estado.ativo, { aoAtivo(false) }, label = { Text(stringResource(Res.string.alimentos_inativos)) }, modifier = Modifier.heightIn(min = Dimens.alvoToque))
                    }
                }
            }
            if (estado.lista.isEmpty()) item {
                when {
                    estado.atualizando -> EstadoCarregando()
                    estado.erro != null -> EstadoErro(aoAtualizar, mensagem = textoMensagemAlimentos(estado.erro))
                    else -> EstadoVazio(mensagem = stringResource(Res.string.alimentos_vazio))
                }
            }
            items(estado.lista, key = { it.id.toString() }) { alimento ->
                CardDieta(Modifier.fillMaxWidth()) {
                    Text(alimento.nome, style = MaterialTheme.typography.titleMedium)
                    Text(estado.grupos.firstOrNull { it.id == alimento.grupoId }?.nome ?: stringResource(Res.string.alimentos_grupo), style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(Res.string.alimentos_quantidade, alimento.medidaCaseira, alimento.gramasPorPorcao.toString().removeSuffix(".0").replace('.', ',')), style = MaterialTheme.typography.bodyMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                        BotaoTexto(stringResource(Res.string.alimentos_acao_editar), { aoEditar(alimento) }, habilitado = habilitado)
                        if (alimento.ativo) BotaoTexto(stringResource(Res.string.alimentos_acao_desativar), { aoDesativar(alimento) }, habilitado = habilitado)
                    }
                }
            }
        }
    }
}

@Composable
internal fun GruposAlimentos(grupos: List<GrupoAlimentoResposta>, selecionado: Uuid?, aoGrupo: (Uuid?) -> Unit,
    todos: Boolean = false, habilitado: Boolean = true) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.espacoMinimo)) {
        Text(stringResource(Res.string.alimentos_grupo), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
            if (todos) FilterChip(selecionado == null, { aoGrupo(null) }, enabled = habilitado,
                label = { Text(stringResource(Res.string.alimentos_todos_grupos)) }, modifier = Modifier.heightIn(min = Dimens.alvoToque))
            grupos.forEach { grupo -> FilterChip(selecionado == grupo.id, { aoGrupo(grupo.id) }, enabled = habilitado,
                label = { Text(grupo.nome) }, modifier = Modifier.heightIn(min = Dimens.alvoToque)) }
        }
        if (grupos.isEmpty()) Text(stringResource(Res.string.alimentos_grupos_vazios), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
internal fun MensagensAlimentos(estado: EstadoAlimentos) {
    estado.erro?.let { Text(textoMensagemAlimentos(it), style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
    estado.aviso?.let { Text(textoMensagemAlimentos(it), style = MaterialTheme.typography.bodySmall, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
}

@Composable
fun textoMensagemAlimentos(mensagem: MensagemAlimentos): String = stringResource(when (mensagem) {
    MensagemAlimentos.OBRIGATORIO -> Res.string.auth_erro_obrigatorio
    MensagemAlimentos.POSITIVO -> Res.string.alimentos_positivo
    MensagemAlimentos.CAMPO_INVALIDO -> Res.string.alimentos_campo_invalido
    MensagemAlimentos.SEM_CONEXAO -> Res.string.pacientes_erro_rede
    MensagemAlimentos.INDISPONIVEL -> Res.string.alimentos_indisponivel
    MensagemAlimentos.CONFLITO -> Res.string.alimentos_conflito
    MensagemAlimentos.ASSINATURA_INATIVA -> Res.string.pacientes_erro_assinatura
    MensagemAlimentos.TAXA_EXCEDIDA -> Res.string.auth_erro_taxa
    MensagemAlimentos.SALVO -> Res.string.alimentos_salvo
    MensagemAlimentos.ERRO -> Res.string.erro_padrao
})
