package org.br.dietamais.dominio.anamnese

import kotlinx.serialization.json.*
import org.br.dietamais.data.remoto.JsonContrato
import org.br.dietamais.data.remoto.dto.*
import kotlin.test.*
import kotlin.time.Instant
import kotlin.uuid.Uuid

class AnamneseEstruturadaTest {
    private val id = Uuid.random()
    private val pergunta = PerguntaQuestionario(id, "Pessoas", TipoPergunta.NUMERO, codigo = "SAL_PESSOAS")
    private val agora = Instant.parse("2026-11-02T00:00:00Z")
    private fun q() = QuestionarioResposta(Uuid.random(), Uuid.random(), Uuid.random(), nome = "Anamnese", blocos = listOf(BlocoQuestionario("Hábitos", listOf(pergunta))),
        respostas = mapOf(id to JsonPrimitive(3)), status = StatusQuestionario.PENDENTE, enviadoEm = agora, versao = 1, criadoEm = agora, atualizadoEm = agora,
        derivados = AnamneseDerivada(salGramasPorPessoaDia = 7))

    @Test fun preservaCodigoEResultadoNoContratoENoEditor() {
        val original = q()
        val recebido = JsonContrato.decodeFromString<QuestionarioResposta>(JsonContrato.encodeToString(original))
        assertEquals("SAL_PESSOAS", recebido.blocos.single().perguntas.single().codigo)
        assertEquals(7, recebido.derivados?.salGramasPorPessoaDia)
        val modelo = ModeloQuestionarioResposta(Uuid.random(), "Modelo", true, original.blocos, 1, agora, agora)
        val editado = editorDe(modelo).mudarNome("Meu modelo").paraRequisicao()
        assertEquals("SAL_PESSOAS", editado.blocos.single().perguntas.single().codigo)
    }

    @Test fun contratoAntigoPermaneceValidoEClasseFuturaNaoQuebra() {
        val json = JsonContrato.encodeToJsonElement(q()).jsonObject
        val antigo = JsonContrato.decodeFromJsonElement<QuestionarioResposta>(JsonObject(json - "derivados"))
        assertNull(antigo.derivados)
        val futuro = JsonContrato.decodeFromString<AnamneseDerivada>("""{"pressaoArterial":"NOVA_CLASSE","novoCampo":42}""")
        assertEquals("NOVA_CLASSE", futuro.pressaoArterial)
    }

    @Test fun mesclaLocalApagaDerivadosEPermiteExcluirEntrada() {
        val original = q()
        val editado = original.comRespostasLocais(mapOf(id to JsonPrimitive(4)))
        assertNull(editado.derivados)
        assertEquals(JsonPrimitive(4), editado.respostas[id])
        assertNull(editado.comRespostasLocais(mapOf(id to JsonNull)).respostas[id])
        assertEquals(original.derivados, original.comRespostasLocais(emptyMap()).derivados)
    }

    @Test fun limitesNumericosConhecidosSaoFeedbackSemAlterarPerguntaGenerica() {
        assertFalse(converter(pergunta, EntradaResposta.Texto("0")).valida)
        assertFalse(converter(pergunta, EntradaResposta.Texto("1,5")).valida)
        assertTrue(converter(pergunta, EntradaResposta.Texto("3")).valida)
        assertTrue(converter(pergunta.copy(codigo = null), EntradaResposta.Texto("0")).valida)
        assertFalse(converter(pergunta.copy(codigo = "ALCOOL_CERVEJA_DIAS_SEMANA"), EntradaResposta.Texto("8")).valida)
    }

    @Test fun digitacaoDiferenteOuInvalidaEscondeResultadoMasTresETresVirgulaZeroSaoIguais() {
        val original = q()
        assertNotNull(derivadosAtuais(original, mapOf(id to EntradaResposta.Texto("3,0"))))
        assertNull(derivadosAtuais(original, mapOf(id to EntradaResposta.Texto("4"))))
        assertNull(derivadosAtuais(original, mapOf(id to EntradaResposta.Texto("inválido"))))
    }
}
