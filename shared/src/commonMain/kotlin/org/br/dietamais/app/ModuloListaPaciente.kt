package org.br.dietamais.app

import kotlinx.coroutines.flow.map
import org.br.dietamais.data.repositorio.ListaPacienteRepositorio
import org.br.dietamais.data.repositorio.PortaListaPaciente
import org.br.dietamais.plataforma.EstadoConexao
import org.br.dietamais.ui.nutri.listapaciente.ListaPacienteViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun moduloListaPaciente(ambiente: AmbienteDieta?) = module {
    single<PortaListaPaciente> { ListaPacienteRepositorio(get(), get()) }
    viewModel { ListaPacienteViewModel(get(), get(), conectividade = ambiente?.conectividade?.estado?.map { it != EstadoConexao.INDISPONIVEL }) }
}

fun moduloDietas(ambiente: AmbienteDieta?) = module {
    single<org.br.dietamais.data.repositorio.PortaDietas> { org.br.dietamais.data.repositorio.DietasRepositorio(get(), get()) }
    viewModel { org.br.dietamais.ui.nutri.dieta.MontadorDietaViewModel(get(), get(), conectividade = ambiente?.conectividade?.estado?.map { it != EstadoConexao.INDISPONIVEL }) }
}

fun moduloDietaPaciente(ambiente: AmbienteDieta?) = module {
    single<org.br.dietamais.data.repositorio.PortaDietaPaciente> { org.br.dietamais.data.repositorio.DietaPacienteRepositorio(get(), get()) }
    single<org.br.dietamais.data.repositorio.PortaRotina> { org.br.dietamais.data.repositorio.RotinaRepositorio(get()) }
    viewModel { org.br.dietamais.ui.paciente.dieta.DietaPacienteViewModel(get(), get(), conectividade = ambiente?.conectividade?.estado?.map { it != EstadoConexao.INDISPONIVEL },
        aoGravar = { ambiente?.solicitarLeitura() }) }
}
