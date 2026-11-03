package org.br.dietamais.data.repositorio

import org.br.dietamais.data.remoto.ApiDieta
import org.br.dietamais.data.remoto.RespostaInvalida
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.dominio.convite.extrairCodigoConvite
import kotlin.uuid.Uuid

interface PortaConvites {
    suspend fun consultar(codigo: String): ConviteResposta
    suspend fun consultarTermosUso(): TermosUsoResposta
    suspend fun aceitar(codigo: String, versaoTermo: String, cadastro: CadastroPacienteRequisicao?, chave: Uuid): AceiteConviteResposta
}

class ConvitesRepositorio(private val api: ApiDieta, private val dispositivo: suspend () -> DispositivoRequisicao) : PortaConvites {
    override suspend fun consultarTermosUso() = api.consultarTermosUso()
    override suspend fun consultar(codigo: String): ConviteResposta {
        require(extrairCodigoConvite(codigo) == codigo)
        val resposta = api.obter<ConviteResposta>("convites/$codigo", publica = true)
        if (resposta.termo.versao.isBlank() || resposta.termo.texto.isBlank() || resposta.nutricionista.nome.isBlank()) throw RespostaInvalida()
        return resposta
    }
    override suspend fun aceitar(codigo: String, versaoTermo: String, cadastro: CadastroPacienteRequisicao?, chave: Uuid) =
        api.aceitarConvite(codigo, AceitarConviteRequisicao(true, versaoTermo, cadastro, dispositivo()), chave)
}
