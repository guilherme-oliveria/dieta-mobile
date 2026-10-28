package org.br.dietamais.ui.nutri

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.dominio.anamnese.*
import org.br.dietamais.ui.nutri.anamnese.*
import org.br.dietamais.ui.paciente.anamnese.*
import org.br.dietamais.ui.theme.DietaTheme
import kotlin.test.*
import kotlin.time.Instant
import kotlin.uuid.Uuid

@OptIn(ExperimentalTestApi::class)
class AnamneseTelasTest {
    private val agora = Instant.parse("2026-10-28T12:00:00Z")
    private val fuma = PerguntaQuestionario(Uuid.random(), "Você fuma?", TipoPergunta.ESCOLHA_UNICA, listOf("Sim", "Não"), true)
    private val peso = PerguntaQuestionario(Uuid.random(), "Peso desejado", TipoPergunta.NUMERO)
    private val blocos = listOf(BlocoQuestionario("Hábitos", listOf(fuma)), BlocoQuestionario("Metas", listOf(peso)))
    private fun questionario(status: StatusQuestionario = StatusQuestionario.PENDENTE, respostas: Map<Uuid, kotlinx.serialization.json.JsonElement> = emptyMap()) =
        QuestionarioResposta(Uuid.random(), Uuid.random(), Uuid.random(), null, "Anamnese inicial", blocos, respostas, status, agora, null, 1, agora, agora)

    @Test fun nutriVeModelosEQuestionariosEEnviaOffline() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        val modelo = ModeloQuestionarioResposta(Uuid.random(), "Anamnese padrão", true, blocos, 1, agora, agora)
        var estado = EstadoAnamneseNutri(modelos = listOf(modelo), questionarios = listOf(questionario(StatusQuestionario.RESPONDIDO)), semConexao = true)
        setContent { DietaTheme { AnamneseNutriTela(estado, AcoesAnamneseNutri()) } }
        onNodeWithText("Anamnese padrão").assertIsDisplayed()
        onNodeWithText("Respondido").assertIsDisplayed()
        onNodeWithText("Enviar").assertIsNotEnabled()
    }

    @Test fun respostasMostramSoPerguntasVisiveisESemRespostaQuandoVazio() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        val q = questionario(StatusQuestionario.RESPONDIDO, mapOf(fuma.chave to kotlinx.serialization.json.JsonPrimitive("Não")))
        setContent { DietaTheme { RespostasAnamneseTela(q, {}) } }
        onNodeWithText("Você fuma?").assertIsDisplayed(); onNodeWithText("Não").assertIsDisplayed()
        onNodeWithText("Sem resposta").assertIsDisplayed()
    }

    @Test fun editorMostraErrosDeValidacao() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        val editor = editorNovo(Uuid.random()).adicionarPergunta(0, Uuid.random())
        setContent { DietaTheme { ModeloAnamneseTela(EstadoAnamneseNutri(editor = editor, validacao = editor.validar()), AcoesEditorModelo()) } }
        onNodeWithText("Dê um nome ao modelo.").assertIsDisplayed()
        onNodeWithText("Dê um título ao bloco.").performScrollTo().assertIsDisplayed()
        onNodeWithText("Escreva a pergunta.").performScrollTo().assertIsDisplayed()
    }

    @Test fun editorOfflineNaoSalva() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        val editor = editorNovo(Uuid.random())
        setContent { DietaTheme { ModeloAnamneseTela(EstadoAnamneseNutri(editor = editor, semConexao = true), AcoesEditorModelo()) } }
        onNodeWithText("Salvar modelo").performScrollTo().assertIsNotEnabled()
    }

    @Test fun assistenteMostraPrimeiroBlocoEObrigatoriaEAvancaPeloBotao() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        var avancou = 0
        val estado = EstadoAnamnesePaciente(questionarios = listOf(questionario()), abertoId = null).let { it.copy(abertoId = it.questionarios.first().id) }
        setContent { DietaTheme { AnamnesePacienteTela(estado, AcoesAnamnesePaciente(aoProximo = { avancou++ })) } }
        onNodeWithText("Parte 1 de 2 · Hábitos").assertIsDisplayed()
        onNodeWithText("Você fuma? (obrigatória)").assertExists()
        onNodeWithText("Próximo").performClick()
        assertEquals(1, avancou)
        onNodeWithText("Enviar respostas").assertDoesNotExist()
    }

    @Test fun ultimoPassoExigeConexaoParaEnviar() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        val q = questionario(respostas = mapOf(fuma.chave to kotlinx.serialization.json.JsonPrimitive("Não")))
        val estado = EstadoAnamnesePaciente(questionarios = listOf(q), abertoId = q.id, passo = 1, semConexao = true)
        setContent { DietaTheme { AnamnesePacienteTela(estado, AcoesAnamnesePaciente()) } }
        onNodeWithText("Enviar respostas").assertIsNotEnabled()
        onNodeWithText("Anterior").assertIsEnabled()
    }

    @Test fun bannerAparecePendenteESomeSemPendencia() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        var estado by androidx.compose.runtime.mutableStateOf(EstadoAnamnesePaciente(questionarios = listOf(questionario())))
        var aberto: Uuid? = null
        setContent { DietaTheme { BannerQuestionario(estado, AcoesAnamnesePaciente(aoAbrir = { aberto = it })) } }
        onNodeWithText("Você tem um questionário para responder").assertIsDisplayed()
        onNodeWithText("Responder").performClick()
        assertNotNull(aberto)
        estado = EstadoAnamnesePaciente(questionarios = listOf(questionario(StatusQuestionario.RESPONDIDO)))
        onNodeWithText("Você tem um questionário para responder").assertDoesNotExist()
    }
}
