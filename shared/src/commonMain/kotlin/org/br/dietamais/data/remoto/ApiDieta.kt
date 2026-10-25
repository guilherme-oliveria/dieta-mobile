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
import org.br.dietamais.data.local.BancoDieta
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.sync.novoIdLocal
import org.br.dietamais.plataforma.CofreSessao
import org.br.dietamais.plataforma.ArmazenamentoSeguroIndisponivel
import org.br.dietamais.plataforma.Config
import org.br.dietamais.plataforma.motorHttp
import kotlin.uuid.Uuid

class ApiDieta(config: Config, versaoApp: String, plataforma: Plataforma, cofre: CofreSessao,
    banco: BancoDieta, private val engine: HttpClientEngine = motorHttp()) : AutoCloseable {
    private val origem = enderecoApi(config.URL_API)
    private val base = origem.toString()
    private val controle = SessaoLocal(cofre, banco)
    val sessao = controle.fluxo
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
                                contentType(ContentType.Application.Json)
                                header("Idempotency-Key", novoIdLocal().toString())
                                setBody(RefreshRequisicao(token))
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

    suspend fun entrar(requisicao: LoginRequisicao): SessaoResposta {
        val geracao = controle.geracao()
        val resposta = decodificar(requisitar(HttpMethod.Post, "auth/login", JsonContrato.encodeToJsonElement(requisicao), novoIdLocal(), publica = true), SessaoResposta.serializer())
        controle.receber(resposta, geracao)
        return resposta
    }

    suspend fun restaurarSessao() { controle.tokens() }
    suspend fun usuarioAtual(): UsuarioResposta? = controle.usuarioAtual()

    suspend fun enviarSemResposta(caminho: String, corpo: JsonElement, chave: Uuid = novoIdLocal()) {
        verificarErro(requisitar(HttpMethod.Post, caminho, corpo, chave))
    }

    suspend fun sair() = controle.encerrar { tokens ->
        if (tokens?.refreshToken != null) {
            val resposta = requisitar(HttpMethod.Post, "auth/logout", JsonContrato.encodeToJsonElement(LogoutRequisicao(tokens.refreshToken!!)), novoIdLocal(), publica = true)
            verificarErro(resposta)
        }
    }

    @PublishedApi
    internal suspend fun requisitar(metodo: HttpMethod, caminho: String, corpo: JsonElement? = null, chave: Uuid? = null,
        parametros: Map<String, String> = emptyMap(), publica: Boolean = false): HttpResponse {
        require(caminho.matches(Regex("[A-Za-z0-9_-]+(/[A-Za-z0-9_-]+)*"))) { "Caminho fora da API." }
        val semBearer = publica || caminho.startsWith("auth/")
        val geracao = if (semBearer) null else controle.geracao()
        val resposta = try {
            cliente.request(base + caminho) {
                method = metodo
                parametros.forEach { (nome, valor) -> parameter(nome, valor) }
                if (semBearer) attributes.put(AuthCircuitBreaker, Unit)
                if (geracao != null) attributes.put(GERACAO_SESSAO, geracao)
                if (chave != null) header("Idempotency-Key", chave.toString())
                if (corpo != null) { contentType(ContentType.Application.Json); setBody(corpo) }
            }
        } catch (cancelamento: CancellationException) { throw cancelamento }
        catch (falha: ArmazenamentoSeguroIndisponivel) { throw falha }
        catch (falha: RespostaInvalida) { throw falha }
        catch (falha: Exception) { throw FalhaComunicacao() }
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
        require(url.protocol == URLProtocol.HTTPS || url.host in setOf("localhost", "127.0.0.1", "10.0.2.2", "::1"))
        val prefixo = if (url.encodedPath == "/api/v1") "" else "/api/v1"
        return Url(url.toString().trimEnd('/') + prefixo + "/")
    } catch (falha: Exception) { throw IllegalArgumentException("URL da API inválida.") }
}
