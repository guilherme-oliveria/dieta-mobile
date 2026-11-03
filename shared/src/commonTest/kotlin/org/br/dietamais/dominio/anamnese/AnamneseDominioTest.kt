package org.br.dietamais.dominio.anamnese

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.ui.nutri.anamnese.perguntaFalsa
import org.br.dietamais.ui.nutri.anamnese.texto
import kotlin.test.*

class AnamneseDominioTest {
    private val fuma = perguntaFalsa("Você fuma?", TipoPergunta.ESCOLHA_UNICA, true, listOf("Sim", "Não"))
    private val quanto = perguntaFalsa("Quantos por dia?", TipoPergunta.NUMERO, true, condicao = CondicaoPergunta(fuma.chave, OperadorCondicao.IGUAL, "Sim"))
    private val alergias = perguntaFalsa("Alergias", TipoPergunta.MULTIPLA_ESCOLHA, false, listOf("Glúten", "Lactose", "Nenhuma"))
    private val semLactose = perguntaFalsa("Sintomas", TipoPergunta.TEXTO, condicao = CondicaoPergunta(alergias.chave, OperadorCondicao.CONTEM, "Lactose"))
    private val naoAlergia = perguntaFalsa("Outra", TipoPergunta.TEXTO, condicao = CondicaoPergunta(alergias.chave, OperadorCondicao.DIFERENTE, "Glúten"))
    private val blocos = listOf(BlocoQuestionario("Hábitos", listOf(fuma, quanto)), BlocoQuestionario("Saúde", listOf(alergias, semLactose, naoAlergia)))

    @Test fun condicaoIgualSoMostraQuandoARespostaCoincide() {
        assertFalse(visivel(quanto, emptyMap()))
        assertFalse(visivel(quanto, mapOf(fuma.chave to texto("Não"))))
        assertTrue(visivel(quanto, mapOf(fuma.chave to texto("Sim"))))
    }

    @Test fun condicaoContemEDiferenteSeguemORegraDoBackend() {
        assertFalse(visivel(semLactose, emptyMap()))
        assertTrue(visivel(semLactose, mapOf(alergias.chave to jsonLista(listOf("Glúten", "Lactose")))))
        assertTrue(visivel(naoAlergia, emptyMap())) // sem resposta, DIFERENTE mostra
        assertFalse(visivel(naoAlergia, mapOf(alergias.chave to jsonLista(listOf("Glúten")))))
    }

    @Test fun operadorDesconhecidoMostraAPergunta() {
        val p = perguntaFalsa("x", condicao = CondicaoPergunta(fuma.chave, OperadorCondicao.OUTRO, "Sim"))
        assertTrue(visivel(p, emptyMap()))
    }

    @Test fun passosPulamBlocosSemPerguntaVisivel() {
        val so = listOf(BlocoQuestionario("A", listOf(fuma)), BlocoQuestionario("B", listOf(quanto)))
        assertEquals(listOf("A"), passos(so, emptyMap()).map { it.titulo })
        assertEquals(listOf("A", "B"), passos(so, mapOf(fuma.chave to texto("Sim"))).map { it.titulo })
    }

    @Test fun faltandoIgnoraObrigatoriasEscondidas() {
        assertEquals(listOf(fuma.chave), faltando(blocos, emptyMap()))
        assertEquals(listOf(quanto.chave), faltando(blocos, mapOf(fuma.chave to texto("Sim"))))
        assertEquals(emptyList(), faltando(blocos, mapOf(fuma.chave to texto("Não"))))
    }

    @Test fun progressoContaSoPerguntasVisiveis() {
        assertEquals(0 to 3, progresso(blocos, emptyMap())) // visíveis: fuma, alergias e naoAlergia (DIFERENTE sem resposta mostra)
        assertEquals(2 to 4, progresso(blocos, mapOf(fuma.chave to texto("Sim"), alergias.chave to jsonLista(listOf("Nenhuma")))))
    }

