package org.br.dietamais.data.remoto

import kotlin.test.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import org.br.dietamais.data.remoto.dto.*
import kotlin.time.Instant

class AvaliacaoInfantilDtoTest {
    private fun resposta(extra: String = "") = """{
        "id":"00000000-0000-7000-8000-000000000001","pacienteId":"00000000-0000-7000-8000-000000000002",
        "vinculoId":"00000000-0000-7000-8000-000000000003","dataHora":"2026-11-01T12:00:00Z",
        "pesoKg":3.25,"estaturaM":0.485,"imc":13.82,"classificacaoImc":"ABAIXO_DO_PESO",
        "versao":1,"criadoEm":"2026-11-01T12:00:00Z","atualizadoEm":"2026-11-01T12:00:00Z"$extra
    }"""

    @Test fun respostaAnteriorContinuaValida() {
        val av = JsonContrato.decodeFromString<AvaliacaoResposta>(resposta())
        assertNull(av.infantil)
        assertNull(av.resultadosInfantis)
    }

    @Test fun preservaSnapshotIntervalosEValoresDesconhecidos() {
        val av = JsonContrato.decodeFromString<AvaliacaoResposta>(resposta(""",
            "infantil":{"pesoNascerGramas":2500,"modoCurva":"ESCORE_Z","etniaSlaughter":"FUTURA","fatorAtividade":1.2},
            "resultadosInfantis":{"idadeMeses":1,"idadeTexto":"1 mês","modoCurva":"ESCORE_Z",
                "curvas":[{"indicador":"IMC_IDADE","classificacao":"FUTURA","percentil":{"inferior":50,"superior":85},"escoreZ":{"inferior":null,"superior":-3},"cortesPercentis":[10,12,14,16,18,20,22,24],"versaoReferencia":"oms-1.0.0"}],
                "frisancho":[{"indicador":"AMBC","p5":1297,"p10":1264,"motivo":"REFERENCIA_INCONSISTENTE_G16"}],
                "energia":{"getRdiKcal":364.25,"motivosNulos":{"getIomKcal":"IDADE_FORA_DA_FORMULA"}},"novoCampo":true}
        """))
        assertEquals("FUTURA", av.infantil?.etniaSlaughter)
        val r = assertNotNull(av.resultadosInfantis)
        assertEquals(1, r.idadeMeses)
        assertEquals(50.0, r.curvas?.single()?.percentil?.inferior)
        assertEquals(85.0, r.curvas?.single()?.percentil?.superior)
        assertNull(r.curvas?.single()?.escoreZ?.inferior)
        assertEquals("FUTURA", r.curvas?.single()?.classificacao)
        assertEquals("REFERENCIA_INCONSISTENTE_G16", r.frisancho?.single()?.motivo)
        assertEquals(r, JsonContrato.decodeFromString<AvaliacaoResposta>(JsonContrato.encodeToString(av)).resultadosInfantis)
    }

    @Test fun requisicaoEnviaEntradasSemDerivados() {
        val r = SalvarAvaliacaoRequisicao(dataHora = Instant.parse("2026-11-01T12:00:00Z"), pesoKg = 3.25, estaturaM = 0.485,
            infantil = DadosInfantisAvaliacaoDto(pesoNascerGramas = 2500, modoCurva = "ESCORE_Z"))
        val json = JsonContrato.encodeToString(r)
        assertTrue(json.contains("\"pesoNascerGramas\":2500"))
        assertFalse(json.contains("resultadosInfantis"))
        assertEquals(r, JsonContrato.decodeFromString<SalvarAvaliacaoRequisicao>(json))
    }
}
