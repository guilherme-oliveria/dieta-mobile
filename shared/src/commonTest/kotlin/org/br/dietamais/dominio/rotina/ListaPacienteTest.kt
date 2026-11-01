package org.br.dietamais.dominio.rotina

import kotlin.test.*
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalTime
import org.br.dietamais.data.remoto.dto.*

class ListaPacienteTest {
    private val grupo = Uuid.random()
    private fun alimento(nome: String, ativo: Boolean = true) = AlimentoResposta(Uuid.random(), null, grupo, nome, "1 unidade", 50.0, ativo, 0, Instant.DISTANT_PAST)
    @Test fun percentuaisUsamEnergiaDosMacrosENaoGramas() {
        val m = macrosPaciente(TotaisNutricionaisResposta(2509.0, 320.0, 125.0, 80.0))!!
        assertEquals(listOf(51.2, 20.0, 28.8), m.map { it.percentual })
        assertEquals(listOf(320.0, 125.0, 80.0), m.map { it.gramas })
        assertEquals(listOf(1280.0, 500.0, 720.0), m.map { it.energia })
        assertNull(macrosPaciente(TotaisNutricionaisResposta(0.0, 0.0, 0.0, 0.0)))
        assertNull(macrosPaciente(TotaisNutricionaisResposta(20.0, Double.NaN, 2.0, 1.0)))
    }
    @Test fun buscaRespeitaListaCongeladaEReferenciaAusente() {
        val pao = alimento("Pão integral"); val fruta = alimento("Maçã").copy(grupoId = Uuid.random())
        val lista = ListaSubstituicaoResposta(Uuid.random(), 0, listOf(pao, fruta, alimento("Inativo", false)), emptyList(), emptyList())
        val item = ItemRefeicaoResposta(Uuid.random(), pao.id, grupo, pao.nome, 2.0, "2 unidades", 100.0,
            totais = TotaisNutricionaisResposta(140.0, 20.0, 5.0, 2.0), grupoNome = "Pães")
        val refeicao = RefeicaoResposta(Uuid.random(), "Café", LocalTime(8, 0), 0, listOf(item), item.totais)
        val grupos = gruposListaPaciente(lista, listOf(refeicao))
        assertEquals(2, grupos.size)
        assertEquals("Pães", grupos.first { it.id == grupo }.nome)
        assertEquals(70.0, grupos.first { it.id == grupo }.kcalPorPorcao)
        assertNull(grupos.first { it.id == fruta.grupoId }.kcalPorPorcao)
        assertEquals(listOf(pao), filtrarListaPaciente(grupos, "PAO", null).single().alimentos)
        assertTrue(filtrarListaPaciente(grupos, "maça", grupo).isEmpty())
    }
    @Test fun itemPorGrupoNaoInventaEnergiaDaPorcaoBaseDaLista() {
        val pao = alimento("Pão")
        val item = ItemRefeicaoResposta(Uuid.random(), grupoId = grupo, porcoes = 2.0, totais = TotaisNutricionaisResposta(300.0, 40.0, 5.0, 2.0), grupoNome = "Pães")
        val r = RefeicaoResposta(Uuid.random(), "Café", LocalTime(8, 0), 0, listOf(item), item.totais)
        val lista = ListaSubstituicaoResposta(Uuid.random(), 0, listOf(pao), emptyList(), emptyList())
        assertNull(gruposListaPaciente(lista, listOf(r)).single().kcalPorPorcao)
    }
}
