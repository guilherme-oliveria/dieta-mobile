package org.br.dietamais.dominio.anamnese

import org.br.dietamais.data.remoto.dto.*
import kotlin.test.*
import kotlin.uuid.Uuid

class EditorModeloTest {
    private val a = Uuid.random(); private val b = Uuid.random(); private val c = Uuid.random()
    private fun base() = editorNovo(Uuid.random()).mudarNome("Modelo").mudarBloco(0, "Hábitos")
        .adicionarPergunta(0, a).adicionarPergunta(0, b).adicionarPergunta(0, c)
        .mudarPergunta(a) { it.copy(texto = "Fuma?", tipo = TipoPergunta.ESCOLHA_UNICA, opcoes = "Sim\nNão") }
        .mudarPergunta(b) { it.copy(texto = "Quantos?", tipo = TipoPergunta.NUMERO) }
        .mudarPergunta(c) { it.copy(texto = "Obs") }

    @Test fun modeloCompletoEhValidoEViraRequisicao() {
        val e = base().ligarCondicao(b, a).mudarPergunta(b) { it.copy(condicaoValor = "Sim", obrigatoria = true) }
        assertTrue(e.validar().valido)
        val r = e.paraRequisicao()
        assertEquals(e.chave, r.id); assertEquals("Modelo", r.nome)
        val p = r.blocos.single().perguntas
        assertEquals(listOf("Sim", "Não"), p[0].opcoes); assertNull(p[1].opcoes)
        assertEquals(CondicaoPergunta(a, OperadorCondicao.IGUAL, "Sim"), p[1].condicao); assertTrue(p[1].obrigatoria)
    }

    @Test fun apontaOsErrosDeCadaCampo() {
        val v = editorNovo(Uuid.random()).adicionarPergunta(0, a).validar()
        assertEquals(ErroModelo.NOME_VAZIO, v.nome); assertEquals(ErroModelo.BLOCO_SEM_TITULO, v.blocos[0]); assertEquals(ErroModelo.PERGUNTA_SEM_TEXTO, v.perguntas[a])
        assertEquals(ErroModelo.SEM_PERGUNTAS, editorNovo(Uuid.random()).mudarNome("x").validar().nome)
        val opcoes = base().mudarPergunta(a) { it.copy(opcoes = "Só uma") }.validar()
        assertEquals(ErroModelo.OPCOES_INSUFICIENTES, opcoes.perguntas[a])
        assertEquals(ErroModelo.OPCOES_REPETIDAS, base().mudarPergunta(a) { it.copy(opcoes = "Sim\nSim") }.validar().perguntas[a])
    }

    @Test fun condicaoSemValorEIncompleta() {
        val v = base().ligarCondicao(b, a).validar()
        assertEquals(ErroModelo.CONDICAO_INCOMPLETA, v.perguntas[b])
    }

    @Test fun valorDaCondicaoPrecisaEstarNasOpcoesDaReferencia() {
        val e = base().ligarCondicao(b, a).mudarPergunta(b) { it.copy(condicaoValor = "Talvez") }
        // ao mudar uma pergunta, o editor descarta o valor que saiu das opções
        assertEquals("", e.blocos[0].perguntas[1].condicaoValor)
    }

    @Test fun contemSoParaMultiplaEscolha() {
        val e = base().ligarCondicao(b, a).mudarPergunta(b) { it.copy(condicaoOperador = OperadorCondicao.CONTEM, condicaoValor = "Sim") }
        assertEquals(OperadorCondicao.IGUAL, e.blocos[0].perguntas[1].condicaoOperador)
        val multipla = base().mudarPergunta(a) { it.copy(tipo = TipoPergunta.MULTIPLA_ESCOLHA) }.ligarCondicao(b, a)
            .mudarPergunta(b) { it.copy(condicaoOperador = OperadorCondicao.CONTEM, condicaoValor = "Sim") }
        assertEquals(OperadorCondicao.CONTEM, multipla.blocos[0].perguntas[1].condicaoOperador)
        assertEquals(listOf(OperadorCondicao.IGUAL, OperadorCondicao.DIFERENTE, OperadorCondicao.CONTEM), operadoresPara(multipla.blocos[0].perguntas[0]))
    }

    @Test fun soPerguntasAnterioresServemDeReferencia() {
        val e = base()
        assertEquals(emptyList(), e.anteriores(a)); assertEquals(listOf(a), e.anteriores(b).map { it.id }); assertEquals(listOf(a, b), e.anteriores(c).map { it.id })
    }

    @Test fun removerOuMoverReferenciaDesligaACondicao() {
        val com = base().ligarCondicao(b, a)
        assertNull(com.removerPergunta(a).blocos[0].perguntas.first { it.id == b }.condicaoPergunta)
        assertNull(com.moverPergunta(a, 1).blocos[0].perguntas.first { it.id == b }.condicaoPergunta) // a passou para depois de b
        assertEquals(listOf(b, a, c), com.moverPergunta(a, 1).blocos[0].perguntas.map { it.id })
        assertEquals(listOf(a, b, c), com.moverPergunta(a, -1).blocos[0].perguntas.map { it.id }) // no limite não move
    }

    @Test fun removerBlocoLevaAsPerguntasEDesligaCondicoesQueApontavamParaElas() {
        val e = base().adicionarBloco().mudarBloco(1, "Extra").adicionarPergunta(1, Uuid.random())
        assertEquals(2, e.blocos.size); assertEquals(1, e.removerBloco(1).blocos.size)
    }

    @Test fun trocarOTipoParaUmSemOpcoesNaoMandaOpcoes() {
        val r = base().mudarPergunta(a) { it.copy(tipo = TipoPergunta.TEXTO) }.paraRequisicao()
        assertNull(r.blocos[0].perguntas[0].opcoes)
    }

    @Test fun editarModeloExistenteMantemIdVersaoEIdsDePergunta() {
        val modelo = ModeloQuestionarioResposta(Uuid.random(), "Padrão", true, listOf(BlocoQuestionario("B", listOf(PerguntaQuestionario(a, "P", TipoPergunta.ESCOLHA_UNICA, listOf("x", "y"), true)))),
            3, kotlin.time.Instant.parse("2026-01-01T00:00:00Z"), kotlin.time.Instant.parse("2026-01-01T00:00:00Z"))
        val e = editorDe(modelo)
        assertEquals(modelo.id, e.id); assertEquals(3L, e.versao)
        assertEquals(modelo.id, e.paraRequisicao().id); assertEquals(a, e.paraRequisicao().blocos[0].perguntas[0].id)
        assertTrue(e.validar().valido)
    }
}
