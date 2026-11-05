package org.br.dietamais.dominio.avaliacao

import org.br.dietamais.data.remoto.dto.*
import kotlin.test.*
import kotlin.time.Instant
import kotlin.uuid.Uuid

class GraficosAvaliacaoTest {
    private val quando = Instant.parse("2026-10-09T00:00:00Z")
    private fun av(peso: Double = 92.8, resultados: ResultadosAvaliacaoDto? = null, perimetros: PerimetrosAvaliacao = PerimetrosAvaliacao(),
        dobras: DobrasAvaliacao = DobrasAvaliacao(), composicao: ComposicaoAvaliacao = ComposicaoAvaliacao()) = AvaliacaoResposta(
        Uuid.random(), Uuid.random(), Uuid.random(), quando, peso, 1.75, imcDe(peso, 1.75), classificarImc(imcDe(peso, 1.75)), perimetros, dobras, composicao,
        versao = 1, criadoEm = quando, atualizadoEm = quando, resultados = resultados)

    @Test fun semMedidasNemFaixaDePesoNaoHaGraficos() {
        assertTrue(graficosDaAvaliacao(av()).isEmpty())
    }

    @Test fun pesoTrazMinimoAtualEMaximoNaOrdemDoPdf() {
        val g = graficosDaAvaliacao(av(resultados = ResultadosAvaliacaoDto(pesoTeoricoMinKg = 68.9, pesoTeoricoMaxKg = 102.5)))
        assertEquals(listOf(SerieGraficoAvaliacao.PESO), g.map { it.serie })
        assertEquals(listOf(ChaveGrafico.Peso.MINIMO, ChaveGrafico.Peso.ATUAL, ChaveGrafico.Peso.MAXIMO), g[0].pontos.map { it.chave })
        assertEquals(listOf(68.9, 92.8, 102.5), g[0].pontos.map { it.valor })
    }

    @Test fun cadaSerieSoTemOQueFoiMedido() {
        val g = graficosDaAvaliacao(av(perimetros = PerimetrosAvaliacao(braco = 35.0, cintura = 95.0), dobras = DobrasAvaliacao(triceps = 19.0),
            composicao = ComposicaoAvaliacao(percentualGordura = 26.5, pesoGordoKg = 24.0, pesoMagroKg = 67.9)))
        assertEquals(listOf(SerieGraficoAvaliacao.PERIMETROS, SerieGraficoAvaliacao.DOBRAS, SerieGraficoAvaliacao.COMPOSICAO), g.map { it.serie })
        assertEquals(listOf(MedidaAvaliacao.CINTURA, MedidaAvaliacao.BRACO).toSet(), g[0].pontos.map { (it.chave as ChaveGrafico.Medida).medida }.toSet())
        // composição só usa gordo e magro (o percentual tem outra unidade)
        assertEquals(listOf(MedidaAvaliacao.PESO_GORDO, MedidaAvaliacao.PESO_MAGRO), g[2].pontos.map { (it.chave as ChaveGrafico.Medida).medida })
    }

    @Test fun siglasNaoSeRepetemDentroDeUmaSerie() {
        listOf(SecaoAvaliacao.PERIMETROS, SecaoAvaliacao.DOBRAS).forEach { secao ->
            val siglas = MedidaAvaliacao.entries.filter { it.secao == secao }.map { siglaMedida(it) }
            assertEquals(siglas.size, siglas.toSet().size, "siglas repetidas em $secao: $siglas")
        }
    }

    @Test fun siglasSeguemOArtboard() {
        assertEquals(listOf("BR", "C", "A", "Q"), listOf(MedidaAvaliacao.BRACO, MedidaAvaliacao.CINTURA, MedidaAvaliacao.ABDOMEN, MedidaAvaliacao.QUADRIL).map { siglaMedida(it) })
        assertEquals(listOf("TR", "PE", "AM", "SE", "AB", "SI", "CX"), listOf(MedidaAvaliacao.DOBRA_TRICEPS, MedidaAvaliacao.DOBRA_PEITORAL, MedidaAvaliacao.DOBRA_AXILAR_MEDIA,
            MedidaAvaliacao.DOBRA_SUBESCAPULAR, MedidaAvaliacao.DOBRA_ABDOMINAL, MedidaAvaliacao.DOBRA_SUPRAILIACA, MedidaAvaliacao.DOBRA_COXA).map { siglaMedida(it) })
    }
}
