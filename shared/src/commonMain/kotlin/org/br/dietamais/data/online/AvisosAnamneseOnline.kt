package org.br.dietamais.data.online

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import io.ktor.http.HttpMethod
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.PortaAnamnese
import org.br.dietamais.data.repositorio.PortaAvisos
import org.br.dietamais.data.repositorio.RespostasPendentes
import kotlin.time.Clock
import kotlin.uuid.Uuid

/** Central de avisos pela API: recarrega ao abrir e a cada minuto enquanto alguém observa (a web não recebe push). */
class AvisosOnline(private val api: ApiDieta, private val intervalo: Long = 60_000) : PortaAvisos {
    private val recurso = RecursoOnline<AvisoResposta>(api.sessao) { emptyList() }

    override fun observar(): Flow<List<AvisoResposta>> = channelFlow {
        val atualizacao = launch {
            while (isActive) {
                try { carregar() } catch (c: CancellationException) { throw c } catch (_: Exception) { }
                delay(intervalo)
            }
        }
        launch { recurso.fluxo.map { it.sortedWith(compareByDescending<AvisoResposta> { a -> a.criadoEm }.thenByDescending { a -> a.id.toString() }) }.collect { send(it) } }
        awaitClose { atualizacao.cancel() }
    }

    private suspend fun carregar() {
        val esperado = api.operacaoSessao.value ?: throw SessaoAlterada()
        val resposta = api.obter<AvisosResposta>("eu/avisos", mapOf("pagina" to "0", "tamanho" to TAMANHO.toString()))
        if (api.operacaoSessao.value != esperado) throw SessaoAlterada()
        recurso.trocar { resposta.itens }
    }

    override suspend fun marcarLido(id: Uuid) {
        val esperado = api.operacaoSessao.value ?: throw SessaoAlterada()
        val aviso = api.enviar<AvisoResposta>("eu/avisos/$id/lido")
        if (api.operacaoSessao.value != esperado || aviso.id != id) throw SessaoAlterada()
        recurso.gravado(aviso) { it.id == id }
    }

    override suspend fun marcarTodosLidos() {
        val esperado = api.operacaoSessao.value ?: throw SessaoAlterada()
        api.enviarSemResposta("eu/avisos/lidos", JsonObject(emptyMap()))
        if (api.operacaoSessao.value != esperado) throw SessaoAlterada()
        val agora = Clock.System.now()
        recurso.trocar { lista -> lista.map { if (it.lido) it else it.copy(lidoEm = agora) } }
    }

    private companion object { const val TAMANHO = 50 }
}

/**
 * Anamnese pela API (web). Nutri: modelos e questionários de cada ficha, carregados ao abrir. Paciente: `GET /eu/questionarios`
 * ao observar; as respostas parciais entram na tela na hora e seguem para o servidor em ordem, sem fila em disco: se a página
 * fechar antes do envio, o que não chegou se perde (aviso em web.md). Concluir só vale com tudo já enviado (RN-ANA-03).
 */
class AnamneseOnline(private val api: ApiDieta, private val escopo: CoroutineScope) : PortaAnamnese {
    private val modelos = RecursoOnline<ModeloQuestionarioResposta>(api.sessao) {
        val esperado = contexto(Perfil.NUTRICIONISTA)
        val resposta = api.obter<ModelosResposta>("modelos-questionario")
        conferir(esperado); resposta.itens
    }
    private val questionarios = RecursoOnline<QuestionarioResposta>(api.sessao) { emptyList() }
    private val trava = Mutex()
    private val aEnviar = mutableMapOf<Uuid, MutableMap<Uuid, JsonElement?>>()

    override fun observarModelos(): Flow<List<ModeloQuestionarioResposta>> =
        modelos.fluxo.map { it.sortedWith(compareByDescending<ModeloQuestionarioResposta> { m -> m.padrao }.thenBy { m -> m.criadoEm }) }

    override suspend fun atualizarModelos() { contexto(Perfil.NUTRICIONISTA); modelos.atualizar() }

    override suspend fun criarModelo(requisicao: SalvarModeloRequisicao): ModeloQuestionarioResposta {
        val esperado = contexto(Perfil.NUTRICIONISTA)
        val id = requisicao.id ?: Uuid.random()
        val criado = api.enviar<ModeloQuestionarioResposta>("modelos-questionario", JsonContrato.encodeToJsonElement(requisicao.copy(id = id)), id)
        conferir(esperado)
        if (criado.id != id) throw RespostaInvalida()
        modelos.gravado(criado) { it.id == id }
        return criado
    }

    override suspend fun salvarModelo(id: Uuid, versao: Long, requisicao: SalvarModeloRequisicao): ModeloQuestionarioResposta {
        val esperado = contexto(Perfil.NUTRICIONISTA)
        val salvo = api.substituir<ModeloQuestionarioResposta>("modelos-questionario/$id", JsonContrato.encodeToJsonElement(requisicao.copy(id = id)), versao)
        conferir(esperado)
        if (salvo.id != id) throw RespostaInvalida()
        modelos.gravado(salvo) { it.id == id }
        return salvo
    }

