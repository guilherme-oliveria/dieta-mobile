package org.br.dietamais.data.sync

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.json.encodeToJsonElement
import org.br.dietamais.data.local.BancoDieta
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*

interface FonteEnvioSync {
    suspend fun enviar(lote: List<MutacaoPendente>): List<ResultadoMutacaoResposta>
}

class FonteEnvioHttp(private val api: ApiDieta) : FonteEnvioSync {
    override suspend fun enviar(lote: List<MutacaoPendente>): List<ResultadoMutacaoResposta> {
        val corpo = EnviarMutacoesRequisicao(lote.map { MutacaoRequisicao(it.idMutacao, it.entidade, it.operacao.name, it.id, it.versaoBase, it.dados) })
        // a idempotência está no idMutacao de cada item; a chave do POST só cobre o lote inteiro
        return api.enviar<EnviarMutacoesResposta>("sync", JsonContrato.encodeToJsonElement(corpo)).resultados
    }
}

enum class ResultadoEnvio { VAZIA, CONCLUIDO, SEM_REDE, FALHOU, SEM_SESSAO, SESSAO_ALTERADA, EM_ANDAMENTO }

/**
 * Envia a fila `mutacao_pendente` em lotes (RN-SYN). APLICADA sai da fila; REJEITADA desfaz a criação local e fica marcada;
 * CONFLITO guarda o estado do servidor para a tela de escolha. O reenvio é seguro: o servidor devolve o resultado guardado do mesmo idMutacao.
 */
class EnviadorFila(private val fonte: FonteEnvioSync, private val banco: BancoDieta, private val contextoAtual: () -> ContextoLeituraSync?,
    private val tamanhoLote: Int = 100) {
    private val mutex = Mutex()
    init { require(tamanhoLote in 1..200) }

    suspend fun enviar(): ResultadoEnvio {
        if (!mutex.tryLock()) return ResultadoEnvio.EM_ANDAMENTO
        try {
            val contexto = contextoAtual() ?: return ResultadoEnvio.SEM_SESSAO
            val fila = FilaMutacoes(banco)
            var enviou = false
            while (true) {
                if (contextoAtual() != contexto) return ResultadoEnvio.SESSAO_ALTERADA
                val lote = fila.pendentes().take(tamanhoLote)
                if (lote.isEmpty()) return if (enviou) ResultadoEnvio.CONCLUIDO else ResultadoEnvio.VAZIA
                val resultados = try { fonte.enviar(lote) }
                catch (c: CancellationException) { throw c }
                catch (_: FalhaComunicacao) { registrarTentativa(contexto, lote); return ResultadoEnvio.SEM_REDE }
                catch (_: Exception) { registrarTentativa(contexto, lote); return ResultadoEnvio.FALHOU }
                if (contextoAtual() != contexto) return ResultadoEnvio.SESSAO_ALTERADA
                if (!aplicar(contexto, lote, resultados)) return ResultadoEnvio.FALHOU
                enviou = true
            }
        } finally { mutex.unlock() }
    }

    private fun registrarTentativa(contexto: ContextoLeituraSync, lote: List<MutacaoPendente>) = banco.transaction {
        lote.forEach { banco.mutacaoPendenteQueries.registrarTentativa(it.idMutacao.toString(), contexto.usuarioId.toString(), contexto.perfil.name) }
    }

    /** Devolve false se o servidor respondeu algo que não casa com o lote (nada é descartado nesse caso). */
    private fun aplicar(contexto: ContextoLeituraSync, lote: List<MutacaoPendente>, resultados: List<ResultadoMutacaoResposta>): Boolean {
        val porId = resultados.associateBy { it.idMutacao }
        if (resultados.size != lote.size || porId.size != lote.size || lote.any { it.idMutacao !in porId }) { registrarTentativa(contexto, lote); return false }
        val usuario = contexto.usuarioId.toString(); val perfil = contexto.perfil.name
        banco.transaction {
            lote.forEach { m ->
                val r = porId.getValue(m.idMutacao); val id = m.idMutacao.toString()
                when (r.status) {
                    StatusMutacao.APLICADA -> {
                        banco.mutacaoPendenteQueries.concluir(id, usuario, perfil)
                        if (m.entidade == "alimentos") {
                            banco.confirmarAlimento(usuario, perfil, m, r.versao)
                            banco.reconciliarAlimento(usuario, perfil, m.id.toString())
                        }
                        banco.limparAnteriorSeLivre(usuario, perfil, m.entidade, m.id.toString())
                    }
                    StatusMutacao.CONFLITO -> {
                        banco.conflitoQueries.salvar(id, usuario, perfil, (r.atual ?: kotlinx.serialization.json.JsonNull).toString())
                        banco.mutacaoPendenteQueries.marcar("CONFLITO", id, usuario, perfil)
                    }
                    StatusMutacao.REJEITADA, StatusMutacao.OUTRO -> {
                        banco.mutacaoPendenteQueries.marcar(if (r.status == StatusMutacao.REJEITADA) "REJEITADA" else "DESCONHECIDA", id, usuario, perfil)
                        // a criação otimista some da tela; atualização/exclusão rejeitada só fica marcada
                        if (m.entidade == "alimentos") banco.reconciliarAlimento(usuario, perfil, m.id.toString())
                        else if (m.operacao == OperacaoMutacao.CRIAR) { banco.registroSyncQueries.remover(usuario, perfil, m.entidade, m.id.toString()); banco.projetar(usuario, perfil, m.entidade, m.id.toString()) }
                        else banco.restaurarAnterior(usuario, perfil, m.entidade, m.id.toString())
                        // o código da regra fica guardado para a tela de pendências explicar o motivo
                        r.codigo?.let { banco.conflitoQueries.salvar(id, usuario, perfil, kotlinx.serialization.json.buildJsonObject { put("codigo", kotlinx.serialization.json.JsonPrimitive(it)) }.toString()) }
                    }
                }
            }
        }
        return true
    }
}
