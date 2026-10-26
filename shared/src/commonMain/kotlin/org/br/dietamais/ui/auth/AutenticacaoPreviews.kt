package org.br.dietamais.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.br.dietamais.data.remoto.dto.Perfil
import org.br.dietamais.ui.theme.DietaTheme
import org.br.dietamais.ui.theme.TamanhosPreview

@Preview(widthDp = TamanhosPreview.compacto, heightDp = TamanhosPreview.alturaCompacta)
@Composable private fun EntrarCompactoPreview() = DietaTheme { EntrarTela(EstadoAutenticacao(restaurando = false), {}, {}, {}, {}, {}) }
@Preview(widthDp = TamanhosPreview.expandido, heightDp = TamanhosPreview.alturaExpandida)
@Composable private fun EntrarExpandidoPreview() = EntrarCompactoPreview()
@Preview(widthDp = TamanhosPreview.compacto, heightDp = TamanhosPreview.alturaCompacta)
@Composable private fun CadastroCompactoPreview() = DietaTheme { CadastroNutriTela(EstadoAutenticacao(restaurando = false), null, {}, {}, {}, {}, {}, {}, {}) }
@Preview(widthDp = TamanhosPreview.expandido, heightDp = TamanhosPreview.alturaExpandida)
@Composable private fun CadastroExpandidoPreview() = CadastroCompactoPreview()
@Preview(widthDp = TamanhosPreview.compacto, heightDp = TamanhosPreview.alturaCompacta)
@Composable private fun RecuperarCompactoPreview() = DietaTheme { RecuperarSenhaTela(EstadoAutenticacao(restaurando = false), {}, {}, {}, {}, {}, {}) }
@Preview(widthDp = TamanhosPreview.expandido, heightDp = TamanhosPreview.alturaExpandida)
@Composable private fun RecuperarExpandidoPreview() = RecuperarCompactoPreview()
@Preview(widthDp = TamanhosPreview.compacto, heightDp = TamanhosPreview.alturaCompacta)
@Composable private fun EscolherCompactoPreview() = DietaTheme { EscolherPerfilTela(EstadoAutenticacao(restaurando = false, perfis = listOf(Perfil.NUTRICIONISTA, Perfil.PACIENTE)), {}, {}) }
@Preview(widthDp = TamanhosPreview.expandido, heightDp = TamanhosPreview.alturaExpandida)
@Composable private fun EscolherExpandidoPreview() = EscolherCompactoPreview()
