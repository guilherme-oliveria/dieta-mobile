package org.br.dietamais.dominio.dieta

import kotlin.test.*
import kotlin.uuid.Uuid

class AjusteListaTest {
    private val a = Uuid.parse("019a0f32-5000-7000-8000-00000000000a")
    private val b = Uuid.parse("019a0f32-5000-7000-8000-00000000000b")

    @Test fun removerDaBaseVaiParaRemovidos() = assertEquals(setOf(a), AjusteLista().remover(a).removidos)
    @Test fun adicionarForaDaBaseVaiParaAdicionados() = assertEquals(setOf(a), AjusteLista().adicionar(a).adicionados)
    @Test fun desfazerCancelaOAjuste() {
        assertTrue(AjusteLista().remover(a).adicionar(a).vazio)
        assertTrue(AjusteLista().adicionar(b).remover(b).vazio)
    }
    @Test fun ajustesIndependentes() {
        val r = AjusteLista().remover(a).adicionar(b)
        assertEquals(setOf(a), r.removidos); assertEquals(setOf(b), r.adicionados)
    }
}
