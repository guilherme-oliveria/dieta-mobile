package org.br.dietamais.data.repositorio

import kotlinx.serialization.json.encodeToJsonElement
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.sync.novoIdLocal
import kotlin.uuid.Uuid

/** Produtos e suplementos da nutri (MOB-70, BE-48b). Só online: o backend valida assinatura, limite e dono. */
interface PortaProdutos {
    suspend fun listar(busca: String, ativo: Boolean, pagina: Int): PaginaProdutosResposta
    suspend fun criar(requisicao: CriarProdutoRequisicao, chave: Uuid = novoIdLocal()): ProdutoResposta
    suspend fun alterar(id: Uuid, versao: Long, requisicao: AtualizarProdutoRequisicao): ProdutoResposta
}

class ProdutosRepositorio(private val api: ApiDieta) : PortaProdutos {
    override suspend fun listar(busca: String, ativo: Boolean, pagina: Int): PaginaProdutosResposta {
        val contexto = contexto()
        val parametros = buildMap { put("pagina", pagina.toString()); put("tamanho", TAMANHO.toString()); put("ativo", ativo.toString()); if (busca.isNotBlank()) put("busca", busca.trim()) }
        val resposta = api.obter<PaginaProdutosResposta>("produtos", parametros)
        conferir(contexto)
        if (resposta.pagina != pagina || resposta.itens.size > TAMANHO || resposta.itens.any { it.ativo != ativo }) throw RespostaInvalida()
        return resposta
    }
    override suspend fun criar(requisicao: CriarProdutoRequisicao, chave: Uuid): ProdutoResposta {
        val contexto = contexto()
        val resposta = api.enviar<ProdutoResposta>("produtos", JsonContrato.encodeToJsonElement(requisicao), chave)
        conferir(contexto)
        if (requisicao.id != null && resposta.id != requisicao.id) throw RespostaInvalida()
        return resposta
    }
    override suspend fun alterar(id: Uuid, versao: Long, requisicao: AtualizarProdutoRequisicao): ProdutoResposta {
        val contexto = contexto()
        val resposta = api.alterar<ProdutoResposta>("produtos/$id", JsonContrato.encodeToJsonElement(requisicao), versao)
        conferir(contexto)
        if (resposta.id != id) throw RespostaInvalida()
        return resposta
    }
    private fun contexto() = api.operacaoSessao.value?.takeIf { it.contexto.perfil == Perfil.NUTRICIONISTA } ?: throw SessaoAlterada()
    private fun conferir(esperado: OperacaoSessao) { if (api.operacaoSessao.value != esperado) throw SessaoAlterada() }
    companion object { const val TAMANHO = 20 }
}
