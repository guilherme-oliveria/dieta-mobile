package org.br.dietamais.dominio.rotina

import kotlinx.datetime.LocalDate
import org.br.dietamais.data.remoto.dto.*
import kotlin.test.*
import kotlin.time.Instant
import kotlin.uuid.Uuid

class SubstituicaoTest {
    private val grupo = Uuid.random()
    private fun alimento(nome: String, g: Uuid = grupo, gramas: Double, ativo: Boolean = true) =
        AlimentoResposta(Uuid.random(), null, g, nome, "medida", gramas, ativo, 0, Instant.parse("2026-01-01T00:00:00Z"))
    private val zero = TotaisNutricionaisResposta(0.0, 0.0, 0.0, 0.0)

    @Test fun mantemAsPorcoesDoItemComAMedidaDoSubstituto() {
        val arroz = alimento("Arroz", gramas = 25.0); val macarrao = alimento("Macarrão", gramas = 33.33); val feijao = alimento("Feijão", UUID_OUTRO, 80.0)
        val item = ItemRefeicaoResposta(Uuid.random(), arroz.id, grupo, "Arroz", 2.5, "medida", 62.5, null, zero)
        val lista = ListaSubstituicaoResposta(Uuid.random(), 0, listOf(arroz, macarrao, feijao, alimento("Cuscuz", gramas = 10.0, ativo = false)), emptyList(), emptyList())
        val opcoes = substitutosDoItem(item, lista)
        assertEquals(listOf("Macarrão"), opcoes.map { it.alimento.nome })
        assertEquals(2.5, opcoes.single().porcoes); assertEquals(83.33, opcoes.single().gramas) // 2,5 × 33,33 = 83,325 → 83,33
    }

    @Test fun janelaDeCheckinEMetaDeAgua() {
        assertEquals(listOf(28, 27, 26, 25), diasComCheckin(LocalDate(2026, 10, 28)).map { it.day })
        assertEquals(listOf(2, 1, 31, 30), diasComCheckin(LocalDate(2026, 11, 2)).map { it.day })
        assertEquals(2000, metaAguaMl(null)); assertEquals(2450, metaAguaMl(70.0)); assertEquals(2625, metaAguaMl(75.0))
    }

    private companion object { val UUID_OUTRO = Uuid.random() }
}
