package org.br.dietamais.data.online

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.json.encodeToJsonElement
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.*
import kotlin.uuid.Uuid

/** Itens marcados da lista de compras: só em memória, por conta; recarregar a página zera (RN-ROT-04, web.md). */
class ComprasOnline(private val api: ApiDieta) : PortaCompras {
    private class Marcas(val dono: Uuid, val ids: Set<Uuid>)
    private val guardado = MutableStateFlow<Marcas?>(null)
    override fun observarMarcados(): Flow<Set<Uuid>> = combine(api.sessao, guardado) { sessao, marcas ->
        if (sessao != null && marcas?.dono == sessao.usuarioId) marcas.ids else emptySet()
    }
    override fun marcar(alimentoId: Uuid, marcado: Boolean) {
        val dono = (api.sessao.value ?: throw SessaoAlterada()).usuarioId
        val atual = guardado.value?.takeIf { it.dono == dono }?.ids.orEmpty()
        guardado.value = Marcas(dono, if (marcado) atual + alimentoId else atual - alimentoId)
    }
    override fun limpar() {
        val dono = (api.sessao.value ?: throw SessaoAlterada()).usuarioId
        guardado.value = Marcas(dono, emptySet())
    }
}

/** Preferências de notificação do paciente pela API. Lembretes locais não existem na web (a tela avisa para instalar o app). */
class PreferenciasOnline(private val api: ApiDieta) : PortaPreferencias {
    private class Dados(val dono: Uuid, val resposta: PreferenciasNotificacaoResposta)
    private val guardado = MutableStateFlow<Dados?>(null)
    override fun observar(): Flow<PreferenciasNotificacaoResposta?> = combine(api.sessao, guardado) { sessao, dados ->
        if (sessao != null && dados?.dono == sessao.usuarioId) dados.resposta else null
    }
    override fun observarLembretes(): Flow<Boolean> = flowOf(false)
    override fun definirLembretes(ativo: Boolean) = Unit
    override suspend fun atualizar(): PreferenciasNotificacaoResposta {
        val esperado = api.operacaoSessao.value ?: throw SessaoAlterada()
        val resposta = api.obter<PreferenciasNotificacaoResposta>("eu/preferencias-notificacao")
        guardar(esperado, resposta); return resposta
    }
    override suspend fun salvar(requisicao: AtualizarPreferenciasRequisicao): PreferenciasNotificacaoResposta {
        val esperado = api.operacaoSessao.value ?: throw SessaoAlterada()
        val versao = guardado.value?.takeIf { it.dono == esperado.contexto.usuarioId }?.resposta?.versao ?: 0L
        val resposta = api.substituir<PreferenciasNotificacaoResposta>("eu/preferencias-notificacao", JsonContrato.encodeToJsonElement(requisicao), versao)
        guardar(esperado, resposta); return resposta
    }
    private fun guardar(esperado: OperacaoSessao, resposta: PreferenciasNotificacaoResposta) {
        if (api.operacaoSessao.value != esperado) throw SessaoAlterada()
        guardado.value = Dados(esperado.contexto.usuarioId, resposta)
    }
}

/** Vínculos do paciente: vêm de `GET /eu` a cada abertura. */
class VinculosOnline(private val api: ApiDieta) : PortaVinculosLocais {
    override fun observarVinculos(): Flow<List<VinculoResposta>> = flow {
        val esperado = api.operacaoSessao.value ?: throw SessaoAlterada()
        val eu = api.obter<EuResposta>("eu")
        if (api.operacaoSessao.value != esperado) throw SessaoAlterada()
        emit(eu.vinculos.sortedBy { it.nutricionista.nome.lowercase() })
    }
}

/**
 * Evolução: o paciente vê as avaliações compartilhadas e os pesos em casa (da rotina); a nutri vê as avaliações da ficha aberta.
 * Os pesos de casa só chegam à nutri pelo sync, que a web não tem; o gráfico dela mostra só as avaliações.
 */
class EvolucaoOnline(private val avaliacoes: PortaAvaliacoes, private val rotina: PortaRotina) : PortaEvolucao {
    override fun observar(pacienteId: Uuid?): Flow<DadosEvolucao> =
        if (pacienteId == null) combine(avaliacoes.observar(null), rotina.observar()) { a, r -> DadosEvolucao(a, r.pesos) }
        else avaliacoes.observar(pacienteId).map { DadosEvolucao(it) }.onStart {
            try { avaliacoes.atualizar(pacienteId) } catch (c: CancellationException) { throw c } catch (_: Exception) { }
        }
}
