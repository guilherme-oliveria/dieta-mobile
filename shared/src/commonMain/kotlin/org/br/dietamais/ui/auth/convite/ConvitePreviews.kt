package org.br.dietamais.ui.auth.convite

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.ui.theme.DietaTheme
import org.br.dietamais.ui.theme.TamanhosPreview
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Preview(widthDp = TamanhosPreview.compacto, heightDp = TamanhosPreview.alturaCompacta)
@Composable private fun CodigoConviteCompactoPreview() = DietaTheme {
    ConviteTela(EstadoConvite(visivel = true), null, {}, {}, {}, {}, {}, { _, _ -> }, {}, {})
}
@Preview(widthDp = TamanhosPreview.expandido, heightDp = TamanhosPreview.alturaExpandida)
@Composable private fun CodigoConviteExpandidoPreview() = CodigoConviteCompactoPreview()

@Preview(widthDp = TamanhosPreview.compacto, heightDp = TamanhosPreview.alturaCompacta)
@Composable private fun ConsentimentoCompactoPreview() = DietaTheme {
    val convite = ConviteResposta(NutricionistaResumoResposta(Uuid.parse("01900000-0000-7000-8000-000000000001"), "Nutri fictícia", "CRN-1 0000"),
        TermoConsentimentoResposta("TESTE", "Consentimento fictício apenas para preview."), Instant.parse("2030-01-01T00:00:00Z"))
    ConviteTela(EstadoConvite(visivel = true, codigo = "ABCD2345", convite = convite), null, {}, {}, {}, {}, {}, { _, _ -> }, {}, {})
}
@Preview(widthDp = TamanhosPreview.expandido, heightDp = TamanhosPreview.alturaExpandida)
@Composable private fun ConsentimentoExpandidoPreview() = ConsentimentoCompactoPreview()
