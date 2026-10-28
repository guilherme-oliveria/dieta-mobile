package org.br.dietamais.plataforma

import org.br.dietamais.dominio.rotina.Lembrete

/** Notificações locais agendadas no aparelho (sem push). Sempre substitui o agendamento anterior. */
interface NotificacaoLocal {
    suspend fun permitido(): Boolean
    suspend fun pedirPermissao(): Boolean
    /** [titulo] e [corpo] já vêm montados (sem dado clínico); [corpo] recebe o nome da refeição. */
    suspend fun agendar(lembretes: List<Lembrete>, titulo: String, corpo: (String) -> String)
    suspend fun cancelarTudo()
}

expect class NotificadorLocal : NotificacaoLocal {
    override suspend fun permitido(): Boolean
    override suspend fun pedirPermissao(): Boolean
    override suspend fun agendar(lembretes: List<Lembrete>, titulo: String, corpo: (String) -> String)
    override suspend fun cancelarTudo()
}