    @Test fun validaCadaTipo() {
        assertTrue(respostaValida(fuma, texto("Sim"))); assertFalse(respostaValida(fuma, texto("Talvez")))
        assertTrue(respostaValida(alergias, jsonLista(listOf("Glúten")))); assertFalse(respostaValida(alergias, jsonLista(listOf("Glúten", "Glúten"))))
        assertFalse(respostaValida(alergias, jsonLista(emptyList())))
        assertTrue(respostaValida(perguntaFalsa("e", TipoPergunta.ESCALA_1_5), JsonPrimitive(5))); assertFalse(respostaValida(perguntaFalsa("e", TipoPergunta.ESCALA_1_5), JsonPrimitive(6)))
        val hora = perguntaFalsa("h", TipoPergunta.HORARIO)
        assertTrue(respostaValida(hora, texto("07:30"))); assertFalse(respostaValida(hora, texto("7:30"))); assertFalse(respostaValida(hora, texto("24:00")))
        val lista = perguntaFalsa("l", TipoPergunta.LISTA_POR_REFEICAO)
        assertTrue(respostaValida(lista, jsonItens(listOf(ItemRefeicao("Café", "Pão", "07:00")))))
        assertFalse(respostaValida(lista, jsonItens(listOf(ItemRefeicao("Café", "", null)))))
        assertFalse(respostaValida(lista, jsonItens(emptyList())))
    }

    @Test fun converteDigitacaoEmResposta() {
        val numero = perguntaFalsa("n", TipoPergunta.NUMERO)
        assertEquals(RespostaConvertida(JsonPrimitive(12.5), true), converter(numero, EntradaResposta.Texto(" 12,5 ")))
        assertEquals(RespostaConvertida(null, true), converter(numero, EntradaResposta.Texto("  ")))
        assertFalse(converter(numero, EntradaResposta.Texto("abc")).valida)
        val hora = perguntaFalsa("h", TipoPergunta.HORARIO)
        assertFalse(converter(hora, EntradaResposta.Texto("25:00")).valida)
        val lista = perguntaFalsa("l", TipoPergunta.LISTA_POR_REFEICAO)
        assertEquals(RespostaConvertida(null, true), converter(lista, EntradaResposta.Itens(listOf(ItemRefeicao("", "")))))
        assertFalse(converter(lista, EntradaResposta.Itens(listOf(ItemRefeicao("Café", "")))).valida)
        assertTrue(converter(lista, EntradaResposta.Itens(listOf(ItemRefeicao(" Café ", " Pão ", "")))).valida)
    }

    @Test fun respostasEfetivasSobrepoemADigitacaoValidaEIgnoramInvalida() {
        val gravadas = mapOf(fuma.chave to texto("Não"))
        val ef = respostasEfetivas(gravadas, mapOf(fuma.chave to EntradaResposta.Valor(texto("Sim")), quanto.chave to EntradaResposta.Texto("abc")), listOf(fuma, quanto))
        assertEquals(texto("Sim"), ef[fuma.chave]); assertNull(ef[quanto.chave])
        val limpa = respostasEfetivas(gravadas, mapOf(fuma.chave to EntradaResposta.Valor(null)), listOf(fuma))
        assertTrue(limpa.isEmpty())
    }

    @Test fun consolidarSeparaGravarEErros() {
        val numero = perguntaFalsa("n", TipoPergunta.NUMERO)
        val r = consolidar(listOf(fuma, numero), mapOf(fuma.chave to EntradaResposta.Valor(texto("Sim")), numero.chave to EntradaResposta.Texto("x")))
        assertEquals(mapOf<kotlin.uuid.Uuid, kotlinx.serialization.json.JsonElement?>(fuma.chave to texto("Sim")), r.gravar)
        assertEquals(mapOf(numero.chave to ErroResposta.INVALIDA), r.erros)
    }

    @Test fun textoDaRespostaLegivel() {
        assertEquals("Glúten, Lactose", textoDaResposta(alergias, jsonLista(listOf("Glúten", "Lactose"))))
        val lista = perguntaFalsa("l", TipoPergunta.LISTA_POR_REFEICAO)
        assertEquals("07:00 Café: Pão", textoDaResposta(lista, jsonItens(listOf(ItemRefeicao("Café", "Pão", "07:00")))))
        assertNull(textoDaResposta(fuma, null))
    }

    @Test fun entradaInicialVemDaRespostaGravada() {
        val numero = perguntaFalsa("n", TipoPergunta.NUMERO)
        assertEquals(EntradaResposta.Texto("12,5"), entradaInicial(numero, JsonPrimitive(12.5)))
        assertEquals(EntradaResposta.Valor(texto("Sim")), entradaInicial(fuma, texto("Sim")))
        assertEquals(EntradaResposta.Itens(emptyList()), entradaInicial(perguntaFalsa("l", TipoPergunta.LISTA_POR_REFEICAO), null))
    }
}
