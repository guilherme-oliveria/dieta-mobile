package org.br.dietamais.data.remoto

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.AuthCircuitBreaker
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.AttributeKey
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.serializer
import org.br.dietamais.data.local.ArmazemSessaoSql
import org.br.dietamais.data.local.BancoDieta
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.sync.novoIdLocal
import org.br.dietamais.dominio.convite.extrairCodigoConvite
import org.br.dietamais.plataforma.CofreSessao
import org.br.dietamais.plataforma.ArmazenamentoSeguroIndisponivel
import org.br.dietamais.plataforma.Config
import org.br.dietamais.plataforma.motorHttp
import kotlin.uuid.Uuid

class ApiDieta(config: Config, versaoApp: String, plataforma: Plataforma, cofre: CofreSessao,
    armazem: ArmazemSessao, private val engine: HttpClientEngine = motorHttp()) : AutoCloseable {
    /** Montagem offline-first: a sessão também fica na tabela `sessao` e o logout limpa os caches do banco. */
    constructor(config: Config, versaoApp: String, plataforma: Plataforma, cofre: CofreSessao,
        banco: BancoDieta, engine: HttpClientEngine = motorHttp()) : this(config, versaoApp, plataforma, cofre, ArmazemSessaoSql(banco), engine)
    private val web = plataforma == Plataforma.WEB
    private val origem = enderecoApi(config.URL_API)
    private val base = origem.toString()
    private val controle = SessaoLocal(cofre, armazem, refreshNoCorpo = plataforma != Plataforma.WEB)
    val sessao = controle.fluxo
    val operacaoSessao = controle.fluxoOperacao
    private val obsoleta = kotlinx.coroutines.flow.MutableStateFlow(false)
    /** `426 VERSAO_APP_OBSOLETA` (BE-31): o app mostra a tela de atualização e para de insistir. */
    val versaoObsoleta: kotlinx.coroutines.flow.StateFlow<Boolean> = obsoleta
    private val cliente = HttpClient(engine) {
        followRedirects = false
        expectSuccess = false
        install(ContentNegotiation) { json(JsonContrato) }
        install(HttpTimeout) { requestTimeoutMillis = 30_000; connectTimeoutMillis = 15_000; socketTimeoutMillis = 30_000 }
        defaultRequest {
            header("X-App-Versao", versaoApp)
            header("X-Plataforma", plataforma.name)
            accept(ContentType.Application.Json)
        }
        install(Auth) {
            reAuthorizeOnResponse { resposta ->
                resposta.status == HttpStatusCode.Unauthorized && !resposta.call.request.attributes.contains(AuthCircuitBreaker) &&
                    controle.corresponde(resposta.call.request.attributes.getOrNull(GERACAO_SESSAO))
            }
            bearer {
                cacheTokens = false
                nonCancellableRefresh = true
                loadTokens { controle.tokens() }
                sendWithoutRequest { request ->
                    val url = request.url.build()
                    url.host == origem.host && url.port == origem.port && url.protocol == origem.protocol
                }
                refreshTokens {
                    controle.renovar(response.call.request.attributes.getOrNull(GERACAO_SESSAO),
                        response.call.request.headers[HttpHeaders.Authorization]?.removePrefix("Bearer ") ?: oldTokens?.accessToken) { token ->
                        val resposta = try {
                            client.post(base + "auth/refresh") {
                                markAsRefreshTokenRequest()
                                header("Idempotency-Key", novoIdLocal().toString())
                                // Web: sem corpo; o navegador manda o cookie HttpOnly e o backend confere a Origin.
                                if (!web) { contentType(ContentType.Application.Json); setBody(RefreshRequisicao(token)) }
                            }
                        } catch (cancelamento: CancellationException) { throw cancelamento }
                        catch (falha: Exception) { throw FalhaComunicacao() }
                        if (resposta.status.value == 429 || resposta.status.value >= 500) throw FalhaComunicacao()
                        decodificar(resposta, SessaoResposta.serializer())
                    }
                }
            }
        }
    }

    init {
        require(plataforma != Plataforma.OUTRO && versaoApp.isNotBlank())
    }

    suspend inline fun <reified T> obter(caminho: String, parametros: Map<String, String> = emptyMap(), publica: Boolean = false): T =
        decodificar(requisitar(HttpMethod.Get, caminho, parametros = parametros, publica = publica), serializer())

    suspend inline fun <reified T> enviar(caminho: String, corpo: JsonElement? = null, chave: Uuid = novoIdLocal(), publica: Boolean = false): T =
        decodificar(requisitar(HttpMethod.Post, caminho, corpo, chave, publica = publica), serializer())

    suspend inline fun <reified T> alterar(caminho: String, corpo: JsonElement, versao: Long): T =
        decodificar(requisitar(HttpMethod.Patch, caminho, corpo, versao = versao), serializer())

    suspend inline fun <reified T> substituir(caminho: String, corpo: JsonElement, versao: Long): T =
        decodificar(requisitar(HttpMethod.Put, caminho, corpo, versao = versao), serializer())

    suspend inline fun <reified T> enviarComVersao(caminho: String, versao: Long, corpo: JsonElement? = null, chave: Uuid = novoIdLocal()): T =
        decodificar(requisitar(HttpMethod.Post, caminho, corpo, chave, versao = versao), serializer())

    suspend fun entrar(requisicao: LoginRequisicao): SessaoResposta {
        val geracao = controle.geracao()
        val resposta = decodificar(requisitar(HttpMethod.Post, "auth/login", JsonContrato.encodeToJsonElement(requisicao), novoIdLocal(), publica = true), SessaoResposta.serializer())
        controle.receber(resposta, geracao)
        return resposta
    }

    /**
     * Na web, recarregar a página perde o access (só em memória): tenta o refresh silencioso pelo cookie. O resultado distingue
     * "não há sessão" (cookie ausente ou vencido: 401) de "não deu para saber" (sem rede, 5xx, 429): neste caso o cookie continua
     * valendo e quem chama não deve tratar como logout.
     */
    suspend fun restaurarSessao(): ResultadoRestauracao {
        if (controle.tokens() != null) return ResultadoRestauracao.JA_ATIVA
        if (!web) return ResultadoRestauracao.NAO_APLICAVEL
        val geracao = controle.geracao()
        val resposta = try { requisitar(HttpMethod.Post, "auth/refresh", publica = true, chave = novoIdLocal()) }
        catch (cancelamento: CancellationException) { throw cancelamento }
        catch (falha: FalhaComunicacao) { return ResultadoRestauracao.SEM_REDE }
        val status = resposta.status.value
        if (status in 200..299) {
            controle.receber(decodificar(resposta, SessaoResposta.serializer()), geracao)
            return ResultadoRestauracao.RESTAURADA
        }
        return if (status == 400 || status == 401 || status == 403) ResultadoRestauracao.SEM_SESSAO else ResultadoRestauracao.INDISPONIVEL
    }
    internal suspend fun aguardarOperacoesSessao() = controle.aguardarOperacoes()
    suspend fun usuarioAtual(): UsuarioResposta? = controle.usuarioAtual()
    suspend fun consultarTermosUso(): TermosUsoResposta = obter("legal/terms-of-use", publica = true)

    suspend fun aceitarConvite(codigo: String, requisicao: AceitarConviteRequisicao, chave: Uuid): AceiteConviteResposta {
        require(extrairCodigoConvite(codigo) == codigo)
        val geracao = controle.geracao()
        val usuario = controle.usuarioAtual()
        if (requisicao.cadastro != null && usuario != null) throw SessaoAlterada()
        val resposta = decodificar(requisitar(HttpMethod.Post, "convites/$codigo/aceitar",
            JsonContrato.encodeToJsonElement(requisicao), chave, publica = requisicao.cadastro != null), AceiteConviteResposta.serializer())
        val recebido = resposta.sessao.usuario
        if (recebido.perfil != Perfil.PACIENTE || recebido.pacienteId != resposta.vinculo.pacienteId ||
            resposta.vinculo.status != StatusVinculo.ATIVO || (usuario != null && recebido.id != usuario.id)) throw RespostaInvalida()
        controle.receber(resposta.sessao, geracao)
        return resposta
    }

    suspend fun enviarSemResposta(caminho: String, corpo: JsonElement, chave: Uuid = novoIdLocal()) {
        verificarErro(requisitar(HttpMethod.Post, caminho, corpo, chave))
    }

    suspend fun excluir(caminho: String) {
        verificarErro(requisitar(HttpMethod.Delete, caminho))
    }

    suspend fun sair() = controle.encerrar { tokens ->
        if (web && tokens != null) {
            verificarErro(requisitar(HttpMethod.Post, "auth/logout", null, novoIdLocal(), publica = true))
        } else if (tokens?.refreshToken != null) {
            val resposta = requisitar(HttpMethod.Post, "auth/logout", JsonContrato.encodeToJsonElement(LogoutRequisicao(tokens.refreshToken!!)), novoIdLocal(), publica = true)
            verificarErro(resposta)
        }
    }

    @PublishedApi
    internal suspend fun requisitar(metodo: HttpMethod, caminho: String, corpo: JsonElement? = null, chave: Uuid? = null,
        parametros: Map<String, String> = emptyMap(), publica: Boolean = false, versao: Long? = null): HttpResponse {
        require(caminho.matches(Regex("[A-Za-z0-9_-]+(/[A-Za-z0-9_-]+)*"))) { "Caminho fora da API." }
        require(versao == null || versao >= 0)
        val termosUso = caminho == "legal/terms-of-use"
        if (termosUso) require(metodo == HttpMethod.Get && publica && corpo == null && parametros.isEmpty())
        val endereco = if (termosUso) base.removeSuffix("api/v1/") + caminho else base + caminho
        val semBearer = publica || caminho.startsWith("auth/")
        val geracao = if (semBearer) null else controle.geracao()
        val resposta = try {
            cliente.request(endereco) {
                method = metodo
                parametros.forEach { (nome, valor) -> parameter(nome, valor) }
                if (semBearer) attributes.put(AuthCircuitBreaker, Unit)
                if (geracao != null) attributes.put(GERACAO_SESSAO, geracao)
                if (chave != null) header("Idempotency-Key", chave.toString())
                if (versao != null) header(HttpHeaders.IfMatch, "\"$versao\"")
                if (corpo != null) { contentType(ContentType.Application.Json); setBody(corpo) }
            }
        } catch (cancelamento: CancellationException) { throw cancelamento }
        catch (falha: ArmazenamentoSeguroIndisponivel) { throw falha }
        catch (falha: RespostaInvalida) { throw falha }
        catch (falha: Exception) { throw FalhaComunicacao() }
        if (resposta.status.value == 426) obsoleta.value = true
        if (resposta.status == HttpStatusCode.Unauthorized && !semBearer) controle.rejeitar(geracao)
        if (resposta.status.value in 200..299 && geracao != null && !controle.corresponde(geracao)) throw SessaoAlterada()
        return resposta
    }

    @PublishedApi
    internal suspend fun <T> decodificar(resposta: HttpResponse, serializer: KSerializer<T>): T {
        verificarErro(resposta)
        return try { JsonContrato.decodeFromString(serializer, resposta.bodyAsText()) }
        catch (cancelamento: CancellationException) { throw cancelamento }
        catch (falha: Exception) { throw RespostaInvalida() }
    }

    private suspend fun verificarErro(resposta: HttpResponse) {
        if (resposta.status.value in 200..299) return
        val problema = try { JsonContrato.decodeFromString<ProblemaResposta>(resposta.bodyAsText()) }
        catch (cancelamento: CancellationException) { throw cancelamento }
        catch (falha: Exception) { throw RespostaInvalida() }
        throw ErroApi(problema.codigo, problema.regra, problema.campos, resposta.status.value, problema.traceId, problema.atual)
    }

    override fun close() { cliente.close(); engine.close() }

    private companion object { val GERACAO_SESSAO = AttributeKey<Long>("DietaGeracaoSessao") }
}

