package org.br.dietamais.data.local

import org.br.dietamais.data.remoto.ArmazemSessao

internal class ArmazemSessaoSql(private val banco: BancoDieta) : ArmazemSessao {
    override fun salvar(usuarioId: String, perfil: String) { banco.sessaoQueries.salvar(usuarioId, perfil) }

    override fun limpar() = banco.transaction {
        banco.alimentoCacheQueries.limpar()
        banco.registroSyncQueries.limpar()
        banco.pacienteCacheQueries.limpar()
        banco.conflitoQueries.limpar()
        banco.mutacaoPendenteQueries.limpar()
        banco.cursorSyncQueries.limpar()
        banco.sessaoQueries.limpar()
    }
}
