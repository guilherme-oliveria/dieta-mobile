package org.br.dietamais.app

import org.br.dietamais.data.repositorio.PacientesRepositorio
import org.br.dietamais.data.repositorio.PortaPacientes
import org.br.dietamais.plataforma.Compartilhamento
import org.br.dietamais.ui.nutri.pacientes.PacientesViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun moduloPacientes(compartilhamento: Compartilhamento) = module {
    single<Compartilhamento> { compartilhamento }
    single<PortaPacientes> { PacientesRepositorio(get(), get()) }
    viewModel { PacientesViewModel(get(), get()) }
}
