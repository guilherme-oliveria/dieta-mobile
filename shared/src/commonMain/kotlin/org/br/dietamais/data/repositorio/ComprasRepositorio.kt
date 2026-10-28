package org.br.dietamais.data.repositorio

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.br.dietamais.data.local.BancoDieta
import org.br.dietamais.data.remoto.SessaoAlterada
import kotlin.uuid.Uuid

/** Itens da lista de compras já marcados: ficam só no aparelho, por conta (RN-ROT-04). */
interface PortaCompras {
    fun observarMarcados(): Flow<Set<Uuid>>
    fun marcar(alimentoId: Uuid, marcado: Boolean)
    fun limpar()
}

class ComprasRepositorio(private val banco: BancoDieta) : PortaCompras {
    private val colecao = "compras-marcados"
    override fun observarMarcados(): Flow<Set<Uuid>> = banco.registroSyncQueries.daSessao(colecao).asFlow().mapToList(Dispatchers.Default)
        .map { linhas -> linhas.mapNotNull { runCatching { Uuid.parse(it) }.getOrNull() }.toSet() }

    override fun marcar(alimentoId: Uuid, marcado: Boolean) {
        val s = banco.sessaoQueries.atual().executeAsOneOrNull() ?: throw SessaoAlterada()
        // o id do item é o próprio alimento; o conteúdo guarda o id para a leitura por sessão
        if (marcado) banco.registroSyncQueries.salvar(s.usuario_id, s.perfil, colecao, alimentoId.toString(), 0, 0, alimentoId.toString())
        else banco.registroSyncQueries.remover(s.usuario_id, s.perfil, colecao, alimentoId.toString())
    }

    override fun limpar() {
        val s = banco.sessaoQueries.atual().executeAsOneOrNull() ?: throw SessaoAlterada()
        banco.registroSyncQueries.daSessao(colecao).executeAsList().forEach { banco.registroSyncQueries.remover(s.usuario_id, s.perfil, colecao, it) }
    }
}
