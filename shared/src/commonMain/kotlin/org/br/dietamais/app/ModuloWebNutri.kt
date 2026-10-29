package org.br.dietamais.app

import kotlinx.coroutines.flow.map
import org.br.dietamais.data.online.AlimentosOnline
import org.br.dietamais.data.remoto.ApiDieta
import org.br.dietamais.data.online.PacientesOnline
import org.br.dietamais.data.repositorio.AssinaturaRepositorio
import org.br.dietamais.data.repositorio.DietasRepositorio
import org.br.dietamais.data.repositorio.PerfilNutriRepositorio
import org.br.dietamais.data.repositorio.PortaAssinatura
import org.br.dietamais.data.repositorio.PortaPerfilNutri
import org.br.dietamais.data.repositorio.PortaPrivacidade
import org.br.dietamais.data.repositorio.PrivacidadeRepositorio
import org.br.dietamais.ui.nutri.assinatura.AssinaturaViewModel
import org.br.dietamais.ui.nutri.assinatura.PerfilNutriViewModel
import org.br.dietamais.ui.privacidade.PrivacidadeViewModel
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
    // Conta da nutri: estas Portas já eram só-API
    single<PortaPerfilNutri> { PerfilNutriRepositorio(get<ApiDieta>()) }
    single<PortaPrivacidade> { PrivacidadeRepositorio(get<ApiDieta>()) }
    single<PortaAssinatura> { AssinaturaRepositorio(get<ApiDieta>()) }
    viewModel { PerfilNutriViewModel(get(), conectividade = online) }
    viewModel { p -> PrivacidadeViewModel(get(), conectividade = online, exportacaoDisponivel = p.get()) }
    viewModel { AssinaturaViewModel(get(), conectividade = online) }
    viewModel { PacientesViewModel(get(), get(), conectividade = online) }
    viewModel { MontadorDietaViewModel(get(), get(), conectividade = online) }
}
