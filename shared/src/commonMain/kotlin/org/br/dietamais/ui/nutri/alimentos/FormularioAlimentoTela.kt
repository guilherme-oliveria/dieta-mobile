package org.br.dietamais.ui.nutri.alimentos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import org.br.dietamais.data.remoto.dto.AlimentoResposta
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource
import kotlin.uuid.Uuid

@Composable
fun FormularioAlimentoTela(estado: EstadoAlimentos, aoCampo: (CampoAlimento, String) -> Unit = { _, _ -> },
    aoGrupo: (Uuid) -> Unit = {}, aoSalvar: () -> Unit = {}, aoFechar: () -> Unit = {}, aoAtualizar: () -> Unit = {}) {
    val form = estado.formulario ?: return
    val habilitado = !estado.enviando && !estado.atualizando && !estado.conflito
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = Dimens.larguraLeitura).fillMaxWidth().verticalScroll(rememberScrollState()).padding(Dimens.margemCompacta),
                verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
                Text(stringResource(if (form.versao == null) Res.string.alimentos_novo else Res.string.alimentos_editar),
                    style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
                MensagensAlimentos(estado)
                CampoTexto(stringResource(Res.string.alimentos_nome), form.nome, { aoCampo(CampoAlimento.NOME, it) },
                    erro = estado.errosCampos["nome"]?.let { textoMensagemAlimentos(it) }, habilitado = habilitado)
                GruposAlimentos(estado.grupos, form.grupoId, { it?.let(aoGrupo) }, habilitado = habilitado)
                estado.errosCampos["grupoId"]?.let { Text(textoMensagemAlimentos(it), color = MaterialTheme.colorScheme.onErrorContainer) }
                CampoTexto(stringResource(Res.string.alimentos_medida), form.medida, { aoCampo(CampoAlimento.MEDIDA, it) },
                    erro = estado.errosCampos["medidaCaseira"]?.let { textoMensagemAlimentos(it) }, habilitado = habilitado)
                CampoNumero(stringResource(Res.string.alimentos_gramas), form.gramas, { aoCampo(CampoAlimento.GRAMAS, it) },
                    erro = estado.errosCampos["gramasPorPorcao"]?.let { textoMensagemAlimentos(it) }, habilitado = habilitado)
                BotaoPrincipal(stringResource(Res.string.alimentos_salvar), aoSalvar, Modifier.fillMaxWidth(),
                    habilitado = habilitado && estado.grupos.isNotEmpty(), carregando = estado.enviando)
                if (estado.conflito) BotaoContornado(stringResource(Res.string.alimentos_descartar_atualizar), aoAtualizar, Modifier.fillMaxWidth(), habilitado = !estado.enviando)
                else BotaoTexto(stringResource(Res.string.sync_atualizar), aoAtualizar, Modifier.fillMaxWidth(), habilitado = !estado.enviando && !estado.atualizando)
                BotaoTexto(stringResource(Res.string.pacientes_cancelar), aoFechar, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun ConfirmarDesativacao(alimento: AlimentoResposta, enviando: Boolean, aoConfirmar: () -> Unit, aoFechar: () -> Unit,
    habilitado: Boolean = true, erro: MensagemAlimentos? = null) {
    AlertDialog(onDismissRequest = { if (!enviando) aoFechar() },
        title = { Text(stringResource(Res.string.alimentos_desativar_titulo)) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
            Text(alimento.nome); Text(stringResource(Res.string.alimentos_desativar_descricao))
            erro?.let { Text(textoMensagemAlimentos(it), color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
        } },
        confirmButton = { BotaoTexto(stringResource(Res.string.alimentos_desativar_titulo), aoConfirmar, carregando = enviando, habilitado = habilitado) },
        dismissButton = { BotaoTexto(stringResource(Res.string.pacientes_cancelar), aoFechar, habilitado = !enviando) })
}
