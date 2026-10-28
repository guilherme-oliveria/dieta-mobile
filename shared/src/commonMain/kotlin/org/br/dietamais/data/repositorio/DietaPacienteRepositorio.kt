package org.br.dietamais.data.repositorio

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.json.encodeToJsonElement
import org.br.dietamais.data.local.BancoDieta
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import kotlin.uuid.Uuid

data class DadosDietaPaciente(val snapshots: List<DietaSnapshotResposta> = emptyList(), val emUso: DietaEmUsoResposta? = null)

interface PortaDietaPaciente {
    /** Lê só o banco local (preenchido pelo sync). */
    fun observar(): Flow<DadosDietaPaciente>
    /** Escolha de dieta em uso: exige rede (integracao.md). */
    suspend fun selecionar(dietaId: Uuid, versao: Long): DietaEmUsoResposta
}

class DietaPacienteRepositorio(private val api: ApiDieta, private val banco: BancoDieta) : PortaDietaPaciente {
    override fun observar(): Flow<DadosDietaPaciente> = combine(
        banco.registroSyncQueries.daSessao("snapshots").asFlow().mapToList(Dispatchers.Default),
        banco.registroSyncQueries.daSessao("dietaEmUso").asFlow().mapToList(Dispatchers.Default)) { snapshots, emUso ->
        try { DadosDietaPaciente(snapshots.map { JsonContrato.decodeFromString<DietaSnapshotResposta>(it) },
            emUso.firstNotNullOfOrNull { JsonContrato.decodeFromString<DietaEmUsoResposta>(it) }) }
        catch (_: Exception) { throw RespostaInvalida() }
    }

    override suspend fun selecionar(dietaId: Uuid, versao: Long): DietaEmUsoResposta {
        val contexto = api.operacaoSessao.value?.takeIf { it.contexto.perfil == Perfil.PACIENTE } ?: throw SessaoAlterada()
        val resposta = api.substituir<DietaEmUsoResposta>("eu/dieta-em-uso", JsonContrato.encodeToJsonElement(SelecionarDietaEmUsoRequisicao(dietaId)), versao)
        if (api.operacaoSessao.value != contexto) throw SessaoAlterada()
        return resposta
    }
}
