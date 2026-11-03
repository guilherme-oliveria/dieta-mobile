package org.br.dietamais.ui.privacidade

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.auth.CampoSenha
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.nutri.avaliacao.textoData
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

class AcoesPrivacidade(val aoAbrirTermos: () -> Unit, val aoPedirExportacao: () -> Unit, val aoAtualizarExportacao: () -> Unit, val aoAbrirExportacao: () -> Unit,
    val aoPedirExclusao: () -> Unit, val aoMudarSenha: (String) -> Unit, val aoConfirmarExclusao: () -> Unit, val aoCancelarExclusao: () -> Unit, val aoUrlAberta: () -> Unit)

/** Cartão "Privacidade" (MOB-34). A nutri só vê os termos; exportar e excluir conta são do paciente (RN-LGPD-02/03). */
@Composable
fun PrivacidadeCard(estado: EstadoPrivacidade, acoes: AcoesPrivacidade, comDadosPessoais: Boolean, modifier: Modifier = Modifier) {
    val navegador = LocalUriHandler.current
    LaunchedEffect(estado.urlParaAbrir) { estado.urlParaAbrir?.let { navegador.openUri(it); acoes.aoUrlAberta() } }
    val habilitado = !estado.semConexao && !estado.enviando
    CardDieta(modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.privacidade_titulo), style = MaterialTheme.typography.titleMedium)
        estado.erro?.takeIf { !estado.confirmandoExclusao }?.let { Text(stringResource(texto(it)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        estado.aviso?.let { Text(stringResource(texto(it)), style = MaterialTheme.typography.bodySmall) }
        BotaoTexto(stringResource(Res.string.privacidade_termos), acoes.aoAbrirTermos, habilitado = habilitado)
        if (comDadosPessoais) {
            Text(stringResource(Res.string.privacidade_exportar_texto), style = MaterialTheme.typography.bodySmall)
            val e = estado.exportacao
            when (e?.status) {
                "PENDENTE" -> Text(stringResource(Res.string.privacidade_exportacao_pendente), style = MaterialTheme.typography.bodyMedium)
                "PRONTA" -> BotaoPrincipal(stringResource(Res.string.privacidade_exportacao_baixar, e.expiraEm?.let { textoData(it) } ?: "—"), acoes.aoAbrirExportacao, habilitado = habilitado)
                "FALHOU" -> Text(stringResource(Res.string.privacidade_exportacao_falhou), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            if (e?.status != "PENDENTE") BotaoContornado(stringResource(Res.string.privacidade_exportar), acoes.aoPedirExportacao, habilitado = habilitado)
            else BotaoTexto(stringResource(Res.string.sync_atualizar), acoes.aoAtualizarExportacao, habilitado = habilitado)
            Text(stringResource(Res.string.privacidade_excluir_texto), style = MaterialTheme.typography.bodySmall)
            BotaoTexto(stringResource(Res.string.privacidade_excluir), acoes.aoPedirExclusao, habilitado = habilitado)
        }
    }
    if (estado.confirmandoExclusao) AlertDialog(
        onDismissRequest = acoes.aoCancelarExclusao,
        title = { Text(stringResource(Res.string.privacidade_excluir_titulo)) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
            Text(stringResource(Res.string.privacidade_excluir_aviso))
            CampoSenha(stringResource(Res.string.privacidade_senha), estado.senha, acoes.aoMudarSenha, estado.erro?.let { stringResource(texto(it)) }, !estado.enviando, acoes.aoConfirmarExclusao)
        } },
        confirmButton = { BotaoTexto(stringResource(Res.string.privacidade_excluir_confirmar), acoes.aoConfirmarExclusao, habilitado = habilitado && estado.senha.isNotEmpty(), carregando = estado.enviando) },
        dismissButton = { BotaoTexto(stringResource(Res.string.privacidade_cancelar), acoes.aoCancelarExclusao) })
}

private fun texto(m: MensagemPrivacidade): StringResource = when (m) {
    MensagemPrivacidade.SEM_CONEXAO -> Res.string.perfil_erro_sem_conexao
    MensagemPrivacidade.EXPORTACAO_EM_ANDAMENTO -> Res.string.privacidade_erro_exportacao
    MensagemPrivacidade.SENHA_INCORRETA -> Res.string.privacidade_erro_senha
    MensagemPrivacidade.SENHA_OBRIGATORIA -> Res.string.privacidade_erro_senha_vazia
    MensagemPrivacidade.VINCULO_ATIVO -> Res.string.privacidade_erro_geral
    MensagemPrivacidade.PEDIDO_ENVIADO -> Res.string.privacidade_exportacao_enviada
    MensagemPrivacidade.ERRO -> Res.string.privacidade_erro_geral
}
