package org.br.dietamais.dominio.calculos

import kotlinx.serialization.json.*
import kotlin.math.abs
import kotlin.test.*

/** Roda `contrato/calculos/adulto.json`, o mesmo arquivo do backend (RN-CAL-06). */
class CalculosAdultoTest {
    private val raiz = Json.parseToJsonElement(
        CalculosAdultoTest::class.java.getResourceAsStream("/contrato/calculos/adulto.json")!!.bufferedReader(Charsets.UTF_8).use { it.readText() },
    ).jsonObject
    private val tolerancia = raiz.getValue("tolerancia").jsonObject.mapValues { it.value.jsonPrimitive.double }

    @Test
    fun todosOsVetoresDoAdultoBatem() {
        val casos = raiz.getValue("casos").jsonArray.map { it.jsonObject }
        assertTrue(casos.size > 50, "esperava os vetores completos")
        val falhas = mutableListOf<String>()
        for (caso in casos) {
            val id = caso.getValue("id").jsonPrimitive.content
            runCatching { rodar(caso) }.onFailure { falhas += "$id: ${it.message}" }
        }
        assertTrue(falhas.isEmpty(), falhas.joinToString("\n"))
    }

    private fun rodar(caso: JsonObject) {
        val e = caso.getValue("entrada").jsonObject
        val esperado = caso.getValue("esperado")
        when (val funcao = caso.getValue("funcao").jsonPrimitive.content) {
            "classificarImcAdulto" -> assertEquals(esperado.jsonPrimitive.content, CalculosAdulto.classificarImcAdulto(e.num("imc")!!).name)
            "classificarImcIdoso" -> assertEquals(esperado.jsonPrimitive.content, CalculosAdulto.classificarImcIdoso(e.num("idadeAnos")!!.toInt(), e.num("imc")!!).name)
            "classificarCintura" -> assertEquals(esperado.jsonPrimitive.content, CalculosAdulto.classificarCintura(e.sexo(), e.num("cinturaCm")!!).name)
            "classificarCompleicao" -> assertEquals(esperado.jsonPrimitive.content, CalculosAdulto.classificarCompleicao(e.sexo(), e.num("estaturaM")!!, e.num("punhoCm")!!).name)
            "classificarPercentualGordura" ->
                assertEquals(esperado.jsonPrimitive.contentOrNull, CalculosAdulto.classificarPercentualGordura(e.sexo(), e.num("idadeAnos")!!.toInt(), e.num("percentualGordura")!!)?.name)
            "calcularIdade" -> {
                val dias = (kotlinx.datetime.LocalDate.parse(e.getValue("dataAvaliacao").jsonPrimitive.content).toEpochDays() -
                    kotlinx.datetime.LocalDate.parse(e.getValue("nascimento").jsonPrimitive.content).toEpochDays())
                assertEquals(esperado.jsonPrimitive.int, CalculosAdulto.idadeEmAnos(dias.toLong()))
            }
            "avaliacaoAdulto" -> conferirAvaliacao(e, esperado.jsonObject)
            else -> fail("função desconhecida: $funcao")
        }
    }

    private fun conferirAvaliacao(e: JsonObject, esperado: JsonObject) {
        val d = e["dobras"]?.jsonObject
        val r = CalculosAdulto.avaliar(
            EntradaAvaliacaoAdulto(
                e.sexo(), e.num("idadeAnos")!!.toInt(), e.num("pesoKg")!!, e.num("estaturaM")!!,
                ProtocoloDobras.valueOf(e.getValue("protocoloDobras").jsonPrimitive.content), ProtocoloMassa.valueOf(e.getValue("protocoloMassa").jsonPrimitive.content),
                DobrasCalculo(d?.num("triceps"), d?.num("peitoral"), d?.num("axilarMedia"), d?.num("subescapular"), d?.num("abdominal"), d?.num("suprailiaca"), d?.num("coxa")),
                e.num("cinturaCm"), e.num("punhoCm"), e.num("diametroPunhoCm"), e.num("diametroJoelhoCm"), e.num("percentualDesejado"),
            ),
        )
        fun numero(campo: String, obtido: Double?, tol: String) {
            val v = esperado[campo]
            if (v == null || v is JsonNull) assertNull(obtido, campo)
            else { assertNotNull(obtido, campo); assertTrue(abs(obtido - v.jsonPrimitive.double) <= tolerancia.getValue(tol) + 1e-9, "$campo: esperado ${v.jsonPrimitive.double}, obtido $obtido") }
        }
        fun texto(campo: String, obtido: String?) = assertEquals(esperado[campo]?.takeIf { it !is JsonNull }?.jsonPrimitive?.content, obtido, campo)
        numero("imc", r.imc, "imc"); texto("classificacaoImc", r.classificacaoImc.name); texto("riscoComorbidades", r.riscoComorbidades.name)
        numero("pesoTeoricoMinKg", r.pesoTeoricoMinKg, "kg"); numero("pesoTeoricoMaxKg", r.pesoTeoricoMaxKg, "kg")
        texto("compleicao", r.compleicao?.name); texto("classificacaoCintura", r.classificacaoCintura?.name)
        numero("somatorioDobrasMm", r.somatorioDobrasMm, "mm"); numero("densidadeCorporal", r.densidadeCorporal, "densidade")
        numero("percentualGordura", r.percentualGordura, "percentual"); texto("classificacaoPercentualGordura", r.classificacaoPercentualGordura?.name)
        numero("percentualMagro", r.percentualMagro, "percentual"); numero("pesoGordoKg", r.pesoGordoKg, "kg"); numero("pesoOsseoKg", r.pesoOsseoKg, "kg")
        numero("pesoResidualKg", r.pesoResidualKg, "kg"); numero("pesoMagroKg", r.pesoMagroKg, "kg"); numero("pesoParaPercentualDesejadoKg", r.pesoParaPercentualDesejadoKg, "kg")
        texto("textoResumo", r.textoResumo)
        assertEquals(esperado.getValue("motivosNulos").jsonObject.mapValues { it.value.jsonPrimitive.content }, r.motivosNulos, "motivosNulos")
    }

    private fun JsonObject.num(campo: String): Double? = this[campo]?.takeIf { it !is JsonNull }?.jsonPrimitive?.double
    private fun JsonObject.sexo() = Sexo.valueOf(getValue("sexo").jsonPrimitive.content.uppercase())

    @Test
    fun arredondamentoMeioParaCimaCobreEmpateBinario() {
        assertEquals(27.15, CalculosAdulto.arredondar(27.145))
        assertEquals(1.0, CalculosAdulto.arredondar(0.995))
    }
}
