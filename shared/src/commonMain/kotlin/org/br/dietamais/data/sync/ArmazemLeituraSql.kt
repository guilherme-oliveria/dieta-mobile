package org.br.dietamais.data.sync

import org.br.dietamais.data.local.BancoDieta
import org.br.dietamais.data.remoto.JsonContrato
import org.br.dietamais.data.remoto.RespostaInvalida
import org.br.dietamais.data.remoto.SessaoAlterada
import org.br.dietamais.data.remoto.dto.*
import kotlin.uuid.Uuid

class ArmazemLeituraSql(private val banco: BancoDieta, private val contextoAtual: () -> ContextoLeituraSync?) : ArmazemLeituraSync {
    override fun cursor(contexto: ContextoLeituraSync): String? = banco.transactionWithResult {
        conferir(contexto)
        banco.cursorSyncQueries.buscar(contexto.usuarioId.toString(), contexto.perfil.name).executeAsOneOrNull().also {
            if (it.isNullOrBlank()) banco.pacienteCacheQueries.prepararBootstrap(contexto.usuarioId.toString(), contexto.perfil.name)
        }
    }

    override fun aplicar(contexto: ContextoLeituraSync, pagina: SyncResposta) = banco.transaction {
        conferir(contexto)
        if (pagina.cursor.isBlank() || (contexto.perfil == Perfil.PACIENTE && pagina.mudancas.pacientes.isNotEmpty())) throw RespostaInvalida()
        pagina.mudancas.pacientes.forEach { mudanca ->
            registrar(contexto, "pacientes", mudanca, PacienteResposta::id) { dados ->
                banco.pacienteCacheQueries.limparPaciente(contexto.usuarioId.toString(), contexto.perfil.name, mudanca.id.toString())
                dados?.let { banco.pacienteCacheQueries.salvar(contexto.usuarioId.toString(), contexto.perfil.name,
                    it.vinculoId.toString(), it.id.toString(), JsonContrato.encodeToString(it)) }
            }
        }
        pagina.mudancas.vinculos.forEach { registrar(contexto, "vinculos", it, VinculoResposta::id) }
        pagina.mudancas.nutris.forEach { registrar(contexto, "nutris", it, NutricionistaResumoResposta::id) }
        pagina.mudancas.grupos.forEach { registrar(contexto, "grupos", it, GrupoAlimentoResposta::id) }
        pagina.mudancas.alimentos.forEach { registrar(contexto, "alimentos", it, AlimentoResposta::id) }
        pagina.mudancas.listas.forEach { registrar(contexto, "listas", it, null) }
        pagina.mudancas.dietas.forEach { registrar(contexto, "dietas", it, DietaResposta::id) }
        pagina.mudancas.snapshots.forEach { registrar(contexto, "snapshots", it, DietaSnapshotResposta::id) }
        pagina.mudancas.dietaEmUso.forEach { registrar(contexto, "dietaEmUso", it, null) }
        pagina.mudancas.checkins.forEach { registrar(contexto, "checkins", it, CheckinResposta::id) }
        pagina.mudancas.agua.forEach { registrar(contexto, "agua", it, AguaResposta::id) }
        pagina.mudancas.pesos.forEach { registrar(contexto, "pesos", it, PesoResposta::id) }
        pagina.mudancas.avaliacoes.forEach { registrar(contexto, "avaliacoes", it, AvaliacaoResposta::id) }
        if (!pagina.temMais) banco.pacienteCacheQueries.concluirBootstrap(contexto.usuarioId.toString(), contexto.perfil.name)
        conferir(contexto)
        banco.cursorSyncQueries.salvar(contexto.usuarioId.toString(), contexto.perfil.name, pagina.cursor)
    }

    private inline fun <reified T> registrar(contexto: ContextoLeituraSync, colecao: String, mudanca: MudancaSyncResposta<T>,
        noinline id: ((T) -> Uuid)?, aoAplicar: (T?) -> Unit = {}) {
        val dados = mudanca.dados
        if (mudanca.versao < 0 || (mudanca.excluido && dados != null) || (!mudanca.excluido && dados == null) ||
            (dados != null && id != null && id(dados) != mudanca.id)) throw RespostaInvalida()
        val anterior = banco.registroSyncQueries.revisao(contexto.usuarioId.toString(), contexto.perfil.name, colecao, mudanca.id.toString()).executeAsOneOrNull()
        if (anterior != null && mudanca.versao < anterior) return
        banco.registroSyncQueries.salvar(contexto.usuarioId.toString(), contexto.perfil.name, colecao, mudanca.id.toString(),
            mudanca.versao, if (mudanca.excluido) 1L else 0L, dados?.let { JsonContrato.encodeToString(it) })
        aoAplicar(dados)
    }

    private fun conferir(contexto: ContextoLeituraSync) {
        val sessao = banco.sessaoQueries.atual().executeAsOneOrNull()
        if (contextoAtual() != contexto || sessao?.usuario_id != contexto.usuarioId.toString() || sessao.perfil != contexto.perfil.name) throw SessaoAlterada()
    }
}
