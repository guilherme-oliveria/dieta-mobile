package org.br.dietamais.data.repositorio

import kotlinx.serialization.json.encodeToJsonElement
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*

interface PortaPrivacidade {
    /** Pedido mais recente de exportação, ou null se nunca pediu. */
    suspend fun exportacao(): ExportacaoResposta?
    suspend fun pedirExportacao(): ExportacaoResposta
    suspend fun excluirConta(senha: String)
    suspend fun urlTermos(): String?
}

class PrivacidadeRepositorio(private val api: ApiDieta) : PortaPrivacidade {
    override suspend fun exportacao() = try { api.obter<ExportacaoResposta>("eu/exportacao") } catch (e: ErroApi) { if (e.status == 404) null else throw e }
    override suspend fun pedirExportacao() = api.enviar<ExportacaoResposta>("eu/exportacao")
    override suspend fun excluirConta(senha: String) = api.enviarSemResposta("eu/excluir-conta", JsonContrato.encodeToJsonElement(ExcluirContaRequisicao(senha)))
    override suspend fun urlTermos() = api.consultarTermosUso().url.takeIf { it.startsWith("https://") || it.startsWith("http://") }
}
