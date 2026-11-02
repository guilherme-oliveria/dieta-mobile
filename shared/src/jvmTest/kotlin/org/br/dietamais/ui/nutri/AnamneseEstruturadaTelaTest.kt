package org.br.dietamais.ui.nutri

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import kotlinx.serialization.json.JsonPrimitive
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.dominio.anamnese.*
import org.br.dietamais.ui.comum.anamnese.ResultadosAnamnese
import org.br.dietamais.ui.nutri.anamnese.RespostasAnamneseTela
import org.br.dietamais.ui.paciente.anamnese.*
import org.br.dietamais.ui.theme.DietaTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.*
import kotlin.time.Instant
import kotlin.uuid.Uuid

@OptIn(ExperimentalTestApi::class)
class AnamneseEstruturadaTelaTest {
    private val sal = PerguntaQuestionario(Uuid.random(), "Sal consumido no mês (g)", TipoPergunta.NUMERO, codigo = "SAL_GRAMAS_MES")
    private val pessoas = PerguntaQuestionario(Uuid.random(), "Pessoas em casa", TipoPergunta.NUMERO, codigo = "SAL_PESSOAS")
    private val oleo = PerguntaQuestionario(Uuid.random(), "Óleo consumido no mês (ml)", TipoPergunta.NUMERO, codigo = "OLEO_ML_MES")
    private val pessoasOleo = PerguntaQuestionario(Uuid.random(), "Pessoas que consomem o óleo", TipoPergunta.NUMERO, codigo = "OLEO_PESSOAS")
    private val agora = Instant.parse("2026-11-02T00:00:00Z")
    private fun q() = QuestionarioResposta(Uuid.random(), Uuid.random(), Uuid.random(), nome = "Anamnese completa",
        blocos = listOf(BlocoQuestionario("Hábitos", listOf(sal, pessoas, oleo, pessoasOleo))),
        respostas = mapOf(sal.chave to JsonPrimitive(650), pessoas.chave to JsonPrimitive(3), oleo.chave to JsonPrimitive(2000), pessoasOleo.chave to JsonPrimitive(4)),
        status = StatusQuestionario.PENDENTE, enviadoEm = agora, versao = 1, criadoEm = agora, atualizadoEm = agora,
        derivados = AnamneseDerivada(salGramasPorPessoaDia = 7, oleoMlPorPessoaDia = 16, oleoKcalPorPessoaDia = 144))

    @Test fun mostraValoresConfirmadosESomeResultadoAoDigitarOutraEntrada() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        val q = q(); var estado by mutableStateOf(EstadoAnamnesePaciente(questionarios = listOf(q), abertoId = q.id))
        setContent { DietaTheme { AnamnesePacienteTela(estado, AcoesAnamnesePaciente(aoMudar = { id, entrada -> estado = estado.copy(entradas = estado.entradas + (id to entrada)) })) } }
        onNodeWithText("7 g").performScrollTo().assertIsDisplayed()
        onNodeWithContentDescription("Pessoas em casa").performScrollTo().performTextReplacement("4")
        onNodeWithText("7 g").assertDoesNotExist()
        onNodeWithText("Complete os campos e sincronize para atualizar os resultados.").performScrollTo().assertIsDisplayed()
    }

    @Test fun escolhaDeMedidaNaoExigeRolarUmaListaDeRadios() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        val p = PerguntaQuestionario(Uuid.random(), "Medida da bebida", TipoPergunta.ESCOLHA_UNICA, listOf("Dose (50 ml)", "Latinha (355 ml)"), codigo = "ALCOOL_CERVEJA_UNIDADE")
        var entrada by mutableStateOf<EntradaResposta>(EntradaResposta.Valor(null))
        setContent { DietaTheme { CampoPergunta(p, entrada, null, true) { entrada = it } } }
        onNodeWithContentDescription("Medida da bebida").performClick()
        onNodeWithText("Latinha (355 ml)").performClick()
        assertEquals(EntradaResposta.Valor(JsonPrimitive("Latinha (355 ml)")), entrada)
        onNodeWithText("Limpar resposta").performClick()
        assertEquals(EntradaResposta.Valor(null), entrada)
    }

    @Test fun classificacaoFuturaNaoExpoeCodigoENuloNaoViraZero() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        setContent { DietaTheme { ResultadosAnamnese(AnamneseDerivada(pressaoArterial = "FUTURA"), setOf(AssuntoAnamnese.PRESSAO, AssuntoAnamnese.SAL)) } }
        onNodeWithText("Classificação indisponível nesta versão").assertIsDisplayed()
        onNodeWithText("Ainda não calculado").assertIsDisplayed()
        onNodeWithText("0 g").assertDoesNotExist(); onNodeWithText("FUTURA").assertDoesNotExist()
    }

    @Test fun evidenciaPacienteENutriEmTresTamanhosEFonteDuzentos() {
        listOf(Triple(390, 844, 1f), Triple(820, 1180, 1f), Triple(1180, 820, 1f), Triple(390, 844, 2f)).forEach { (w, h, fonte) ->
            listOf("paciente", "nutri").forEach { tipo ->
                runSkikoComposeUiTest(size = Size(w.toFloat(), h.toFloat()), density = Density(1f, fonte)) {
                    val q = q()
                    setContent { DietaTheme { Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        if (tipo == "paciente") AnamnesePacienteTela(EstadoAnamnesePaciente(questionarios = listOf(q), abertoId = q.id), AcoesAnamnesePaciente())
                        else RespostasAnamneseTela(q.copy(status = StatusQuestionario.RESPONDIDO), {})
                    } } }
                    waitForIdle()
                    var arquivo = File("build/reports/MOB-61/$tipo-$w-fonte-$fonte-campos.png")
                    val raiz = onAllNodes(isRoot())[if (tipo == "paciente") 1 else 0]
                    arquivo.parentFile.mkdirs(); ImageIO.write(raiz.captureToImage().toAwtImage(), "png", arquivo)
                    onNodeWithText("144 kcal").performScrollTo().assertIsDisplayed()
                    arquivo = File("build/reports/MOB-61/$tipo-$w-fonte-$fonte-resultados.png")
                    ImageIO.write(raiz.captureToImage().toAwtImage(), "png", arquivo)
                    if (tipo == "paciente") onNodeWithText("Enviar respostas").assertIsDisplayed()
                }
            }
        }
    }
}
