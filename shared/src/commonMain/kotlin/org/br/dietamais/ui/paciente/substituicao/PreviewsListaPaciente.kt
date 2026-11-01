package org.br.dietamais.ui.paciente.substituicao

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.br.dietamais.data.remoto.dto.AlimentoResposta
import org.br.dietamais.dominio.rotina.GrupoListaPaciente
import org.br.dietamais.ui.paciente.dieta.EstadoDietaPaciente
import org.br.dietamais.ui.theme.*
import kotlin.time.Instant
import kotlin.uuid.Uuid

/** Dados inteiramente fictícios, sem validade clínica. */
@Preview(name = "Lista compacta", widthDp = TamanhosPreview.compacto, heightDp = TamanhosPreview.alturaCompacta)
@Preview(name = "Lista média", widthDp = TamanhosPreview.medio, heightDp = TamanhosPreview.altura)
@Preview(name = "Lista expandida", widthDp = TamanhosPreview.expandido, heightDp = TamanhosPreview.alturaExpandida)
@Composable
private fun ListaPreview() {
    val grupo = Uuid.parse("019a0000-0000-7000-8000-000000000001")
    val alimento = AlimentoResposta(Uuid.parse("019a0000-0000-7000-8000-000000000002"), null, grupo, "Alimento fictício", "1 unidade fictícia", 50.0, true, 0, Instant.DISTANT_PAST)
    DietaTheme {
        ListaSubstituicaoTela(EstadoDietaPaciente(carregado = true, gruposLista = listOf(GrupoListaPaciente(grupo, "Grupo fictício", 70.0, listOf(alimento))), grupoListaAberto = grupo), AcoesListaPaciente())
    }
}
