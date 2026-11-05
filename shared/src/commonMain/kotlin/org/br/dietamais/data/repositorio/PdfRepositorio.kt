package org.br.dietamais.data.repositorio

import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import kotlin.uuid.Uuid

enum class RecursoPdf { DIETA, AVALIACAO, LISTA }

/** MOB-73: versões do PDF da lista de substituição (valores do parâmetro `tamanho`, BE-50b). */
enum class TamanhoListaPdf { PEQUENA, MEDIA, GRANDE, CARBOIDRATOS }

interface PortaPdf {
    /** Link assinado de curta duração (10 min) para baixar o PDF; o backend gera na hora (BE-28). */
    suspend fun link(recurso: RecursoPdf, id: Uuid, tamanho: TamanhoListaPdf = TamanhoListaPdf.MEDIA): String
}

class PdfRepositorio(private val api: ApiDieta) : PortaPdf {
    override suspend fun link(recurso: RecursoPdf, id: Uuid, tamanho: TamanhoListaPdf): String {
        val contexto = api.operacaoSessao.value?.takeIf { it.contexto.perfil == Perfil.NUTRICIONISTA || it.contexto.perfil == Perfil.PACIENTE } ?: throw SessaoAlterada()
        val prefixo = if (contexto.contexto.perfil == Perfil.PACIENTE) "eu/" else ""
        val caminho = caminhoPdf(recurso, id, tamanho)
        val url = api.obter<PdfResposta>("$prefixo$caminho").url
        if (api.operacaoSessao.value != contexto) throw SessaoAlterada()
        if (!(url.startsWith("https://") || url.startsWith("http://"))) throw RespostaInvalida()
        return url
    }
}

internal fun caminhoPdf(recurso: RecursoPdf, id: Uuid, tamanho: TamanhoListaPdf = TamanhoListaPdf.MEDIA): String = when (recurso) {
    RecursoPdf.DIETA -> "dietas/$id/pdf"
    RecursoPdf.AVALIACAO -> "avaliacoes/$id/pdf"
    RecursoPdf.LISTA -> "dietas/$id/pdf-lista?tamanho=${tamanho.name}"
}
