package org.br.dietamais.dominio.avaliacao

import kotlinx.datetime.LocalDate
import org.br.dietamais.ui.nutri.avaliacao.EstadoAvaliacoes
import org.br.dietamais.ui.nutri.avaliacao.FormularioAvaliacao
import org.br.dietamais.ui.nutri.avaliacao.previaDe
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

/** Executa no app web: a JVM já fornece a base IANA e não reproduz a falha do Wasm. */
class FusosAvaliacaoWebTest {
    @Test fun aniversarioUsaDiaDeSaoPauloMesmoQuandoUtcJaVirou() {
        val instante = Instant.parse("2026-11-02T02:30:00Z")
        assertEquals(119, idadeAvaliacaoMeses(LocalDate(2016, 11, 2), instante))
        assertTrue(ehAvaliacaoInfantil(LocalDate(2008, 11, 2), instante))
        assertFalse(ehAvaliacaoInfantil(LocalDate(2008, 11, 1), instante))
    }

    @Test fun avaliacaoHistoricaRespeitaHorarioDeVeraoDeSaoPaulo() {
        // Em dezembro de 2018 São Paulo usava UTC-2: já é 02/12, não 01/12.
        assertEquals(120, idadeAvaliacaoMeses(LocalDate(2008, 12, 2), Instant.parse("2018-12-02T02:30:00Z")))
    }

    @Test fun previaAdultaTambemResolveFusoPadraoNoNavegador() {
        val estado = EstadoAvaliacoes(nascimento = LocalDate(1986, 11, 2))
        val formulario = FormularioAvaliacao(valores = mapOf(MedidaAvaliacao.PESO to "70", MedidaAvaliacao.ESTATURA to "1.70"))
        val previa = previaDe(estado, formulario, Instant.parse("2026-11-02T02:30:00Z"))
        assertEquals(39, previa.idadeAnos)
    }
}
