package org.br.dietamais.app

import kotlinx.serialization.Serializable

enum class PerfilNavegacao { NUTRICIONISTA, PACIENTE }

@Serializable
data object Entrada

@Serializable
data object GrafoNutri

@Serializable
data object GrafoPaciente

sealed interface RotaPaciente {
    @Serializable data object Hoje : RotaPaciente
    @Serializable data object Dieta : RotaPaciente
    @Serializable data object Compras : RotaPaciente
    @Serializable data object Evolucao : RotaPaciente
    @Serializable data object Perfil : RotaPaciente
}

sealed interface RotaNutri {
    @Serializable data object Inicio : RotaNutri
    @Serializable data object Pacientes : RotaNutri
    @Serializable data object ListaPacientes : RotaNutri
    @Serializable data class FichaPaciente(val pacienteId: String, val vinculoId: String? = null) : RotaNutri
    @Serializable data object Agenda : RotaNutri
    @Serializable data object Alimentos : RotaNutri
    @Serializable data object Conta : RotaNutri
}
