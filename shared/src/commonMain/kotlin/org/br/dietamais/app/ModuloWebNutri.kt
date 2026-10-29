package org.br.dietamais.app

import kotlinx.coroutines.flow.map
import org.br.dietamais.data.online.AlimentosOnline
import org.br.dietamais.data.remoto.ApiDieta
import org.br.dietamais.data.online.PacientesOnline
import org.br.dietamais.data.repositorio.DietasRepositorio
import org.br.dietamais.data.repositorio.PdfRepositorio
import org.br.dietamais.data.repositorio.PortaAlimentos
import org.br.dietamais.data.repositorio.PortaDietas
import org.br.dietamais.data.repositorio.PortaPacientes
import org.br.dietamais.data.repositorio.PortaPdf
import org.br.dietamais.plataforma.Compartilhamento
import org.br.dietamais.plataforma.EstadoConexao
import org.br.dietamais.ui.nutri.dieta.MontadorDietaViewModel
import org.br.dietamais.ui.nutri.pacientes.PacientesViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** Telas da nutri na web (MOB-43): Portas só-API, sem banco, fila nem sync. */
fun moduloWebNutri(ambiente: AmbienteWeb, compartilhamento: Compartilhamento) = module {
    val online = ambiente.conectividade.estado.map { it != EstadoConexao.INDISPONIVEL }
    single<Compartilhamento> { compartilhamento }
    single<PortaPacientes> { PacientesOnline(get()) }
    single<PortaAlimentos> { AlimentosOnline(get()) }
    single<PortaDietas> { DietasRepositorio(get<ApiDieta>()) { } }
    single<PortaPdf> { PdfRepositorio(get()) }
    viewModel { PacientesViewModel(get(), get(), conectividade = online) }
    viewModel { MontadorDietaViewModel(get(), get(), conectividade = online) }
}
