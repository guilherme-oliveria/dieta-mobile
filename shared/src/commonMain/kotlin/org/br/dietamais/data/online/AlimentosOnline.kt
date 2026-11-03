package org.br.dietamais.data.online

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.json.encodeToJsonElement
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.CatalogoAlimentos
import org.br.dietamais.data.repositorio.PortaAlimentos
import kotlin.uuid.Uuid

/** Catálogo de alimentos da nutri direto da API (web). Grupos e alimentos ficam em memória até o próximo `atualizar()`. */
class AlimentosOnline(private val api: ApiDieta) : PortaAlimentos {
    private val grupos = RecursoOnline(api.sessao) { carregarGrupos() }
    private val alimentos = RecursoOnline(api.sessao) { carregarAlimentos() }

    override fun observar(): Flow<CatalogoAlimentos> = combine(grupos.fluxo, alimentos.fluxo) { g, a ->
        CatalogoAlimentos(g.sortedBy { it.nome.lowercase() }, a.sortedBy { it.nome.lowercase() })
    }

    override suspend fun atualizar() {
        contexto()
        grupos.atualizar()
        alimentos.atualizar()
    }

    override suspend fun criar(requisicao: CriarAlimentoRequisicao, chave: Uuid): AlimentoResposta {
        val esperado = contexto()
        val criado = api.enviar<AlimentoResposta>("alimentos", JsonContrato.encodeToJsonElement(requisicao), chave)
        conferir(esperado)
        alimentos.gravado(criado) { it.id == criado.id }
        return criado
    }

    override suspend fun alterar(id: Uuid, versao: Long, requisicao: AtualizarAlimentoRequisicao): AlimentoResposta {
        val esperado = contexto()
        val alterado = api.alterar<AlimentoResposta>("alimentos/$id", JsonContrato.encodeToJsonElement(requisicao), versao)
        conferir(esperado)
        if (alterado.id != id) throw RespostaInvalida()
        alimentos.gravado(alterado) { it.id == id }
        return alterado
    }

    private suspend fun carregarGrupos(): List<GrupoAlimentoResposta> {
        val esperado = contexto()
        val itens = api.obter<GruposAlimentosResposta>("grupos-alimentos").itens
        conferir(esperado)
        if (itens.map { it.id }.distinct().size != itens.size) throw RespostaInvalida()
        return itens
    }

    private suspend fun carregarAlimentos(): List<AlimentoResposta> {
        val esperado = contexto()
        val todos = mutableListOf<AlimentoResposta>()
        for (ativo in listOf(true, false)) {
            val doEstado = mutableListOf<AlimentoResposta>()
            var pagina = 0
            do {
                val resposta = api.obter<PaginaAlimentosResposta>("alimentos", mapOf("pagina" to pagina.toString(), "tamanho" to "20", "ativo" to ativo.toString()))
                conferir(esperado)
                if (resposta.pagina != pagina || resposta.total < 0 || (resposta.itens.isEmpty() && doEstado.size < resposta.total)) throw RespostaInvalida()
                doEstado += resposta.itens
                pagina++
            } while (doEstado.size < resposta.total)
            todos += doEstado
        }
        if (todos.map { it.id }.distinct().size != todos.size) throw RespostaInvalida()
        return todos
    }

    private fun contexto() = api.operacaoSessao.value?.takeIf { it.contexto.perfil == Perfil.NUTRICIONISTA } ?: throw SessaoAlterada()
    private fun conferir(esperado: OperacaoSessao) { if (api.operacaoSessao.value != esperado) throw SessaoAlterada() }
}
