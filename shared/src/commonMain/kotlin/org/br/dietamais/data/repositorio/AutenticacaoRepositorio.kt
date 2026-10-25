package org.br.dietamais.data.repositorio

import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.encodeToJsonElement
import org.br.dietamais.data.remoto.ApiDieta
import org.br.dietamais.data.remoto.ContextoSessao
import org.br.dietamais.data.remoto.JsonContrato
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.plataforma.IdentidadeDispositivo
import org.br.dietamais.plataforma.plataformaApi
import org.br.dietamais.data.sync.novoIdLocal
import kotlin.uuid.Uuid

interface PortaAutenticacao {
    val sessao: StateFlow<ContextoSessao?>
    suspend fun restaurar(): UsuarioResposta?
    suspend fun entrar(email: String, senha: String, perfil: Perfil? = null): SessaoResposta
    suspend fun cadastrar(requisicao: CadastroNutriRequisicao, chave: Uuid = novoIdLocal())
    suspend fun recuperar(email: String)
    suspend fun redefinir(token: String, senha: String)
    suspend fun consultarEu(): EuResposta
    suspend fun sair()
}

class AutenticacaoRepositorio(private val api: ApiDieta, private val identidade: IdentidadeDispositivo) : PortaAutenticacao {
    override val sessao = api.sessao
    override suspend fun restaurar(): UsuarioResposta? = api.usuarioAtual()
    override suspend fun entrar(email: String, senha: String, perfil: Perfil?) = api.entrar(LoginRequisicao(email, senha,
        DispositivoRequisicao(identidade.obter(), plataformaApi(), "Dieta+ ${plataformaApi().name}"), perfil))
    override suspend fun cadastrar(requisicao: CadastroNutriRequisicao, chave: Uuid) {
        api.enviar<CadastroNutriResposta>("auth/cadastro-nutri", JsonContrato.encodeToJsonElement(requisicao), chave = chave, publica = true)
    }
    override suspend fun recuperar(email: String) = api.enviarSemResposta("auth/esqueci-senha", JsonContrato.encodeToJsonElement(EsqueciSenhaRequisicao(email)))
    override suspend fun redefinir(token: String, senha: String) = api.enviarSemResposta("auth/redefinir-senha", JsonContrato.encodeToJsonElement(RedefinirSenhaRequisicao(token, senha)))
    override suspend fun consultarEu(): EuResposta = api.obter("eu")
    override suspend fun sair() = api.sair()
}
