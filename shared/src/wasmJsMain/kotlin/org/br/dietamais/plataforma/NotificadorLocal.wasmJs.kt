package org.br.dietamais.plataforma

import org.br.dietamais.dominio.rotina.Lembrete

/** Web não agenda lembretes; a tela mostra "instale o app para receber lembretes". */
actual class NotificadorLocal : NotificacaoLocal {
    actual override suspend fun permitido(): Boolean = false
    actual override suspend fun pedirPermissao(): Boolean = false
    actual override suspend fun agendar(lembretes: List<Lembrete>, titulo: String, corpo: (String) -> String) = Unit
    actual override suspend fun cancelarTudo() = Unit
}
