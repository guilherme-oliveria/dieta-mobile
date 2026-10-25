package org.br.dietamais.ui.comum

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import org.br.dietamais.ui.theme.Dimens
import org.br.dietamais.ui.theme.FormasDieta

@Composable
fun CampoTexto(
    rotulo: String,
    valor: String,
    aoMudar: (String) -> Unit,
    modifier: Modifier = Modifier,
    erro: String? = null,
    habilitado: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val cores = MaterialTheme.colorScheme
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Dimens.espacoMinimo)) {
        Text(rotulo, style = MaterialTheme.typography.labelLarge, modifier = Modifier.clearAndSetSemantics {})
        OutlinedTextField(
            value = valor, onValueChange = aoMudar, singleLine = true,
            enabled = habilitado, isError = erro != null, keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation, keyboardActions = keyboardActions, trailingIcon = trailingIcon,
            shape = FormasDieta.campo, textStyle = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth().heightIn(min = Dimens.alturaBotao).semantics {
                contentDescription = rotulo
                if (erro != null) error(erro)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = cores.surface,
                unfocusedContainerColor = cores.surface,
                errorContainerColor = cores.surface,
                unfocusedBorderColor = cores.outline,
                errorBorderColor = cores.error,
                errorCursorColor = cores.onErrorContainer,
                errorTextColor = cores.onSurface,
                errorSupportingTextColor = cores.onErrorContainer,
            ),
            supportingText = erro?.let { mensagem ->
                { Text(mensagem, color = cores.onErrorContainer, style = MaterialTheme.typography.bodySmall) }
            },
        )
    }
}

@Composable
fun CampoHora(
    rotulo: String,
    valor: String,
    aoMudar: (String) -> Unit,
    modifier: Modifier = Modifier,
    erro: String? = null,
    habilitado: Boolean = true,
) = CampoTexto(rotulo, valor, aoMudar, modifier, erro, habilitado, KeyboardOptions(keyboardType = KeyboardType.Ascii))

@Composable
fun CampoNumero(
    rotulo: String,
    valor: String,
    aoMudar: (String) -> Unit,
    modifier: Modifier = Modifier,
    erro: String? = null,
    habilitado: Boolean = true,
) = CampoTexto(rotulo, valor, aoMudar, modifier, erro, habilitado, KeyboardOptions(keyboardType = KeyboardType.Decimal))