    override fun observarQuestionarios(pacienteId: Uuid?): Flow<List<QuestionarioResposta>> =
        questionarios.fluxo.map { lista ->
            lista.filter { pacienteId == null || it.pacienteId == pacienteId }
                .sortedWith(compareByDescending<QuestionarioResposta> { it.enviadoEm }.thenByDescending { it.criadoEm })
        }.let { fluxo -> if (api.sessao.value?.perfil == Perfil.PACIENTE) fluxo.onStart { carregarMeus() } else fluxo }

    /** Paciente: um erro de rede aqui só deixa a lista como está; a tela mostra o que já tinha. */
    private suspend fun carregarMeus() {
        val esperado = contexto(Perfil.PACIENTE)
        val resposta = try { api.obter<QuestionariosResposta>("eu/questionarios") } catch (c: CancellationException) { throw c } catch (_: Exception) { return }
        conferir(esperado)
        questionarios.trocar { atual ->
            // o que o paciente digitou e ainda não chegou ao servidor continua valendo na tela
            resposta.itens.map { novo -> aEnviar[novo.id]?.let { novo.comRespostas(it) } ?: novo }
        }
    }

    override suspend fun atualizarQuestionarios(pacienteId: Uuid) {
        val esperado = contexto(Perfil.NUTRICIONISTA)
        val resposta = api.obter<QuestionariosResposta>("pacientes/$pacienteId/questionarios")
        conferir(esperado)
        if (resposta.itens.any { it.pacienteId != pacienteId }) throw RespostaInvalida()
        questionarios.trocar { atual -> atual.filter { it.pacienteId != pacienteId } + resposta.itens }
    }

    override suspend fun enviar(pacienteId: Uuid, requisicao: EnviarQuestionarioRequisicao): QuestionarioResposta {
        val esperado = contexto(Perfil.NUTRICIONISTA)
        val id = requisicao.id ?: Uuid.random()
        val enviado = api.enviar<QuestionarioResposta>("pacientes/$pacienteId/questionarios", JsonContrato.encodeToJsonElement(requisicao.copy(id = id)), id)
        conferir(esperado)
        if (enviado.id != id || enviado.pacienteId != pacienteId) throw RespostaInvalida()
        questionarios.gravado(enviado) { it.id == id }
        return enviado
    }

    override fun salvarRespostas(id: Uuid, respostas: Map<Uuid, JsonElement?>) {
        if (respostas.isEmpty()) return
        val dono = api.sessao.value?.takeIf { it.perfil == Perfil.PACIENTE } ?: throw SessaoAlterada()
        var fechado = false
        questionarios.trocar { lista ->
            lista.map { q ->
                if (q.id != id) q
                else if (q.status != StatusQuestionario.PENDENTE) { fechado = true; q }
                else q.comRespostas(respostas)
            }
        }
        if (fechado) throw RespostasPendentes()
        aEnviar.getOrPut(id) { mutableMapOf() }.putAll(respostas)
        escopo.launch { descarregar(dono) }
    }

    /** Manda ao servidor, uma pergunta de cada vez na ordem, o que está acumulado; em falha o acúmulo fica para a próxima tentativa. */
    private suspend fun descarregar(dono: ContextoSessao) = trava.withLock {
        for (id in aEnviar.keys.toList()) {
            val lote = aEnviar[id]?.toMap().orEmpty()
            if (lote.isEmpty()) { aEnviar.remove(id); continue }
            try {
                if (api.sessao.value?.usuarioId != dono.usuarioId) { aEnviar.clear(); return@withLock }
                val corpo = JsonContrato.encodeToJsonElement(SalvarRespostasRequisicao(lote))
                api.decodificar(api.requisitar(HttpMethod.Put, "eu/questionarios/$id/respostas", corpo), QuestionarioResposta.serializer())
                val atual = aEnviar[id]
                lote.forEach { (pergunta, valor) -> if (atual != null && atual[pergunta] == valor) atual.remove(pergunta) }
                if (atual.isNullOrEmpty()) aEnviar.remove(id)
            } catch (c: CancellationException) { throw c }
            catch (falha: ErroApi) { if (falha.codigo == "QUESTIONARIO_FECHADO") aEnviar.remove(id) }
            catch (_: Exception) { }
        }
    }

    override suspend fun concluir(id: Uuid): QuestionarioResposta {
        val esperado = contexto(Perfil.PACIENTE)
        descarregar(esperado.contexto)
        if (!aEnviar[id].isNullOrEmpty()) throw RespostasPendentes()
        val concluido = api.enviar<QuestionarioResposta>("eu/questionarios/$id/enviar", null, Uuid.random())
        conferir(esperado)
        if (concluido.id != id) throw RespostaInvalida()
        questionarios.gravado(concluido) { it.id == id }
        return concluido
    }

    private fun QuestionarioResposta.comRespostas(novas: Map<Uuid, JsonElement?>): QuestionarioResposta {
        val mescladas = respostas.toMutableMap()
        novas.forEach { (pergunta, valor) -> if (valor == null || valor is kotlinx.serialization.json.JsonNull) mescladas.remove(pergunta) else mescladas[pergunta] = valor }
        return copy(respostas = mescladas)
    }

    private fun contexto(perfil: Perfil) = api.operacaoSessao.value?.takeIf { it.contexto.perfil == perfil } ?: throw SessaoAlterada()
    private fun conferir(esperado: OperacaoSessao) { if (api.operacaoSessao.value != esperado) throw SessaoAlterada() }
}
