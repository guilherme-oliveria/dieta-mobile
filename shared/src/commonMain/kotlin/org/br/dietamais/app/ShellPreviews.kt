package org.br.dietamais.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.br.dietamais.ui.theme.DietaTheme
import org.br.dietamais.ui.theme.TamanhosPreview

@Preview(name = "Paciente compacto", widthDp = TamanhosPreview.compacto, heightDp = TamanhosPreview.alturaCompacta)
@Composable
private fun ShellPacienteCompactoPreview() = DietaTheme { NavegacaoApp(PerfilNavegacao.PACIENTE) }

@Preview(name = "Nutri médio", widthDp = TamanhosPreview.medio, heightDp = TamanhosPreview.altura)
@Preview(name = "Nutri expandido", widthDp = TamanhosPreview.expandido, heightDp = TamanhosPreview.alturaExpandida)
@Composable
private fun ShellNutriPreview() = DietaTheme { NavegacaoApp(PerfilNavegacao.NUTRICIONISTA) }
