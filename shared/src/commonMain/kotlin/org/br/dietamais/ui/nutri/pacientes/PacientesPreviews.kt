package org.br.dietamais.ui.nutri.pacientes

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.br.dietamais.data.remoto.dto.PacienteResposta
import org.br.dietamais.data.remoto.dto.StatusVinculo
import org.br.dietamais.ui.theme.DietaTheme
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Preview
@Composable
private fun PacientesPreview() = DietaTheme {
    val paciente = PacienteResposta(
        id = Uuid.parse("01900000-0000-7000-8000-000000000001"),
        vinculoId = Uuid.parse("01900000-0000-7000-8000-000000000002"),
        nome = "Pessoa fictícia", status = StatusVinculo.CONVIDADO, versao = 0,
        criadoEm = Instant.parse("2026-01-01T00:00:00Z"), atualizadoEm = Instant.parse("2026-01-01T00:00:00Z"))
    PacientesTela(EstadoPacientes(lista = listOf(paciente)), {}, {}, {}, {}, {})
}
