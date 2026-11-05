package org.br.dietamais.data.repositorio

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import org.br.dietamais.data.local.BancoDieta
import org.br.dietamais.data.remoto.JsonContrato
import org.br.dietamais.dominio.pacientes.FixadosRecentes
import kotlin.uuid.Uuid

/** Pacientes fixados e recentes da nutri (MOB-67c). Só no aparelho; some com o logout junto com os outros dados da conta. */
interface PortaFixados {
    fun observar(): Flow<FixadosRecentes>
    fun alternarFixado(vinculo: Uuid)
    fun registrarAbertura(vinculo: Uuid)
}

/** Guarda em `registro_sync` (por conta): coleção `fixados-locais`. */
class FixadosRepositorio(private val banco: BancoDieta) : PortaFixados {
    override fun observar(): Flow<FixadosRecentes> = banco.registroSyncQueries.daSessao(COLECAO).asFlow().mapToList(Dispatchers.Default).map { linhas ->
        linhas.firstOrNull()?.let { try { JsonContrato.decodeFromString<FixadosRecentes>(it) } catch (_: Exception) { null } } ?: FixadosRecentes()
    }
    override fun alternarFixado(vinculo: Uuid) = mudar { it.alternarFixado(vinculo) }
    override fun registrarAbertura(vinculo: Uuid) = mudar { it.registrarAbertura(vinculo) }
    private fun mudar(bloco: (FixadosRecentes) -> FixadosRecentes) {
        val s = banco.sessaoQueries.atual().executeAsOneOrNull() ?: return
        val atual = banco.registroSyncQueries.daSessao(COLECAO).executeAsList().firstOrNull()
            ?.let { try { JsonContrato.decodeFromString<FixadosRecentes>(it) } catch (_: Exception) { null } } ?: FixadosRecentes()
        banco.registroSyncQueries.salvar(s.usuario_id, s.perfil, COLECAO, "lista", 0, 0, JsonContrato.encodeToString(bloco(atual)))
    }
    private companion object { const val COLECAO = "fixados-locais" }
}

/** Web (sem banco local): vale enquanto a página estiver aberta. */
class FixadosEmMemoria : PortaFixados {
    private val atual = MutableStateFlow(FixadosRecentes())
    override fun observar(): Flow<FixadosRecentes> = atual
    override fun alternarFixado(vinculo: Uuid) = atual.update { it.alternarFixado(vinculo) }
    override fun registrarAbertura(vinculo: Uuid) = atual.update { it.registrarAbertura(vinculo) }
}
