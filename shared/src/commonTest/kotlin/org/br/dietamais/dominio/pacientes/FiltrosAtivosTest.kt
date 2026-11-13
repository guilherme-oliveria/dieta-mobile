package org.br.dietamais.dominio.pacientes

import org.br.dietamais.data.remoto.dto.StatusVinculo
import kotlin.test.*

class FiltrosAtivosTest {
    @Test fun contaSituacaoEAtencaoUmACadaESemNadaDaZero() {
        assertEquals(0, contarFiltrosAtivos(null, null))
        assertEquals(1, contarFiltrosAtivos(StatusVinculo.ATIVO, null))
        assertEquals(1, contarFiltrosAtivos(null, FiltroAtencao.SEM_RETORNO))
        assertEquals(2, contarFiltrosAtivos(StatusVinculo.ENCERRADO, FiltroAtencao.RETORNO_HOJE))
    }

    @Test fun filtrandoQuandoHaBuscaOuFiltro() {
        assertFalse(estaFiltrando("", null, null)); assertFalse(estaFiltrando("   ", null, null))
        assertTrue(estaFiltrando("ana", null, null)); assertTrue(estaFiltrando("", StatusVinculo.ATIVO, null)); assertTrue(estaFiltrando("", null, FiltroAtencao.SEM_RETORNO))
    }
}
