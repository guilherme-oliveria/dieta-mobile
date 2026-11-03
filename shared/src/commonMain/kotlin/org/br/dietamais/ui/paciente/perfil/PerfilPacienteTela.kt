package org.br.dietamais.ui.paciente.perfil

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import org.br.dietamais.data.remoto.dto.StatusVinculo
import org.br.dietamais.data.remoto.dto.TipoNotificacao
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

class AcoesPerfil(val aoMudarLembretes: (Boolean) -> Unit, val aoMudarTipo: (TipoNotificacao, Boolean) -> Unit, val aoMudarSilencioAtivo: (Boolean) -> Unit,
    val aoMudarInicio: (String) -> Unit, val aoMudarFim: (String) -> Unit, val aoSalvar: () -> Unit, val aoGerarLink: () -> Unit, val aoRevogarLink: () -> Unit)

/** Aba Perfil do paciente: lembretes, avisos, silêncio, minhas nutris e agenda do celular (MOB-19 e MOB-21). */
@Composable
fun PerfilPacienteTela(estado: EstadoPerfilPaciente, acoes: AcoesPerfil, modifier: Modifier = Modifier, extras: @Composable ColumnScope.() -> Unit = {}) {
    val navegador = LocalUriHandler.current
    val habilitado = !estado.semConexao && !estado.enviando
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Dimens.paddingCard), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
        Text(stringResource(Res.string.perfil_titulo), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        if (estado.semConexao) FaixaOffline()
        estado.erro?.let { Text(stringResource(textoMensagem(it)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        estado.aviso?.let { Text(stringResource(textoMensagem(it)), style = MaterialTheme.typography.bodySmall) }

        Interruptor(stringResource(Res.string.perfil_lembretes), estado.lembretes, acoes.aoMudarLembretes)

        CardDieta(Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.perfil_avisos), style = MaterialTheme.typography.titleMedium)
            estado.tipos.filter { it.tipo != TipoNotificacao.OUTRO }.forEach { t ->
                Interruptor(stringResource(rotuloTipo(t.tipo)), t.ativo, { acoes.aoMudarTipo(t.tipo, it) }, habilitado)
            }
            Text(stringResource(Res.string.perfil_silencio), style = MaterialTheme.typography.titleSmall)
            Interruptor(stringResource(Res.string.perfil_silencio), estado.silencioAtivo, acoes.aoMudarSilencioAtivo, habilitado)
            if (estado.silencioAtivo) {
                CampoHora(stringResource(Res.string.perfil_silencio_inicio), estado.inicio, acoes.aoMudarInicio, habilitado = habilitado)
                CampoHora(stringResource(Res.string.perfil_silencio_fim), estado.fim, acoes.aoMudarFim, habilitado = habilitado)
            }
            BotaoPrincipal(stringResource(Res.string.perfil_salvar), acoes.aoSalvar, habilitado = habilitado && estado.alterado, carregando = estado.enviando)
        }

        if (estado.nutris.isNotEmpty()) CardDieta(Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.perfil_nutris), style = MaterialTheme.typography.titleMedium)
            estado.nutris.forEach { v ->
                Text(stringResource(Res.string.perfil_nutri_item, v.nutricionista.nome, v.nutricionista.crn), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(when (v.status) {
                    StatusVinculo.ATIVO -> Res.string.perfil_nutri_ativo
                    StatusVinculo.ENCERRADO -> Res.string.perfil_nutri_encerrado
                    else -> Res.string.perfil_nutri_convidado
                }), style = MaterialTheme.typography.labelLarge)
            }
        }

        CardDieta(Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.perfil_agenda), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(Res.string.perfil_agenda_texto), style = MaterialTheme.typography.bodySmall)
            val link = estado.calendario
            if (link == null) BotaoPrincipal(stringResource(Res.string.perfil_agenda_gerar), acoes.aoGerarLink, habilitado = habilitado)
            else {
                BotaoPrincipal(stringResource(Res.string.perfil_agenda_google), {
                    navegador.openUri("https://calendar.google.com/calendar/r?cid=" + link.urlWebcal)
                })
                BotaoTexto(stringResource(Res.string.perfil_agenda_apple), { navegador.openUri(link.urlWebcal) })
                BotaoTexto(stringResource(Res.string.perfil_agenda_revogar), acoes.aoRevogarLink, habilitado = habilitado)
            }
        }
        extras()
    }
}

@Composable
private fun Interruptor(rotulo: String, marcado: Boolean, aoMudar: (Boolean) -> Unit, habilitado: Boolean = true) {
    Row(Modifier.fillMaxWidth().heightIn(min = Dimens.alvoToque).toggleable(marcado, enabled = habilitado, role = Role.Switch, onValueChange = aoMudar),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(rotulo, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = marcado, onCheckedChange = null, enabled = habilitado)
    }
}

private fun rotuloTipo(t: TipoNotificacao): StringResource = when (t) {
    TipoNotificacao.NOVA_DIETA -> Res.string.perfil_tipo_nova_dieta
    TipoNotificacao.QUESTIONARIO_ENVIADO -> Res.string.perfil_tipo_questionario_enviado
    else -> Res.string.perfil_tipo_consulta_amanha
}

private fun textoMensagem(m: MensagemPerfil): StringResource = when (m) {
    MensagemPerfil.SEM_CONEXAO -> Res.string.perfil_erro_sem_conexao
    MensagemPerfil.CONFLITO -> Res.string.perfil_erro_conflito
    MensagemPerfil.HORARIO_INVALIDO -> Res.string.perfil_erro_horario
    MensagemPerfil.SILENCIO_IGUAL -> Res.string.perfil_erro_silencio_igual
    MensagemPerfil.ERRO -> Res.string.perfil_erro_geral
    MensagemPerfil.SALVO -> Res.string.perfil_aviso_salvo
    MensagemPerfil.LINK_REVOGADO -> Res.string.perfil_aviso_revogado
}
