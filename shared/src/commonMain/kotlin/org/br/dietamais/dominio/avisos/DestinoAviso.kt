package org.br.dietamais.dominio.avisos

import org.br.dietamais.app.PerfilNavegacao
import kotlin.uuid.Uuid

sealed interface DestinoAviso {
    data object Hoje : DestinoAviso
    data object Dieta : DestinoAviso
    data object Agenda : DestinoAviso
    /** [fichaId] é o id da ficha na rota `/pacientes/{id}`. */
    data class Paciente(val fichaId: String) : DestinoAviso
    data object Nenhum : DestinoAviso
}

/**
 * Traduz a `rota` do aviso (contrato-fase-4.md, BE-24) para uma tela do perfil. Rota desconhecida ou de outro perfil
 * não faz nada: o servidor pode evoluir antes do app.
 */
fun destinoDaRota(perfil: PerfilNavegacao, rota: String?): DestinoAviso {
    val partes = rota?.trim()?.split('/')?.filter { it.isNotEmpty() }.orEmpty()
    val primeiro = partes.firstOrNull() ?: return DestinoAviso.Nenhum
    return when (perfil) {
        PerfilNavegacao.PACIENTE -> when (primeiro) {
            "dieta" -> DestinoAviso.Dieta
            "questionarios", "agenda" -> DestinoAviso.Hoje // o questionário e a próxima consulta aparecem no Hoje
            else -> DestinoAviso.Nenhum
        }
        PerfilNavegacao.NUTRICIONISTA -> when {
            primeiro == "agenda" -> DestinoAviso.Agenda
            primeiro == "pacientes" && partes.size == 2 && runCatching { Uuid.parse(partes[1]) }.isSuccess -> DestinoAviso.Paciente(partes[1])
            else -> DestinoAviso.Nenhum
        }
    }
}
