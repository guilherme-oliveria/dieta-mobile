package org.br.dietamais.dominio.avaliacao

import kotlinx.datetime.LocalDate
import org.br.dietamais.data.remoto.dto.EnergiaAvaliacaoDto
import org.br.dietamais.data.remoto.dto.Sexo
import org.br.dietamais.dominio.calculos.*
import kotlin.test.*
import kotlin.time.Instant

class NecessidadesTest {
    private val valores = mapOf(MedidaAvaliacao.PESO to "102,5", MedidaAvaliacao.ESTATURA to "1,93")
    private fun previa(sexo: Sexo?, e: EscolhasEnergia, nascimento: LocalDate? = LocalDate(1997, 4, 1)): PreviaEnergia {
        val dia = LocalDate(2026, 10, 31)
        val base = previaAvaliacao(valores, sexo, nascimento, dia)
        return previaEnergia(valores, sexo, base.idadeAnos, base, e)
    }

    @Test fun fao2004ModeradoBateComOBackend() {
        val p = previa(Sexo.MASCULINO, EscolhasEnergia(nivel = NivelAtividade.MODERADO))
        assertEquals(2235.54, p.gebKcal); assertEquals(3934.55, p.getKcal); assertEquals(3934.55, p.vetKcal); assertEquals(38.39, p.kcalPorKg)
        assertEquals(45.0, p.carboidrato?.percentualMinimo); assertEquals(65.0, p.carboidrato?.percentualMaximo)
        assertEquals(20.0, p.lipidio?.percentualMinimo); assertTrue(p.faltas.isEmpty()); assertNull(p.lipidios)
    }

    @Test fun fao1985EOutrosMetodosMudamAFormula() {
        assertEquals(2247.25, previa(Sexo.MASCULINO, EscolhasEnergia(metodo = MetodoEnergia.FAO_1985, nivel = NivelAtividade.LEVE)).gebKcal)
        val rdi = previa(Sexo.MASCULINO, EscolhasEnergia(metodo = MetodoEnergia.RDI_2005, nivel = NivelAtividade.LEVE))
        assertNull(rdi.gebKcal); assertNotNull(rdi.getKcal)
    }

    @Test fun semNivelDaOGebEPedeONivel() {
        val p = previa(Sexo.MASCULINO, EscolhasEnergia())
        assertNotNull(p.gebKcal); assertNull(p.getKcal); assertNull(p.vetKcal); assertNull(p.carboidrato)
        assertEquals(listOf(FaltaEnergia.NIVEL_ATIVIDADE), p.faltas)
    }

    @Test fun fichaIncompletaOuCriancaNaoCalcula() {
        assertEquals(listOf(FaltaEnergia.SEXO), previa(null, EscolhasEnergia(nivel = NivelAtividade.LEVE)).faltas)
        assertEquals(listOf(FaltaEnergia.NASCIMENTO), previa(Sexo.FEMININO, EscolhasEnergia(), nascimento = null).faltas)
        assertEquals(listOf(FaltaEnergia.FAIXA_ETARIA), previa(Sexo.FEMININO, EscolhasEnergia(), nascimento = LocalDate(2015, 1, 1)).faltas)
        assertEquals(listOf(FaltaEnergia.PESO_ESTATURA), previaEnergia(emptyMap(), Sexo.MASCULINO, 30, PreviaAvaliacao(null, 30, emptyList()), EscolhasEnergia()).faltas)
        assertEquals(listOf(FaltaEnergia.METODO_MET), previa(Sexo.MASCULINO, EscolhasEnergia(metodo = MetodoEnergia.MET)).faltas)
    }

    @Test fun lactanteSomaNoFao2004EObjetivoMudaOVet() {
        val comum = previa(Sexo.FEMININO, EscolhasEnergia(nivel = NivelAtividade.LEVE))
        val lactante = previa(Sexo.FEMININO, EscolhasEnergia(nivel = NivelAtividade.LEVE, condicao = CondicaoEnergia.LACTANTE_ATE_6_MESES))
        assertEquals(675.0, lactante.getKcal!! - comum.getKcal!!, 0.011)
        val perda = previa(Sexo.MASCULINO, EscolhasEnergia(nivel = NivelAtividade.MODERADO, objetivo = ObjetivoPeso.PERDA, kgPorMes = "2"))
        assertTrue(perda.vetKcal!! < perda.getKcal!!)
    }

    @Test fun dislipidemicoTrazLipidiosEColesterol() {
        val p = previa(Sexo.MASCULINO, EscolhasEnergia(nivel = NivelAtividade.LEVE, dislipidemico = true, colesterolTotal = 230.0))
        assertEquals(7.0, p.lipidios?.saturado?.percentualMinimo); assertEquals(200.0, p.colesterolMaximoMg)
    }

    @Test fun sobeParaOServidorSoQuandoANutriEscolheuAlgo() {
        assertNull(energiaParaEnviar(EscolhasEnergia()))
        val dto = energiaParaEnviar(EscolhasEnergia(tocada = true, carboidrato = ProtocoloCarboidrato.POR_KG, condicao = CondicaoEnergia.LACTANTE_7_A_12_MESES))!!
        assertEquals("FAO_2004", dto.metodo); assertEquals("POR_KG", dto.carboidrato); assertEquals("DE_7_A_12_MESES", dto.lactacao); assertNull(dto.gestante)
        val req = montarRequisicao(valores, Instant.parse("2026-10-31T12:00:00Z"), "", true, energia = EscolhasEnergia(nivel = NivelAtividade.MODERADO, objetivo = ObjetivoPeso.PERDA, kgPorMes = "1,5"))
        assertEquals("MODERADO", req.complementares?.nivelAtividade); assertEquals("PERDA", req.complementares?.objetivo); assertEquals(1.5, req.complementares?.objetivoKgPorMes)
        assertNotNull(req.energia)
        assertNull(montarRequisicao(valores, Instant.parse("2026-10-31T12:00:00Z"), "", true).energia)
    }

    @Test fun leAsEscolhasGravadasEIgnoraValorDesconhecido() {
        val e = escolhasDe(EnergiaAvaliacaoDto(metodo = "IOM_2005", carboidrato = "NOVO_PROTOCOLO", lactacao = "DE_7_A_12_MESES", dislipidemico = true), "LEVE", "GANHO", 0.5)
        assertEquals(MetodoEnergia.IOM_2005, e.metodo); assertEquals(ProtocoloCarboidrato.FAIXA_DRI, e.carboidrato); assertEquals(CondicaoEnergia.LACTANTE_7_A_12_MESES, e.condicao)
        assertEquals(NivelAtividade.LEVE, e.nivel); assertEquals(ObjetivoPeso.GANHO, e.objetivo); assertEquals("0,5", e.kgPorMes); assertTrue(e.dislipidemico)
    }
}
