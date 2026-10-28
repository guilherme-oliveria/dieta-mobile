package org.br.dietamais.dominio.rotina

import kotlinx.datetime.LocalTime
import org.br.dietamais.data.remoto.dto.*
import kotlin.test.*
import kotlin.time.Instant
import kotlin.uuid.Uuid

class DietaVigenteTest {
    private val zero = TotaisNutricionaisResposta(0.0, 0.0, 0.0, 0.0)
    private val nutri = NutricionistaResumoResposta(Uuid.random(), "Ana", "CRN-1")
    private fun snap(dietaId: Uuid, publicado: String, refeicoes: List<RefeicaoResposta> = emptyList()) = DietaSnapshotResposta(Uuid.random(), dietaId,
        Uuid.random(), Uuid.random(), nutri, "Dieta", null, refeicoes, ListaSubstituicaoResposta(Uuid.random(), 0, emptyList(), emptyList(), emptyList()), zero, Instant.parse(publicado))
    private fun ref(hora: String, ordem: Int = 0) = RefeicaoResposta(Uuid.random(), "R$hora", LocalTime.parse(hora), ordem, emptyList(), zero)

    @Test fun usaAEscolhidaOuAMaisRecente() {
        val a = snap(Uuid.random(), "2026-01-01T10:00:00Z"); val b = snap(Uuid.random(), "2026-02-01T10:00:00Z")
        assertEquals(b, dietaVigente(listOf(a, b), null))
        assertEquals(a, dietaVigente(listOf(a, b), a.dietaId))
        assertEquals(b, dietaVigente(listOf(a, b), Uuid.random()))
        assertNull(dietaVigente(emptyList(), null))
    }

    @Test fun proximaRefeicaoIgnoraAsQueJaPassaram() {
        val lista = listOf(ref("12:00"), ref("08:00"), ref("19:30"))
        assertEquals(LocalTime(12, 0), proximaRefeicao(lista, LocalTime(9, 0))?.horario)
        assertEquals(LocalTime(12, 0), proximaRefeicao(lista, LocalTime(12, 0))?.horario)
        assertNull(proximaRefeicao(lista, LocalTime(20, 0)))
        assertEquals(listOf("08:00", "12:00", "19:30"), refeicoesOrdenadas(lista).map { it.horario.toString() })
    }
}
