package org.br.dietamais.ui.nutri.dieta

import kotlinx.coroutines.test.runTest
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.PortaModelosDieta
import kotlin.test.*
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalTime

class ModeloComoDietaTest {
    private val agora = Instant.parse("2026-11-05T12:00:00Z")
    private val grupo = Uuid.random()
    private fun modelo(escopo: String = "NUTRI", itens: List<ItemRefeicaoRequisicao> = listOf(ItemRefeicaoRequisicao(id = Uuid.random(), grupoId = grupo, porcoes = 2.0))) =
        ModeloDietaResposta(Uuid.random(), escopo, "Hipertensão", descricao = "Menos sódio", condicao = "HAS", orientacao = "Beba água", restricoes = listOf("ALTO_SODIO"),
            kcalReferencia = 1800.0, rodape = "Rodapé", porcoes = listOf(PorcaoGrupo(grupo, 1)), metas = MetasDieta(1800.0, 50.0, 20.0, 30.0), versao = 4, criadoEm = agora, atualizadoEm = agora,
            refeicoes = listOf(RefeicaoRequisicao(id = Uuid.random(), nome = "Desjejum", horario = LocalTime(7, 0), ordem = 0, itens = itens)))

    private class Modelos : PortaModelosDieta {
        val criados = mutableListOf<SalvarModeloDietaRequisicao>(); val atualizados = mutableListOf<Triple<Uuid, Long, SalvarModeloDietaRequisicao>>()
        private fun resposta(r: SalvarModeloDietaRequisicao, id: Uuid, versao: Long) = ModeloDietaResposta(id, "NUTRI", r.titulo, r.descricao, r.condicao, r.orientacao, r.refeicoes, r.porcoes, r.metas,
            r.rodape, r.restricoes.orEmpty(), r.kcalReferencia, versao, Instant.parse("2026-11-05T12:00:00Z"), Instant.parse("2026-11-05T12:00:00Z"))
        override suspend fun criar(requisicao: SalvarModeloDietaRequisicao, chave: Uuid): ModeloDietaResposta { criados += requisicao; return resposta(requisicao, requisicao.id ?: Uuid.random(), 1) }
        override suspend fun atualizar(id: Uuid, versao: Long, requisicao: SalvarModeloDietaRequisicao): ModeloDietaResposta { atualizados += Triple(id, versao, requisicao); return resposta(requisicao, id, versao + 1) }
        override suspend fun listar() = emptyList<ModeloDietaResposta>()
        override suspend fun duplicar(id: Uuid, chave: Uuid) = error("não usado")
        override suspend fun excluir(id: Uuid, versao: Long) = error("não usado")
        override suspend fun salvarComoModelo(dietaId: Uuid, requisicao: SalvarComoModeloRequisicao, chave: Uuid) = error("não usado")
        override suspend fun referenciaAvaliacao(pacienteId: Uuid) = null
        override suspend fun aplicar(pacienteId: Uuid, modeloId: Uuid, requisicao: AplicarModeloRequisicao, chave: Uuid) = error("não usado")
    }

    @Test fun modeloViraRascunhoSemPacienteEComConteudoPreservado() {
        val m = modelo(); val d = ModeloComoDieta.comoDieta(m)
        assertEquals(m.id, d.id); assertEquals(ModeloComoDieta.SEM_PACIENTE, d.pacienteId); assertEquals(StatusDieta.RASCUNHO, d.status)
        assertEquals(m.versao, d.versao); assertEquals(m.porcoes, d.porcoes); assertEquals(m.metas, d.metas); assertEquals("Rodapé", d.rodape)
        val item = d.refeicoes.single().itens.single()
        assertEquals(m.refeicoes.single().itens.single().id, item.id); assertEquals(grupo, item.grupoId); assertEquals(2.0, item.porcoes)
        assertTrue(usaPorcoes(d))
    }
    @Test fun soModeloDelaSemAlimentoDoCatalogoEEditavel() {
        assertTrue(ModeloComoDieta.editavel(modelo()))
        assertFalse(ModeloComoDieta.editavel(modelo("SISTEMA")))
        assertFalse(ModeloComoDieta.editavel(modelo(itens = listOf(ItemRefeicaoRequisicao(alimentoId = Uuid.random(), porcoes = 1.0)))))
    }
    @Test fun salvarAtualizaComVersaoEMantemDescricaoCondicaoERestricoes() = runTest {
        val modelos = Modelos(); val ponte = DietasDeModelo(modelos); val m = modelo(); ponte.base = m
        val pedido = SalvarDietaRequisicao(titulo = "Novo título", refeicoes = m.refeicoes, porcoes = m.porcoes, metas = m.metas)
        val resposta = ponte.salvar(m.id, m.versao, pedido)
        val (id, versao, corpo) = modelos.atualizados.single()
        assertEquals(m.id, id); assertEquals(4L, versao); assertEquals("Novo título", corpo.titulo)
        assertEquals("Menos sódio", corpo.descricao); assertEquals("HAS", corpo.condicao); assertEquals(listOf("ALTO_SODIO"), corpo.restricoes); assertNull(corpo.id)
        assertEquals(5L, resposta.versao); assertEquals(5L, ponte.base?.versao)
    }
    @Test fun modeloNovoNasceSemDescricaoEComIdDoRascunho() = runTest {
        val modelos = Modelos(); val ponte = DietasDeModelo(modelos); val id = Uuid.random()
        val criada = ponte.criar(ModeloComoDieta.SEM_PACIENTE, SalvarDietaRequisicao(id = id, titulo = "Do zero", refeicoes = emptyList()), Uuid.random())
        val corpo = modelos.criados.single()
        assertEquals(id, corpo.id); assertNull(corpo.descricao); assertNull(corpo.restricoes); assertEquals(id, criada.id); assertEquals(1L, criada.versao)
    }
}
