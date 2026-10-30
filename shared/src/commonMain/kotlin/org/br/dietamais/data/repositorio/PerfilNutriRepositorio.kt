package org.br.dietamais.data.repositorio

import kotlinx.serialization.json.encodeToJsonElement
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*

interface PortaPerfilNutri {
    suspend fun obter(): PerfilNutriResposta
    suspend fun salvar(versao: Long, requisicao: AtualizarPerfilNutriRequisicao): PerfilNutriResposta
    /** Logo do cabeçalho dos PDFs (BE-28): PNG ou JPEG de até 512 KB, conferido pelo backend. */
    suspend fun enviarLogo(bytes: ByteArray, tipo: String)
    suspend fun removerLogo()
}

class PerfilNutriRepositorio(private val api: ApiDieta) : PortaPerfilNutri {
    override suspend fun obter(): PerfilNutriResposta {
        val contexto = contexto()
        return api.obter<PerfilNutriResposta>("eu/nutri").also { conferir(contexto) }
    }
    override suspend fun salvar(versao: Long, requisicao: AtualizarPerfilNutriRequisicao): PerfilNutriResposta {
        val contexto = contexto()
        return api.alterar<PerfilNutriResposta>("eu/nutri", JsonContrato.encodeToJsonElement(requisicao), versao).also { conferir(contexto) }
    }
    override suspend fun enviarLogo(bytes: ByteArray, tipo: String) {
        val contexto = contexto()
        api.enviarImagem("eu/nutri/logo", bytes, io.ktor.http.ContentType.parse(tipo))
        conferir(contexto)
    }
    override suspend fun removerLogo() {
        val contexto = contexto()
        api.excluir("eu/nutri/logo")
        conferir(contexto)
    }
    private fun contexto() = api.operacaoSessao.value?.takeIf { it.contexto.perfil == Perfil.NUTRICIONISTA } ?: throw SessaoAlterada()
    private fun conferir(esperado: OperacaoSessao) { if (api.operacaoSessao.value != esperado) throw SessaoAlterada() }
}