private fun enderecoApi(configurada: String): Url {
    try {
        val url = Url(configurada.trimEnd('/'))
        require(url.protocol in listOf(URLProtocol.HTTP, URLProtocol.HTTPS) && url.user.isNullOrEmpty() &&
            url.password.isNullOrEmpty() && url.parameters.isEmpty() && url.fragment.isEmpty())
        require(url.encodedPath in listOf("", "/", "/api/v1"))
        require(url.protocol == URLProtocol.HTTPS || url.host in setOf("localhost", "127.0.0.1", "10.0.2.2", "::1") || REDE_PRIVADA.matches(url.host))
        val prefixo = if (url.encodedPath == "/api/v1") "" else "/api/v1"
        return Url(url.toString().trimEnd('/') + prefixo + "/")
    } catch (falha: Exception) { throw IllegalArgumentException("URL da API inválida.") }
}

/** Desenvolvimento: API no PC/WSL acessada pela rede local (http só para IPv4 privado; o Android ainda exige o build debug para liberar o tráfego). */
private val REDE_PRIVADA = Regex("(10\\.\\d{1,3}|192\\.168|172\\.(1[6-9]|2\\d|3[01]))\\.\\d{1,3}\\.\\d{1,3}")

enum class ResultadoRestauracao { NAO_APLICAVEL, JA_ATIVA, RESTAURADA, SEM_SESSAO, SEM_REDE, INDISPONIVEL }
