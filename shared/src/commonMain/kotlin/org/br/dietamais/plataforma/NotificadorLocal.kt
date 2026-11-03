package org.br.dietamais.plataforma

import org.br.dietamais.dominio.rotina.Lembrete
import org.br.dietamais.dominio.rotina.TextosLembrete

/** Notificações locais agendadas no aparelho (sem push). Sempre substitui o agendamento anterior. */
interface NotificacaoLocal {
    suspend fun permitido(): Boolean
    suspend fun pedirPermissao(): Boolean
    /** Textos já montados (sem dado clínico); os botões Comi/Pulei registram o check-in e Troquei abre o app (MOB-18b). */
    suspend fun agendar(lembretes: List<Lembrete>, textos: TextosLembrete)
    suspend fun cancelarTudo()
}

expect class NotificadorLocal : NotificacaoLocal {
    override suspend fun permitido(): Boolean
    override suspend fun pedirPermissao(): Boolean
    override suspend fun agendar(lembretes: List<Lembrete>, textos: TextosLembrete)
    override suspend fun cancelarTudo()
}
