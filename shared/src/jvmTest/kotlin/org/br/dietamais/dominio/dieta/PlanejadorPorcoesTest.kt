package org.br.dietamais.dominio.dieta

import kotlinx.serialization.json.*
import org.br.dietamais.data.remoto.dto.MetasDieta
import org.br.dietamais.data.remoto.dto.OpcaoPorcaoResposta
import kotlin.math.abs
import kotlin.test.*

/** Roda `contrato/calculos/dieta-porcoes.json` (mesmo arquivo do backend, RN-CAL-06) sobre os perfis das porções (mesmos CSVs do Liquibase). */
class PlanejadorPorcoesTest {
    private fun recurso(caminho: String) = PlanejadorPorcoesTest::class.java.getResourceAsStream(caminho)!!.bufferedReader(Charsets.UTF_8).use { it.readText() }
    private fun csv(nome: String) = recurso("/contrato/porcoes/$nome").lines().drop(1).filter { it.isNotBlank() }.map { it.split(";") }

    private val nutrientes: Map<String, Map<String, Double>> =
        csv("grupo-porcao-nutriente.csv").groupBy({ it[0] }, { it[1] to it[2].toDouble() }).mapValues { it.value.toMap() }

    /** (grupo 1..15, opção) -> porção. */
    private val opcoes: Map<Pair<Int, Int>, OpcaoPorcaoResposta> = csv("grupo-porcao.csv").associate { c ->
        val grupo = c[1].takeLast(12).toInt()
        (grupo to c[2].toInt()) to OpcaoPorcaoResposta(
            opcao = c[2].toInt(), kcal = c[3].toDouble(), quantidade = c[4].takeUnless { it == "NULL" }?.toDouble(), unidade = c[5].takeUnless { it == "NULL" },
            medidaCaseira = c[6].takeUnless { it == "NULL" || it.isBlank() }, guiaAlimentar = c[7].toBoolean(), nutrientes = nutrientes[c[0]].orEmpty(),
        )
    }

    private fun itens(caso: JsonObject) = caso.getValue("itens").jsonArray.map { it.jsonObject }.map { i ->
        ItemPorcao(opcoes.getValue(i.getValue("grupo").jsonPrimitive.int to i.getValue("opcao").jsonPrimitive.int), i.getValue("porcoes").jsonPrimitive.content.toDouble())
    }

    private fun conferir(rotulo: String, totais: TotaisPorcoes, esperado: JsonObject, tolerancia: Double) {
        for ((cod, v) in esperado) {
            val real = when (cod) {
                "energia" -> totais.energia; "gli" -> totais.carboidratos; "pro" -> totais.proteinas; "lip" -> totais.gorduras
                else -> totais.micronutrientes.getValue(cod)
            }
            val esp = v.jsonPrimitive.content.toDouble()
            assertTrue(abs(real - esp) <= tolerancia, "$rotulo $cod: esperado $esp, veio $real")
        }
    }

    @Test
    fun refeicoesEDiaBatemComOsVetoresDoContrato() {
        val raiz = Json.parseToJsonElement(recurso("/contrato/calculos/dieta-porcoes.json")).jsonObject
        val casos = raiz.getValue("casos").jsonArray.map { it.jsonObject }
        assertEquals(4, casos.size)
        for (caso in casos) conferir(caso.getValue("refeicao").jsonPrimitive.content, PlanejadorPorcoes.somar(itens(caso)), caso.getValue("totais").jsonObject, 0.01)
        // o dia soma os perfis de todas as refeições sem arredondar antes
        conferir("dia", PlanejadorPorcoes.somar(casos.flatMap { itens(it) }), raiz.getValue("dia").jsonObject, 0.02)
    }

    @Test
    fun saldoDasMetasUsa4Kcal4E9KcalPorGrama() {
        val dia = TotaisPorcoes(1900.0, 200.0, 95.5, 60.0, emptyMap())
        val s = PlanejadorPorcoes.saldo(MetasDieta(1800.0, 50.0, 20.0, 30.0), dia)
        assertEquals(-100.0, s.kcal); assertEquals(25.0, s.carboidrato); assertEquals(-5.5, s.proteina); assertEquals(0.0, s.lipidio)
    }

    @Test
    fun itemSemOMicronutrienteNaoViraZero() {
        val a = OpcaoPorcaoResposta(1, 100.0, 50.0, "g", null, false, nutrientes = mapOf("energia" to 100.0, "ca" to 10.0))
        val b = OpcaoPorcaoResposta(1, 50.0, 50.0, "g", null, false, nutrientes = mapOf("energia" to 50.0))
        val t = PlanejadorPorcoes.somar(listOf(ItemPorcao(a, 2.0), ItemPorcao(b, 1.0)))
        assertEquals(250.0, t.energia); assertEquals(mapOf("ca" to 20.0), t.micronutrientes)
        assertEquals(1.01, PlanejadorPorcoes.arredondar(1.005))
    }
}
