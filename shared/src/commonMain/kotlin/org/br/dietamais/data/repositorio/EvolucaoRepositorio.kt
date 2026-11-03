package org.br.dietamais.data.repositorio

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.br.dietamais.data.local.BancoDieta
import org.br.dietamais.data.remoto.JsonContrato
import org.br.dietamais.data.remoto.RespostaInvalida
import org.br.dietamais.data.remoto.dto.AvaliacaoResposta
import org.br.dietamais.data.remoto.dto.PesoResposta
import kotlin.uuid.Uuid

data class DadosEvolucao(val avaliacoes: List<AvaliacaoResposta> = emptyList(), val pesosEmCasa: List<PesoResposta> = emptyList())

interface PortaEvolucao {
    /** Do banco local. Nutri passa o id da ficha; paciente passa nulo e vê só o que o servidor compartilhou. */
    fun observar(pacienteId: Uuid?): Flow<DadosEvolucao>
}

class EvolucaoRepositorio(private val banco: BancoDieta) : PortaEvolucao {
    override fun observar(pacienteId: Uuid?): Flow<DadosEvolucao> = combine(
        banco.registroSyncQueries.daSessao("avaliacoes").asFlow().mapToList(Dispatchers.Default),
        banco.registroSyncQueries.daSessao("pesos").asFlow().mapToList(Dispatchers.Default)) { avaliacoes, pesos ->
        try {
            DadosEvolucao(
                avaliacoes.map { JsonContrato.decodeFromString<AvaliacaoResposta>(it) }.filter { pacienteId == null || it.pacienteId == pacienteId },
                pesos.map { JsonContrato.decodeFromString<PesoResposta>(it) }.filter { pacienteId == null || it.pacienteId == pacienteId })
        } catch (_: Exception) { throw RespostaInvalida() }
    }
}
