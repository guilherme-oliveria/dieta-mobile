package org.br.dietamais.ui.nutri.avaliacao

import org.br.dietamais.data.remoto.JsonContrato
import org.br.dietamais.data.remoto.dto.AvaliacaoResposta
import org.br.dietamais.data.remoto.dto.FaixaEtariaAvaliacao
import org.br.dietamais.dominio.avaliacao.MedidaAvaliacao
import org.br.dietamais.dominio.avaliacao.SecaoAvaliacao
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** MOB-49: o app lê a avaliação ampliada do BE-41 (campos novos opcionais, valores desconhecidos não derrubam) e mostra as seções preenchidas. */
class DetalhesAvaliacaoTest {
    private val base = """{"id":"0192f6c0-0000-7000-8000-000000000001","pacienteId":"0192f6c0-0000-7000-8000-000000000002","vinculoId":"0192f6c0-0000-7000-8000-000000000003",
        "dataHora":"2026-10-01T12:00:00Z","pesoKg":102.5,"estaturaM":1.93,"imc":27.52,"classificacaoImc":"SOBREPESO","versao":1,
        "criadoEm":"2026-10-01T12:00:00Z","atualizadoEm":"2026-10-01T12:00:00Z","perimetros":{"cintura":100},
        "dobras":{"triceps":20,"abdominal":40},"parecer":"Boa evolução"%EXTRA%}"""

    private fun ler(extra: String = "") = JsonContrato.decodeFromString<AvaliacaoResposta>(base.replace("%EXTRA%", extra))

    @Test fun contratoAntigoSemCamposNovosContinuaValendo() {
        val a = ler()
        assertNull(a.resultados); assertNull(a.faixaEtaria); assertNull(a.compartilhamento)
    }

    @Test fun leResultadosEFaixaDoBackendNovo() {
        val a = ler(""","faixaEtaria":"ADULTO","protocolo":{"dobras":"SETE","massa":"MAGRO","versaoReferencia":"adulto-1.0.0"},
            "resultados":{"idadeAnos":29,"classificacaoImc":"SP","percentualGordura":26.48,"somatorioDobrasMm":199,"motivosNulos":{"pesoOsseoKg":"FALTA_DIAMETRO_PUNHO"}},
            "compartilhamento":{"composicao":false}""")
        assertEquals(FaixaEtariaAvaliacao.ADULTO, a.faixaEtaria)
        assertEquals(26.48, a.resultados?.percentualGordura)
        assertEquals("FALTA_DIAMETRO_PUNHO", a.resultados?.motivosNulos?.get("pesoOsseoKg"))
        assertEquals(false, a.compartilhamento?.composicao)
    }

    @Test fun valorDesconhecidoEMotivoNovoNaoFalham() {
        val a = ler(""","faixaEtaria":"GESTANTE","futuro":1,"resultados":{"classificacaoImc":"XYZ","campoNovo":true}""")
        assertEquals(FaixaEtariaAvaliacao.OUTRO, a.faixaEtaria)
        assertEquals("XYZ", a.resultados?.classificacaoImc)
    }

    @Test fun secoesComMedidasListaSoOQueFoiPreenchido() {
        val secoes = secoesComMedidas(ler())
        assertEquals(listOf(SecaoAvaliacao.PERIMETROS, SecaoAvaliacao.DOBRAS), secoes.map { it.first })
        assertEquals(listOf(MedidaAvaliacao.CINTURA to 100.0), secoes[0].second)
        assertTrue(secoes[1].second.any { it.first == MedidaAvaliacao.DOBRA_ABDOMINAL && it.second == 40.0 })
    }
}
