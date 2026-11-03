package org.br.dietamais.ui.nutri

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.dominio.avaliacao.EscolhasEnergia
import org.br.dietamais.dominio.avaliacao.MedidaAvaliacao
import org.br.dietamais.dominio.calculos.NivelAtividade
import org.br.dietamais.dominio.avaliacao.PassoAvaliacao
import org.br.dietamais.dominio.calculos.ProtocoloDobras
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
        // o "Salvar" fica no último passo; offline continua habilitado (grava na fila)
        val form = FormularioAvaliacao(passo = PassoAvaliacao.RESUMO)
        setContent { DietaTheme { AvaliacaoTela(EstadoAvaliacoes(formulario = form, semConexao = true), AcoesAvaliacao()) } }
        onNodeWithText("Salvar avaliação").performScrollTo().assertIsEnabled()
        onNodeWithText("Passo 4 de 4: Resumo").assertExists()
    }

    @Test fun passoUmMostraProtocoloDeDobrasEDobrasSoDoProtocolo() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        val estado = EstadoAvaliacoes(formulario = FormularioAvaliacao(protocoloDobras = ProtocoloDobras.TRES), sexo = Sexo.MASCULINO)
        setContent { DietaTheme { AvaliacaoTela(estado, AcoesAvaliacao()) } }
        onNodeWithText("Passo 1 de 4: Medidas").assertExists()
        onNodeWithText("3 dobras").performScrollTo().assertExists()
        onNodeWithContentDescription("Peitoral (mm)").assertExists()
        onNodeWithContentDescription("Tríceps (mm)").assertDoesNotExist() // homem, 3 dobras: peitoral, abdominal e coxa
        onNodeWithText("Continuar").performScrollTo().assertIsEnabled()
    }

    @Test fun painelMostraResultadosEAvisoDoQueFalta() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        val form = FormularioAvaliacao(valores = mapOf(MedidaAvaliacao.PESO to "102,5", MedidaAvaliacao.ESTATURA to "1,93", MedidaAvaliacao.CINTURA to "100"))
        val estado = EstadoAvaliacoes(formulario = form, sexo = Sexo.MASCULINO, nascimento = kotlinx.datetime.LocalDate(1997, 4, 1))
        setContent { DietaTheme { AvaliacaoTela(estado, AcoesAvaliacao()) } }
        onNodeWithText("Resultados ao vivo").performScrollTo().assertExists()
        onNodeWithText("Informe todas as dobras do protocolo para ver o % de gordura.").performScrollTo().assertExists()
        onNodeWithText("Informe o punho para ver a compleição.").performScrollTo().assertExists()
    }

    @Test fun semSexoNaFichaExplicaPorqueNaoCalcula() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        val form = FormularioAvaliacao(valores = mapOf(MedidaAvaliacao.PESO to "70", MedidaAvaliacao.ESTATURA to "1,75"))
        setContent { DietaTheme { AvaliacaoTela(EstadoAvaliacoes(formulario = form, nascimento = kotlinx.datetime.LocalDate(1997, 4, 1)), AcoesAvaliacao()) } }
        onNodeWithText("Informe o sexo na ficha do paciente para calcular.").performScrollTo().assertExists()
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

    @Test fun passoNecessidadesMostraEscolhasEOResultadoAoVivo() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        val form = FormularioAvaliacao(passo = PassoAvaliacao.NECESSIDADES, valores = mapOf(MedidaAvaliacao.PESO to "102,5", MedidaAvaliacao.ESTATURA to "1,93"),
            energia = EscolhasEnergia(nivel = NivelAtividade.MODERADO))
        val estado = EstadoAvaliacoes(formulario = form, sexo = Sexo.MASCULINO, nascimento = kotlinx.datetime.LocalDate(1997, 4, 1))
        setContent { DietaTheme { AvaliacaoTela(estado, AcoesAvaliacao()) } }
        onNodeWithText("Passo 3 de 4: Necessidades").assertExists()
        onNodeWithText("FAO 2004").performScrollTo().assertExists()
        onNodeWithText("Faixa esportiva").performScrollTo().assertExists()
        onNodeWithText("Gasto basal (GEB): 2235,54 kcal").performScrollTo().assertExists()
        onNodeWithText("Valor energético total (VET): 3934,55 kcal").performScrollTo().assertExists()
        onNodeWithText("Carboidrato: 45 a 65% do VET · 442,64 a 639,36 g").performScrollTo().assertExists()
    }

    @Test fun passoNecessidadesSemNivelPedeONivelENaoMostraZero() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        val form = FormularioAvaliacao(passo = PassoAvaliacao.NECESSIDADES, valores = mapOf(MedidaAvaliacao.PESO to "70", MedidaAvaliacao.ESTATURA to "1,75"))
        val estado = EstadoAvaliacoes(formulario = form, sexo = Sexo.FEMININO, nascimento = kotlinx.datetime.LocalDate(1990, 1, 1))
        setContent { DietaTheme { AvaliacaoTela(estado, AcoesAvaliacao()) } }
        onNodeWithText("Escolha o nível de atividade para ver o gasto total.").performScrollTo().assertExists()
        onNodeWithText("Lactante até 6 meses").performScrollTo().assertExists()
    }
}
