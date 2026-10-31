package org.br.dietamais.dominio.calculos

import kotlinx.serialization.json.*
import kotlin.math.abs
import kotlin.test.*

/** Roda `contrato/calculos/energia.json` e `met.json`, os mesmos arquivos do backend (RN-CAL-06). */
class CalculosEnergiaTest {
    private fun ler(nome: String) = Json.parseToJsonElement(
        CalculosEnergiaTest::class.java.getResourceAsStream("/contrato/calculos/$nome")!!.bufferedReader(Charsets.UTF_8).use { it.readText() },
    ).jsonObject

    /** MET das atividades que os vetores usam; a lista completa (113) vem do backend. */
    private val met = mapOf(
        "alpinismo" to 6.9, "waterpolo" to 10.0, "caminhada_5_63_km_h_media" to 3.8, "trilha" to 6.0, "bicicleta_carga_moderada" to 5.5,
        "caminhada_3_2_km_h" to 2.5, "caminhada_3_2_km_h_2" to 2.5,
    )

    @Test
    fun todosOsVetoresDeEnergiaBatem() = conferir(ler("energia.json"), minimo = 60)

    @Test
    fun todosOsVetoresDeMetBatem() = conferir(ler("met.json"), minimo = 8)

    private fun conferir(raiz: JsonObject, minimo: Int) {
        val casos = raiz.getValue("casos").jsonArray.map { it.jsonObject }
        assertTrue(casos.size > minimo, "esperava os vetores completos")
        val falhas = mutableListOf<String>()
        for (caso in casos) {
            val id = caso.getValue("id").jsonPrimitive.content
            runCatching { rodar(caso) }.onFailure { falhas += "$id: ${it.message}" }
        }
        assertTrue(falhas.isEmpty(), falhas.joinToString("\n"))
    }

    private fun rodar(caso: JsonObject) {
        val e = caso.getValue("entrada").jsonObject
        val esp = caso.getValue("esperado")
        when (val funcao = caso.getValue("funcao").jsonPrimitive.content) {
            "gebFao1985" -> num(CalculosEnergia.gebFao1985(e.sexo(), e.int("idadeAnos"), e.d("pesoKg")), esp)
            "gebFao2004" -> num(CalculosEnergia.gebFao2004(e.sexo(), e.int("idadeAnos"), e.d("pesoKg")), esp)
            "gebIom2002" -> num(CalculosEnergia.gebIom2002(e.sexo(), e.int("idadeAnos"), e.d("pesoKg"), e.d("estaturaM"), e.bool("sobrepesoOuObeso")), esp)
            "gebCunningham" -> num(CalculosEnergia.gebCunningham(e.d("massaMagraKg")), esp)
            "getFao1985" -> num(CalculosEnergia.getFao1985(e.d("gebKcal"), e.sexo(), e.nivel()), esp)
            "getFao2004" -> num(CalculosEnergia.getFao2004(e.d("gebKcal"), e.nivel(), e.bool("lactante")), esp)
            "getIom2005" -> num(CalculosEnergia.getIom2005(e.d("gebIomKcal"), e.nivel()), esp)
            "getRdi2005" -> num(
                CalculosEnergia.getRdi2005(e.sexo(), e.int("idadeAnos"), e.d("pesoKg"), e.d("estaturaM"), e.nivel(), e.texto("periodoLactacao")?.let { PeriodoLactacao.valueOf(it) }), esp,
            )
            "vet" -> num(CalculosEnergia.vet(e.d("getKcal"), ObjetivoPeso.valueOf(e.texto("objetivo")!!), e.dn("kgPorMes")), esp)
            "kcalPorKg" -> num(CalculosEnergia.kcalPorKg(e.d("vetKcal"), e.d("pesoKg")), esp)
            "carboidrato" -> meta(
                CalculosEnergia.carboidrato(ProtocoloCarboidrato.valueOf(e.texto("protocolo")!!), e.d("vetKcal"), e.d("pesoKg"), e.bool("gestante"), e.bool("lactante")), esp.jsonObject,
            )
            "proteina" -> meta(
                CalculosEnergia.proteina(ProtocoloProteina.valueOf(e.texto("protocolo")!!), e.d("vetKcal"), e.d("pesoKg"), e.sexo(), e.bool("gestante"), e.bool("lactante")), esp.jsonObject,
            )
            "lipidio" -> meta(CalculosEnergia.lipidio(ProtocoloLipidio.valueOf(e.texto("protocolo")!!), e.d("vetKcal")), esp.jsonObject)
            "lipidiosDislipidemico" -> {
                val l = CalculosEnergia.lipidiosDislipidemico(e.d("vetKcal")); val o = esp.jsonObject
                meta(l.total, o.getValue("total").jsonObject); meta(l.saturado, o.getValue("saturado").jsonObject); meta(l.monoinsaturado, o.getValue("monoinsaturado").jsonObject)
                meta(l.poliinsaturado, o.getValue("poliinsaturado").jsonObject); meta(l.trans, o.getValue("trans").jsonObject)
            }
            "colesterolMaximoMg" -> num(CalculosEnergia.colesterolMaximoMg(e.dn("colesterolTotal")), esp)
            "metDaAtividade" -> num(met.getValue(e.texto("codigo")!!), esp)
            "getPorMet" -> {
                val r = CalculosEnergia.getPorMet(
                    EntradaGetPorMet(
                        e.sexo(), e.d("pesoKg"), e.d("massaMagraKg"), e.d("horasSono"), e.d("horasTrabalho"), NivelAtividade.valueOf(e.texto("intensidadeTrabalho")!!),
                        e.getValue("atividades").jsonArray.map { it.jsonObject }.map { AtividadeDia(met.getValue(it.texto("codigo")!!), it.d("minutos")) },
                    ),
                )
                val o = esp.jsonObject
                num(r.gebDia, o.getValue("gebDia")); num(r.gebHora, o.getValue("gebHora")); num(r.gastoSono, o.getValue("gastoSono")); num(r.gastoTrabalho, o.getValue("gastoTrabalho"))
                num(r.horasRotina, o.getValue("horasRotina")); num(r.gastoRotina, o.getValue("gastoRotina")); num(r.get, o.getValue("get"))
                val gastos = o.getValue("gastoAtividades").jsonArray
                assertEquals(gastos.size, r.gastoAtividades.size, "gastoAtividades")
                gastos.forEachIndexed { i, v -> num(r.gastoAtividades[i], v) }
            }
            else -> fail("função desconhecida: $funcao")
        }
    }

