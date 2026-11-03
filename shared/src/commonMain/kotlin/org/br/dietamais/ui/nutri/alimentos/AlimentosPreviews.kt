package org.br.dietamais.ui.nutri.alimentos

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.ui.theme.DietaTheme
import kotlin.time.Instant
import kotlin.uuid.Uuid

// Mesmos dados fictícios do exemplo aprovado BE-C2; usados somente pelo preview.
private val grupoPreview = GrupoAlimentoResposta(Uuid.parse("019a0f32-5000-7000-8000-000000000001"), "Grupo fictício", 150.0, 25.0, 5.0, 3.0)
private val alimentoPreview = AlimentoResposta(Uuid.parse("019a0f32-5000-7000-8000-000000000002"), grupoId = grupoPreview.id,
    nome = "Alimento fictício", medidaCaseira = "1 unidade fictícia", gramasPorPorcao = 50.0, ativo = true, versao = 1,
    atualizadoEm = Instant.parse("2026-10-26T12:00:00Z"))

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun ListaAlimentosPreview() = DietaTheme { AlimentosTela(EstadoAlimentos(grupos = listOf(grupoPreview), lista = listOf(alimentoPreview))) }

@Preview(widthDp = 820, heightDp = 1180)
@Composable
private fun FormularioAlimentoPreview() = DietaTheme { FormularioAlimentoTela(EstadoAlimentos(grupos = listOf(grupoPreview),
    formulario = FormularioAlimento(grupoId = grupoPreview.id, nome = alimentoPreview.nome, medida = alimentoPreview.medidaCaseira, gramas = "50"))) }
