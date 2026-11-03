package org.br.dietamais.dominio.avaliacao

import org.br.dietamais.data.remoto.dto.*
import kotlin.test.*
import kotlin.time.Instant
import kotlin.uuid.Uuid

class MeusResultadosTest {
    private fun av(data: String, peso: Double, compartilhada: Boolean = true, r: ResultadosAvaliacaoDto? = null, alvo: Double? = null, massa: String? = null) = AvaliacaoResposta(
        Uuid.random(), Uuid.random(), Uuid.random(), Instant.parse(data), peso, 1.7, imcDe(peso, 1.7), classificarImc(imcDe(peso, 1.7)),
        compartilhada = compartilhada, versao = 1, criadoEm = Instant.parse(data), atualizadoEm = Instant.parse(data),
        protocolo = massa?.let { ProtocoloAvaliacaoDto(massa = it) }, complementares = alvo?.let { MedidasComplementaresDto(percentualGorduraDesejado = it) }, resultados = r)

    @Test fun semAvaliacaoCompartilhadaNaoHaResumo() {
        assertNull(meusResultados(emptyList()))
        assertNull(meusResultados(listOf(av("2026-10-01T00:00:00Z", 70.0, compartilhada = false))))
    }

    @Test fun semResultadosDoServidorUsaAClassificacaoDoImc() {
        val m = meusResultados(listOf(av("2026-10-01T00:00:00Z", 70.0)))!!
        assertEquals(listOf(TipoIndicador.PESO_ALTURA), m.indicadores.map { it.tipo })
        assertEquals(Semaforo.BOM, m.indicadores[0].semaforo)
        assertTrue(m.composicao.isEmpty()); assertNull(m.meta)
    }

    @Test fun usaAUltimaAvaliacaoCompartilhadaComSemaforoEMeta() {
        val r = ResultadosAvaliacaoDto(classificacaoImc = "SP", classificacaoCintura = "A", classificacaoPercentualGordura = "ALTO", riscoComorbidades = "MO",
            percentualGordura = 26.48, pesoGordoKg = 24.0, pesoMagroKg = 67.9, pesoParaPercentualDesejadoKg = 91.9, pesoTeoricoMinKg = 60.0, pesoTeoricoMaxKg = 80.0, textoResumo = "Bom caminho")
        val m = meusResultados(listOf(av("2026-09-01T00:00:00Z", 95.0), av("2026-10-01T00:00:00Z", 100.0, r = r, alvo = 18.0), av("2026-10-10T00:00:00Z", 99.0, compartilhada = false)))!!
        assertEquals(100.0, m.pesoKg)
        assertEquals(listOf(Semaforo.ATENCAO, Semaforo.ATENCAO, Semaforo.ALERTA, Semaforo.ALERTA), m.indicadores.map { it.semaforo })
        assertEquals(MetaPeso(18.0, 91.9, 8.1), m.meta)
        assertEquals(60.0..80.0, m.pesoSaudavel)
        assertEquals(listOf(ParteCorpo.GORDURA, ParteCorpo.MASSA_MAGRA), m.composicao.map { it.first })
        assertEquals(listOf(0.0 to 95.0, 30.0 to 100.0), m.seriePeso)
        assertEquals("Bom caminho", m.textoResumo)
    }

    @Test fun massaMuscularDivideEmQuatroPartes() {
        val r = ResultadosAvaliacaoDto(pesoGordoKg = 20.0, pesoMagroKg = 30.0, pesoOsseoKg = 10.0, pesoResidualKg = 20.0)
        val m = meusResultados(listOf(av("2026-10-01T00:00:00Z", 80.0, r = r, massa = "MUSCULAR")))!!
        assertEquals(listOf(ParteCorpo.GORDURA, ParteCorpo.MUSCULO, ParteCorpo.OSSO, ParteCorpo.RESIDUAL), m.composicao.map { it.first })
    }

    @Test fun codigoDesconhecidoSomeEmVezDeQuebrar() {
        val r = ResultadosAvaliacaoDto(classificacaoCintura = "XYZ", classificacaoPercentualGordura = "NOVO", riscoComorbidades = "?")
        val m = meusResultados(listOf(av("2026-10-01T00:00:00Z", 70.0, r = r)))!!
        assertEquals(listOf(TipoIndicador.PESO_ALTURA), m.indicadores.map { it.tipo })
    }
}