    private fun num(obtido: Double, esperado: JsonElement) =
        assertTrue(abs(obtido - esperado.jsonPrimitive.double) <= 0.01 + 1e-9, "esperado ${esperado.jsonPrimitive.double}, obtido $obtido")

    private fun meta(m: MetaMacro, esp: JsonObject) {
        fun campo(nome: String, obtido: Double?) {
            val v = esp[nome]
            if (v == null || v is JsonNull) assertNull(obtido, nome) else { assertNotNull(obtido, nome); num(obtido, v) }
        }
        campo("percentualMinimo", m.percentualMinimo); campo("percentualMaximo", m.percentualMaximo)
        campo("gramasMinimo", m.gramasMinimo); campo("gramasMaximo", m.gramasMaximo)
    }

    private fun JsonObject.d(campo: String) = getValue(campo).jsonPrimitive.double
    private fun JsonObject.dn(campo: String): Double? = this[campo]?.takeIf { it !is JsonNull }?.jsonPrimitive?.double
    private fun JsonObject.int(campo: String) = getValue(campo).jsonPrimitive.int
    private fun JsonObject.bool(campo: String) = getValue(campo).jsonPrimitive.boolean
    private fun JsonObject.texto(campo: String): String? = this[campo]?.takeIf { it !is JsonNull }?.jsonPrimitive?.content
    private fun JsonObject.sexo() = Sexo.valueOf(getValue("sexo").jsonPrimitive.content.uppercase())
    private fun JsonObject.nivel() = NivelAtividade.valueOf(getValue("nivel").jsonPrimitive.content)

    @Test fun idadeAbaixoDe18Recusa() { assertFailsWith<IllegalArgumentException> { CalculosEnergia.gebFao1985(Sexo.MASCULINO, 17, 70.0) } }

    @Test fun diaComMaisDe24HorasRecusa() {
        assertFailsWith<IllegalArgumentException> {
            CalculosEnergia.getPorMet(EntradaGetPorMet(Sexo.MASCULINO, 80.0, 60.0, 10.0, 10.0, NivelAtividade.LEVE, listOf(AtividadeDia(6.0, 300.0))))
        }
    }
}
