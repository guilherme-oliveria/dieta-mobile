package org.br.dietamais.dominio.avaliacao

import kotlin.test.*
import kotlin.time.Instant
import kotlin.uuid.Uuid
import org.br.dietamais.data.remoto.dto.*

class CrescimentoTest {
    private val paciente = Uuid.random()
    private val vinculo = Uuid.random()
    private fun avaliacao(mes: Int, referencia: Int = mes, sexo: String = "masculino", versao: String = "oms-1", cortes: List<Double>? = listOf(1.0,2.0,3.0,4.0,5.0,6.0,7.0,8.0)) : AvaliacaoResposta {
        val data = Instant.parse("2026-01-01T12:00:00Z") + kotlin.time.Duration.parse("${mes}h")
        return AvaliacaoResposta(Uuid.random(), paciente, vinculo, data, 10.0, 0.8, 15.625, ClassificacaoImc.NORMAL,
            versao = 1, criadoEm = data, atualizadoEm = data, resultadosInfantis = ResultadosInfantisAvaliacaoDto(
                idadeMeses = mes, sexoReferencia = sexo, curvas = listOf(CurvaCrescimentoAvaliacaoDto(
                    indicador = "IMC_IDADE", valor = 15.625, unidade = "kg/m²", idadeMesesReferencia = referencia,
                    cortesPercentis = cortes, fonte = "OMS", versaoReferencia = versao, percentil = IntervaloCurvaDto(50.0,85.0)))))
    }

    @Test fun conservaIdadeRealDoPontoEIdadeAnualDaReferencia() {
        val a = avaliacao(131,120)
        val serie = crescimento(listOf(a), a.id, "IMC_IDADE")!!
        assertEquals(131, serie.pontos.single().idadeMeses)
        assertEquals(120, serie.referencias.single().idadeMeses)
        assertEquals(IntervaloCurvaDto(50.0,85.0), serie.selecionado.curva.percentil)
    }

    @Test fun naoMisturaPacienteVinculoSexoOuVersaoENaoReclassifica() {
        val a = avaliacao(18)
        val mesma = avaliacao(19)
        val outras = listOf(avaliacao(20,sexo="feminino"), avaliacao(21,versao="oms-2"),
            avaliacao(22).copy(pacienteId=Uuid.random()), avaliacao(23).copy(vinculoId=Uuid.random()))
        val serie = crescimento(outras + mesma + a, a.id, "IMC_IDADE")!!
        assertEquals(listOf(18,19), serie.pontos.map { it.idadeMeses })
        assertEquals(2, serie.referencias.size)
        assertNull(serie.selecionado.curva.classificacao)
    }

    @Test fun referenciaInvalidaNaoViraCurvaMasMedidaConfirmadaContinuaDisponivel() {
        val a = avaliacao(18,cortes=listOf(1.0,3.0,2.0))
        val serie = crescimento(listOf(a),a.id,"IMC_IDADE")!!
        assertTrue(serie.referencias.isEmpty())
        assertEquals(15.625,serie.pontos.single().valor)
        assertNull(crescimento(listOf(a.copy(resultadosInfantis=null)),a.id,"IMC_IDADE"))
        assertNull(crescimento(listOf(a),a.id,"FUTURO"))
    }

    @Test fun duplicatasDeMesComCortesDivergentesNaoInventamReferenciaUnica() {
        val a=avaliacao(18)
        val b=avaliacao(19,18,cortes=(1..8).map { it.toDouble()+0.1 })
        val serie=crescimento(listOf(a,b),a.id,"IMC_IDADE")!!
        assertEquals(2,serie.pontos.size)
        assertTrue(serie.referencias.isEmpty())
    }
}
