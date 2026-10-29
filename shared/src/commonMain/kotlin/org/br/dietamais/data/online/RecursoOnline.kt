package org.br.dietamais.data.online

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.br.dietamais.data.remoto.ContextoSessao
import org.br.dietamais.data.remoto.SessaoAlterada
import kotlin.uuid.Uuid

/**
 * Base das Portas da web (web.md): a lista vem da API por `GET` e fica só em memória; o servidor continua sendo a única
 * fonte de verdade. Escritas devolvem o registro que o servidor gravou, que entra na lista sem novo `GET`.
 * Erros (inclusive `409 CONFLITO_VERSAO`) sobem como [org.br.dietamais.data.remoto.ErroApi] para a tela tratar pelo `codigo`.
 *
 * A lista é de quem a carregou: se a sessão acabar ou trocar de usuário, [fluxo] volta a ser vazio na hora, então nunca
 * aparece dado da conta anterior, nem por um instante.
 */
class RecursoOnline<T>(private val sessao: StateFlow<ContextoSessao?>, private val carregar: suspend () -> List<T>) {
    private class Dados<T>(val dono: Uuid, val itens: List<T>)
    private val dados = MutableStateFlow<Dados<T>?>(null)
    private val trava = Mutex()

    val fluxo: Flow<List<T>> = combine(sessao, dados) { atual, guardado ->
        if (atual != null && guardado?.dono == atual.usuarioId) guardado.itens else emptyList()
    }

    suspend fun atualizar() = trava.withLock {
        val dono = (sessao.value ?: throw SessaoAlterada()).usuarioId
        val itens = carregar()
        if (sessao.value?.usuarioId != dono) throw SessaoAlterada()
        dados.value = Dados(dono, itens)
    }

    /** Coloca [novo] na lista, trocando o item que [mesmo] reconhecer como o mesmo registro. */
    fun gravado(novo: T, mesmo: (T) -> Boolean) {
        val dono = sessao.value?.usuarioId ?: return
        val atual = dados.value?.takeIf { it.dono == dono }?.itens.orEmpty()
        dados.value = Dados(dono, if (atual.any(mesmo)) atual.map { if (mesmo(it)) novo else it } else atual + novo)
    }
}
