package org.br.dietamais.data.repositorio

import kotlinx.serialization.json.encodeToJsonElement
import org.br.dietamais.data.local.BancoDieta
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.sync.novoIdLocal
import kotlin.uuid.Uuid

/**
 * Modelos de dieta da nutri (MOB-56, BE-52). Só online: a lista vem de `GET /eu/nutri/modelos-dieta` (sistema e dela) e as ações passam pela API.
 * MOB-56c: sem rede, a lista cai nos modelos dela já sincronizados (`modelosDieta` no sync); os do sistema vêm só pelo GET. Escrever offline é a MOB-29b.
 */
interface PortaModelosDieta {
    suspend fun listar(): List<ModeloDietaResposta>
    suspend fun duplicar(id: Uuid, chave: Uuid = novoIdLocal()): ModeloDietaResposta
    suspend fun excluir(id: Uuid, versao: Long)
    /** MOB-56b: cria um modelo da nutri do zero (RN-MOD-01: o do sistema nunca muda). A chave mantém o reenvio idempotente. */
    suspend fun criar(requisicao: SalvarModeloDietaRequisicao, chave: Uuid = novoIdLocal()): ModeloDietaResposta = throw UnsupportedOperationException()
    /** MOB-56b: substitui o conteúdo de um modelo dela (`If-Match` pela versão). */
    suspend fun atualizar(id: Uuid, versao: Long, requisicao: SalvarModeloDietaRequisicao): ModeloDietaResposta = throw UnsupportedOperationException()
    suspend fun salvarComoModelo(dietaId: Uuid, requisicao: SalvarComoModeloRequisicao, chave: Uuid = novoIdLocal()): ModeloDietaResposta
    /** `null` quando o paciente não tem avaliação (404): o modelo se aplica sem escala. */
    suspend fun referenciaAvaliacao(pacienteId: Uuid): ReferenciaAvaliacaoDietaResposta?
    /** RN-MOD-02: cria o rascunho (nunca publica). A chave mantém o reenvio idempotente. */
    suspend fun aplicar(pacienteId: Uuid, modeloId: Uuid, requisicao: AplicarModeloRequisicao, chave: Uuid = novoIdLocal()): DietaDeModeloResposta
}

class ModelosDietaRepositorio(private val api: ApiDieta, private val conferirArmazem: (OperacaoSessao) -> Unit,
    private val lerLocais: () -> List<ModeloDietaResposta> = { emptyList() }) : PortaModelosDieta {
    constructor(api: ApiDieta, banco: BancoDieta) : this(api, { esperado ->
        val sessao = banco.sessaoQueries.atual().executeAsOneOrNull()
        if (sessao?.usuario_id != esperado.contexto.usuarioId.toString() || sessao.perfil != esperado.contexto.perfil.name) throw SessaoAlterada()
    }, { lerModelosLocais(banco) })

    override suspend fun listar(): List<ModeloDietaResposta> {
        val contexto = contexto()
        val resposta = try { api.obter<ModelosDietaResposta>("eu/nutri/modelos-dieta") }
        catch (falha: FalhaComunicacao) {
            // MOB-56c: sem rede, mostra os modelos dela que o sync já trouxe; sem nenhum, a falha segue para a tela
            conferir(contexto)
            val locais = lerLocais()
            if (locais.isEmpty()) throw falha
            return locais
        }
        conferir(contexto)
        return resposta.itens
    }
    override suspend fun duplicar(id: Uuid, chave: Uuid): ModeloDietaResposta {
        val contexto = contexto()
        val resposta = api.enviar<ModeloDietaResposta>("eu/nutri/modelos-dieta/$id/duplicar", chave = chave)
        conferir(contexto)
        return resposta
    }
    override suspend fun criar(requisicao: SalvarModeloDietaRequisicao, chave: Uuid): ModeloDietaResposta {
        val contexto = contexto()
        val resposta = api.enviar<ModeloDietaResposta>("eu/nutri/modelos-dieta", JsonContrato.encodeToJsonElement(requisicao), chave)
        conferir(contexto)
        if (requisicao.id != null && resposta.id != requisicao.id) throw RespostaInvalida()
        return resposta
    }
    override suspend fun atualizar(id: Uuid, versao: Long, requisicao: SalvarModeloDietaRequisicao): ModeloDietaResposta {
        val contexto = contexto()
        val resposta = api.substituir<ModeloDietaResposta>("eu/nutri/modelos-dieta/$id", JsonContrato.encodeToJsonElement(requisicao), versao)
        conferir(contexto)
        if (resposta.id != id) throw RespostaInvalida()
        return resposta
    }
    override suspend fun excluir(id: Uuid, versao: Long) {
        val contexto = contexto()
        api.excluirComVersao("eu/nutri/modelos-dieta/$id", versao)
        conferir(contexto)
    }
    override suspend fun salvarComoModelo(dietaId: Uuid, requisicao: SalvarComoModeloRequisicao, chave: Uuid): ModeloDietaResposta {
        val contexto = contexto()
        val resposta = api.enviar<ModeloDietaResposta>("eu/nutri/modelos-dieta/de-dieta/$dietaId", JsonContrato.encodeToJsonElement(requisicao), chave)
        conferir(contexto)
        return resposta
    }
    override suspend fun referenciaAvaliacao(pacienteId: Uuid): ReferenciaAvaliacaoDietaResposta? {
        val contexto = contexto()
        val resposta = try { api.obter<ReferenciaAvaliacaoDietaResposta>("pacientes/$pacienteId/dietas/referencia-avaliacao") }
        catch (e: ErroApi) { if (e.status == 404) null else throw e }
        conferir(contexto)
        return resposta
    }
    override suspend fun aplicar(pacienteId: Uuid, modeloId: Uuid, requisicao: AplicarModeloRequisicao, chave: Uuid): DietaDeModeloResposta {
        val contexto = contexto()
        val resposta = api.enviar<DietaDeModeloResposta>("pacientes/$pacienteId/dietas/de-modelo/$modeloId", JsonContrato.encodeToJsonElement(requisicao), chave)
        conferir(contexto)
        return resposta
    }
    private fun contexto() = api.operacaoSessao.value?.takeIf { it.contexto.perfil == Perfil.NUTRICIONISTA } ?: throw SessaoAlterada()
    private fun conferir(esperado: OperacaoSessao) {
        if (api.operacaoSessao.value != esperado) throw SessaoAlterada()
        conferirArmazem(esperado)
    }
}

/** Modelos da nutri guardados pelo sync (coleção `modelosDieta`), do mais novo para o mais antigo. Registro ilegível é ignorado: é cache, não verdade. */
internal fun lerModelosLocais(banco: BancoDieta): List<ModeloDietaResposta> =
    banco.registroSyncQueries.daSessao("modelosDieta").executeAsList().mapNotNull { json ->
        try { JsonContrato.decodeFromString<ModeloDietaResposta>(json) } catch (_: Exception) { null }
    }.sortedWith(compareByDescending<ModeloDietaResposta> { it.atualizadoEm }.thenBy { it.id.toString() })
