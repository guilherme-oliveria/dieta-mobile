package org.br.dietamais.dominio.rotina

import kotlinx.datetime.LocalTime
import org.br.dietamais.data.remoto.dto.*
import kotlin.test.*
import kotlin.time.Instant
import kotlin.uuid.Uuid

class ListaComprasTest {
    private val zero = TotaisNutricionaisResposta(0.0, 0.0, 0.0, 0.0)
    private val arroz = Uuid.random(); private val ovo = Uuid.random(); private val fruta = Uuid.random()
    private fun item(a: Uuid, nome: String, gramas: Double, porcoes: Double, grupo: String?) =
        ItemRefeicaoResposta(Uuid.random(), a, Uuid.random(), nome, porcoes, "medida", gramas, null, zero, grupo)
    private fun refeicao(vararg itens: ItemRefeicaoResposta) = RefeicaoResposta(Uuid.random(), "R", LocalTime(8, 0), 0, itens.toList(), zero)
    private fun dieta(vararg r: RefeicaoResposta) = DietaSnapshotResposta(Uuid.random(), Uuid.random(), Uuid.random(), Uuid.random(),
        NutricionistaResumoResposta(Uuid.random(), "Ana", "CRN"), "D", null, r.toList(),
        ListaSubstituicaoResposta(Uuid.random(), 0, emptyList(), emptyList(), emptyList()), zero, Instant.parse("2026-01-01T00:00:00Z"))

    @Test fun somaPorAlimentoMultiplicaPelosDiasEAgrupa() {
        val d = dieta(refeicao(item(arroz, "Arroz", 100.0, 4.0, "Arroz"), item(ovo, "Ovo", 50.0, 1.0, "Carne ou ovo")),
            refeicao(item(arroz, "Arroz", 150.0, 6.0, "Arroz"), item(fruta, "Banana", 70.0, 1.0, null)))
        val l = gerarListaCompras(d, 7)
        assertEquals(listOf<String?>("Arroz", "Carne ou ovo", null), l.map { it.nome })
        assertEquals(1750.0, l[0].itens.single().gramas); assertEquals(70.0, l[0].itens.single().porcoes)
        assertEquals(350.0, l[1].itens.single().gramas); assertEquals(490.0, l[2].itens.single().gramas)
    }

    @Test fun formataPesoEmPtBrETextoParaCompartilhar() {
        assertEquals("850 g", textoPeso(850.0)); assertEquals("1,75 kg", textoPeso(1750.0)); assertEquals("1 kg", textoPeso(1000.0)); assertEquals("12,5 g", textoPeso(12.5))
        val d = dieta(refeicao(item(arroz, "Arroz", 100.0, 4.0, "Arroz")))
        val texto = textoListaCompras(gerarListaCompras(d, 7), setOf(arroz), "Lista de compras")
        assertEquals("Lista de compras\n\nArroz\n[x] Arroz: 700 g", texto)
    }
}
