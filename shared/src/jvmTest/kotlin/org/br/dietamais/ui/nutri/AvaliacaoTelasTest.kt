package org.br.dietamais.ui.nutri

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.dominio.avaliacao.MedidaAvaliacao
import org.br.dietamais.ui.nutri.avaliacao.*
import org.br.dietamais.ui.theme.DietaTheme
import kotlin.test.*
import kotlin.time.Instant
import kotlin.uuid.Uuid

@OptIn(ExperimentalTestApi::class)
class AvaliacaoTelasTest {
    private fun item(data: String, peso: Double) = AvaliacaoResposta(Uuid.random(), Uuid.random(), Uuid.random(), Instant.parse(data), peso, 1.75, 22.86,
        ClassificacaoImc.NORMAL, versao = 1, criadoEm = Instant.parse(data), atualizadoEm = Instant.parse(data))

    @Test fun formularioMostraPreviaDoImcEErroDeCampo() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        val form = FormularioAvaliacao(valores = mapOf(MedidaAvaliacao.PESO to "70", MedidaAvaliacao.ESTATURA to "1,75"))
        setContent { DietaTheme { AvaliacaoTela(EstadoAvaliacoes(formulario = form), AcoesAvaliacao()) } }
        onNodeWithText("IMC 22,86 · Peso normal").assertExists()
    }

    @Test fun offlineSalvaNaFila() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        setContent { DietaTheme { AvaliacaoTela(EstadoAvaliacoes(formulario = FormularioAvaliacao(), semConexao = true), AcoesAvaliacao()) } }
        onNodeWithText("Salvar avaliação").performScrollTo().assertIsEnabled()
    }

    @Test fun historicoDoPacienteNaoOfereceEdicaoEComparaDuasDatas() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        val a = item("2026-09-01T12:00:00Z", 72.0); val b = item("2026-10-01T12:00:00Z", 70.0)
        var estado by mutableStateOf(EstadoAvaliacoes(itens = listOf(b, a), somenteLeitura = true, selecionadas = listOf(a.id, b.id), comparando = true))
        setContent { DietaTheme { HistoricoAvaliacoesTela(estado, AcoesHistorico(), podeEditar = false) } }
        onNodeWithText("Peso (kg)").assertExists(); onNodeWithText("72 → 70 (-2)").assertExists()
        onNodeWithText("Nova avaliação").assertDoesNotExist()
        estado = estado.copy(comparando = false)
        onNodeWithText("Editar avaliação").assertDoesNotExist()
    }
}
