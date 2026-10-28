package org.br.dietamais.ui.nutri.assinatura

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

class AcoesPerfilNutri(val aoMudarNome: (String) -> Unit, val aoMudarCrn: (String) -> Unit, val aoMudarTelefone: (String) -> Unit,
    val aoMudarOrientacao: (String) -> Unit, val aoSalvar: () -> Unit, val aoRecarregar: () -> Unit)

@Composable
fun ContaNutriTela(estado: EstadoPerfilNutri, acoes: AcoesPerfilNutri, modifier: Modifier = Modifier, extras: @Composable ColumnScope.() -> Unit = {}) {
    val habilitado = !estado.semConexao && !estado.enviando
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Dimens.paddingCard), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
        Text(stringResource(Res.string.aba_conta), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        if (estado.semConexao) FaixaOffline()
        extras()
        estado.erro?.let { Text(stringResource(texto(it)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        estado.aviso?.let { Text(stringResource(texto(it)), style = MaterialTheme.typography.bodySmall) }
        if (!estado.carregado) {
            if (estado.enviando) EstadoCarregando() else BotaoTexto(stringResource(Res.string.sync_atualizar), acoes.aoRecarregar, habilitado = !estado.semConexao)
        } else {
            Text(stringResource(Res.string.conta_meus_dados), style = MaterialTheme.typography.titleMedium)
            CampoTexto(stringResource(Res.string.conta_nome), estado.nome, acoes.aoMudarNome, habilitado = habilitado, erro = estado.errosCampos["nome"]?.let { stringResource(texto(it)) })
            CampoTexto(stringResource(Res.string.conta_crn), estado.crn, acoes.aoMudarCrn, habilitado = habilitado, erro = estado.errosCampos["crn"]?.let { stringResource(texto(it)) })
            CampoTexto(stringResource(Res.string.conta_telefone), estado.telefone, acoes.aoMudarTelefone, habilitado = habilitado, erro = estado.errosCampos["telefone"]?.let { stringResource(texto(it)) })
            CampoTexto(stringResource(Res.string.conta_orientacao), estado.orientacao, acoes.aoMudarOrientacao, habilitado = habilitado, singleLine = false)
            Text(stringResource(Res.string.conta_orientacao_ajuda), style = MaterialTheme.typography.bodySmall)
            BotaoPrincipal(stringResource(Res.string.conta_salvar), acoes.aoSalvar, habilitado = habilitado && estado.alterado, carregando = estado.enviando)
        }
    }
}

private fun texto(m: MensagemPerfilNutri): StringResource = when (m) {
    MensagemPerfilNutri.NOME_OBRIGATORIO -> Res.string.conta_erro_nome
    MensagemPerfilNutri.CRN_INVALIDO -> Res.string.conta_erro_crn
    MensagemPerfilNutri.TELEFONE_LONGO -> Res.string.conta_erro_telefone
    MensagemPerfilNutri.SEM_CONEXAO -> Res.string.perfil_erro_sem_conexao
    MensagemPerfilNutri.CONFLITO -> Res.string.conta_erro_conflito
    MensagemPerfilNutri.ASSINATURA_INATIVA -> Res.string.conta_erro_assinatura
    MensagemPerfilNutri.ERRO -> Res.string.perfil_erro_geral
    MensagemPerfilNutri.SALVO -> Res.string.conta_salvo
}
