package org.br.dietamais.dominio.avisos

import org.br.dietamais.app.PerfilNavegacao
import kotlin.test.*
import kotlin.uuid.Uuid

class DestinoAvisoTest {
    private val ficha = Uuid.random().toString()

    @Test fun pacienteVaiParaDietaOuHoje() {
        assertEquals(DestinoAviso.Dieta, destinoDaRota(PerfilNavegacao.PACIENTE, "/dieta"))
        assertEquals(DestinoAviso.Hoje, destinoDaRota(PerfilNavegacao.PACIENTE, "/questionarios/${Uuid.random()}"))
        assertEquals(DestinoAviso.Hoje, destinoDaRota(PerfilNavegacao.PACIENTE, "/agenda"))
    }

    @Test fun nutriVaiParaAgendaOuFicha() {
        assertEquals(DestinoAviso.Agenda, destinoDaRota(PerfilNavegacao.NUTRICIONISTA, "/agenda"))
        assertEquals(DestinoAviso.Paciente(ficha), destinoDaRota(PerfilNavegacao.NUTRICIONISTA, "/pacientes/$ficha"))
    }

    @Test fun rotaDesconhecidaOuDeOutroPerfilNaoFazNada() {
        assertEquals(DestinoAviso.Nenhum, destinoDaRota(PerfilNavegacao.PACIENTE, null))
        assertEquals(DestinoAviso.Nenhum, destinoDaRota(PerfilNavegacao.PACIENTE, "/pacientes/$ficha"))
        assertEquals(DestinoAviso.Nenhum, destinoDaRota(PerfilNavegacao.PACIENTE, "/algo-novo"))
        assertEquals(DestinoAviso.Nenhum, destinoDaRota(PerfilNavegacao.NUTRICIONISTA, "/dieta"))
        assertEquals(DestinoAviso.Nenhum, destinoDaRota(PerfilNavegacao.NUTRICIONISTA, "/pacientes/nao-e-uuid"))
        assertEquals(DestinoAviso.Nenhum, destinoDaRota(PerfilNavegacao.NUTRICIONISTA, "/pacientes"))
        assertEquals(DestinoAviso.Nenhum, destinoDaRota(PerfilNavegacao.NUTRICIONISTA, ""))
    }
}
