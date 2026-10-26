package org.br.dietamais.dominio.convite

import org.br.dietamais.data.remoto.dto.TermosUsoResposta
import org.br.dietamais.dominio.contas.permiteCadastro
import kotlin.test.*
import kotlin.time.Instant

class TermosUsoTest {
    private val publicado = TermosUsoResposta("TESTE", "PUBLICADO", true, true, true,
        Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-02T00:00:00Z"), "Texto fictício apenas de teste.", "/legal/terms-of-use")
    @Test fun exigePublicacaoEAprovacaoComMetadadosCoerentes() {
        assertTrue(publicado.permiteCadastro())
        listOf(publicado.copy(status = "RASCUNHO"), publicado.copy(status = "APROVADO"), publicado.copy(aprovado = false),
            publicado.copy(publicado = false), publicado.copy(cadastroHabilitado = false), publicado.copy(aprovadoEm = null),
            publicado.copy(publicadoEm = null), publicado.copy(versao = ""), publicado.copy(conteudo = ""),
            publicado.copy(aprovadoEm = Instant.parse("2026-02-01T00:00:00Z")), publicado.copy(status = "FUTURO")).forEach {
            assertFalse(it.permiteCadastro())
        }
    }
}
