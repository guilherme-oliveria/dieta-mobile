package org.br.dietamais.dominio.rotina

import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import org.br.dietamais.data.remoto.dto.RefeicaoResposta
import org.br.dietamais.data.remoto.dto.TotaisNutricionaisResposta
import kotlin.test.*
import kotlin.time.Instant
import kotlin.uuid.Uuid

class LembretesTest {
    private val zero = TotaisNutricionaisResposta(0.0, 0.0, 0.0, 0.0)
    private fun ref(nome: String, hora: LocalTime) = RefeicaoResposta(Uuid.random(), nome, hora, 0, emptyList(), zero)
    private val cafe = ref("Café", LocalTime(8, 0)); private val almoco = ref("Almoço", LocalTime(12, 30)); private val ceia = ref("Ceia", LocalTime(22, 30))
    private val fuso = TimeZone.of("America/Sao_Paulo")
    private val agora = Instant.parse("2026-10-28T11:00:00Z") // 08:00 em São Paulo (UTC-3)

    @Test fun ignoraPassadoESilencioEOrdena() {
        val l = planejarLembretes(listOf(ceia, almoco, cafe), agora, fuso, dias = 2)
        // hoje: café já passou (08:00 == agora), ceia cai no silêncio 22–07; sobram almoço hoje e amanhã + café amanhã
        assertEquals(listOf("Almoço", "Café", "Almoço"), l.map { it.refeicao })
        assertEquals(l.sortedBy { it.quando }, l)
        assertEquals(Instant.parse("2026-10-28T15:30:00Z"), l.first().quando)
    }

    @Test fun cadaLembreteLevaODiaDaRefeicaoParaOBotaoDaNotificacao() {
        val l = planejarLembretes(listOf(almoco), agora, fuso, dias = 2)
        assertEquals(listOf("2026-10-28", "2026-10-29"), l.map { it.dia.toString() })
        assertTrue(l.all { it.dietaId == null }, "a dieta é preenchida por quem agenda (AgendadorLembretes)")
    }

    @Test fun refeicaoJaFeitaHojeNaoLembraMasAmanhaSim() {
        val l = planejarLembretes(listOf(almoco), agora, fuso, feitasHoje = setOf(almoco.id), dias = 2)
        assertEquals(1, l.size); assertEquals(Instant.parse("2026-10-29T15:30:00Z"), l.single().quando)
    }

    @Test fun silencioDesligadoEnxergaTudoELimiteCorta() {
        val sem = JanelaSilencio(ativo = false)
        assertEquals(3, planejarLembretes(listOf(ceia, almoco, cafe), Instant.parse("2026-10-28T03:30:00Z"), fuso, sem, dias = 1).size)
        assertEquals(64, planejarLembretes(listOf(ceia, almoco, cafe), agora, fuso, sem, dias = 30).size)
        assertEquals(5, planejarLembretes(listOf(ceia, almoco, cafe), agora, fuso, sem, dias = 30, limite = 5).size)
    }

    @Test fun janelaDeSilencioCruzaAMeiaNoite() {
        val j = JanelaSilencio()
        assertTrue(j.contem(LocalTime(23, 0))); assertTrue(j.contem(LocalTime(6, 59))); assertFalse(j.contem(LocalTime(7, 0))); assertFalse(j.contem(LocalTime(12, 0)))
        assertFalse(JanelaSilencio(true, LocalTime(8, 0), LocalTime(8, 0)).contem(LocalTime(8, 0)))
        assertTrue(JanelaSilencio(true, LocalTime(13, 0), LocalTime(15, 0)).contem(LocalTime(14, 0)))
    }
}
