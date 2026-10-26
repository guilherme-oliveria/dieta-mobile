package org.br.dietamais.ui.auth.convite

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import org.br.dietamais.data.remoto.dto.TermosUsoResposta
import org.br.dietamais.dominio.contas.permiteCadastro
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.BotaoTexto
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TermosUsoParte(termos: TermosUsoResposta?, aceitou: Boolean, carregando: Boolean,
    aoAceitar: (Boolean) -> Unit, aoAtualizar: () -> Unit) {
    var lendo by remember(termos?.versao, termos?.status) { mutableStateOf(false) }
    val habilitado = termos?.permiteCadastro() == true
    if (termos != null) {
        Text(stringResource(Res.string.convite_termos_estado, termos.status, termos.versao), style = MaterialTheme.typography.bodySmall)
        BotaoTexto(stringResource(Res.string.auth_ler_termos), { lendo = true }, Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth().toggleable(aceitou && habilitado, enabled = habilitado && !carregando,
            role = Role.Checkbox, onValueChange = aoAceitar).heightIn(min = Dimens.alvoToque)
            .padding(vertical = Dimens.espacoPequeno), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
            Checkbox(aceitou && habilitado, onCheckedChange = null)
            Text(stringResource(Res.string.auth_aceitar_termos), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        }
    }
    if (!habilitado) Text(stringResource(Res.string.convite_cadastro_bloqueado), color = MaterialTheme.colorScheme.onErrorContainer,
        style = MaterialTheme.typography.bodyMedium)
    BotaoTexto(stringResource(Res.string.convite_atualizar_termos), aoAtualizar, Modifier.fillMaxWidth(), carregando = carregando)
    if (lendo && termos != null) AlertDialog(onDismissRequest = { lendo = false },
        title = { Text(stringResource(Res.string.auth_termos_titulo)) },
        text = { Column(Modifier.heightIn(max = Dimens.alturaTermos).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
            Text(stringResource(Res.string.convite_termos_estado, termos.status, termos.versao))
            if (!habilitado) Text(stringResource(Res.string.convite_cadastro_bloqueado))
            Text(termos.conteudo)
        } }, confirmButton = { BotaoTexto(stringResource(Res.string.auth_fechar), { lendo = false }) })
}
