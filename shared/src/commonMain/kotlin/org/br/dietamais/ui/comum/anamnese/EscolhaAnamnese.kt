package org.br.dietamais.ui.comum.anamnese

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.BotaoTexto
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

@Composable
fun EscolhaAnamnese(rotulo: String, opcoes: List<String>, escolhida: String?, habilitado: Boolean, obrigatoria: Boolean, aoMudar: (String?) -> Unit) {
    var aberta by remember(rotulo) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMinimo)) {
        Text(rotulo, style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton({ aberta = true }, enabled = habilitado, modifier = Modifier.fillMaxWidth().heightIn(min = Dimens.alvoToque).semantics { contentDescription = rotulo }) {
                Text(escolhida ?: stringResource(Res.string.ana_escolher))
            }
            DropdownMenu(expanded = aberta && habilitado, onDismissRequest = { aberta = false }) {
                opcoes.forEach { opcao -> DropdownMenuItem(text = { Text(opcao) }, onClick = { aberta = false; aoMudar(opcao) }) }
            }
        }
        if (escolhida != null && !obrigatoria) BotaoTexto(stringResource(Res.string.ana_limpar), { aoMudar(null) }, habilitado = habilitado)
    }
}
