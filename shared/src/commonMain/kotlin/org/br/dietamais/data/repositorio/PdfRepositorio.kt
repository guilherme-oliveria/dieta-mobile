package org.br.dietamais.data.repositorio

import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import kotlin.uuid.Uuid

enum class RecursoPdf { DIETA, AVALIACAO }

interface PortaPdf {
    /** Link assinado de curta duração (10 min) para baixar o PDF; o backend gera na hora (BE-28). */
    suspend fun link(recurso: RecursoPdf, id: Uuid): String
}

class PdfRepositorio(private val api: ApiDieta) : PortaPdf {
    override suspend fun link(recurso: RecursoPdf, id: Uuid): String {
        val contexto = api.operacaoSessao.value?.takeIf { it.contexto.perfil == Perfil.NUTRICIONISTA || it.contexto.perfil == Perfil.PACIENTE } ?: throw SessaoAlterada()
        val prefixo = if (contexto.contexto.perfil == Perfil.PACIENTE) "eu/" else ""
        val colecao = if (recurso == RecursoPdf.DIETA) "dietas" else "avaliacoes"
        val url = api.obter<PdfResposta>("$prefixo$colecao/$id/pdf").url
        if (api.operacaoSessao.value != contexto) throw SessaoAlterada()
        if (!(url.startsWith("https://") || url.startsWith("http://"))) throw RespostaInvalida()
        return url
    }
}
