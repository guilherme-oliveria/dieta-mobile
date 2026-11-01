package org.br.dietamais.app

import kotlinx.coroutines.flow.map
import org.br.dietamais.data.online.AlimentosOnline
import org.br.dietamais.data.online.AvaliacoesOnline
import org.br.dietamais.data.online.ConsultasOnline
import org.br.dietamais.data.repositorio.ListaPacienteRepositorio
import org.br.dietamais.data.repositorio.PortaAvaliacoes
import org.br.dietamais.data.repositorio.PortaConsultas
import org.br.dietamais.data.repositorio.PortaListaPaciente
import org.br.dietamais.ui.nutri.agenda.AgendaViewModel
import org.br.dietamais.ui.nutri.alimentos.AlimentosViewModel
import org.br.dietamais.ui.nutri.avaliacao.AvaliacoesViewModel
import org.br.dietamais.ui.nutri.listapaciente.ListaPacienteViewModel
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
import org.br.dietamais.ui.nutri.anamnese.AnamneseNutriViewModel
import org.br.dietamais.ui.nutri.painel.PainelViewModel
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
    single<org.br.dietamais.data.repositorio.PortaProtocolosNutri> { org.br.dietamais.data.repositorio.ProtocolosNutriRepositorio(get<ApiDieta>()) }
    viewModel { org.br.dietamais.ui.nutri.protocolos.ProtocolosNutriViewModel(get(), conectividade = online) }
    viewModel { p -> PrivacidadeViewModel(get(), conectividade = online, exportacaoDisponivel = p.get()) }
    viewModel { AssinaturaViewModel(get(), conectividade = online) }
    single<PortaConsultas> { ConsultasOnline(get()) }
    single<PortaAvaliacoes> { AvaliacoesOnline(get()) }
    single<PortaListaPaciente> { ListaPacienteRepositorio(get<ApiDieta>()) { } }
    viewModel { AgendaViewModel(get(), get(), conectividade = online) }
    viewModel { AvaliacoesViewModel(get(), conectividade = online, protocolosPadrao = getOrNull<org.br.dietamais.data.repositorio.PortaProtocolosNutri>()?.let { org.br.dietamais.ui.nutri.protocolos.protocolosPadraoDe(it) }) }
    viewModel { AlimentosViewModel(get(), sessao = get<ApiDieta>().operacaoSessao, conectividade = online) }
    viewModel { ListaPacienteViewModel(get(), get(), conectividade = online) }
    viewModel { PacientesViewModel(get(), get(), conectividade = online) }
    viewModel { MontadorDietaViewModel(get(), get(), conectividade = online) }
    single<org.br.dietamais.data.repositorio.PortaPorcoes> { org.br.dietamais.data.repositorio.PorcoesRepositorio(get<ApiDieta>()) }
    viewModel { org.br.dietamais.ui.nutri.dieta.PlanejadorPorcoesViewModel(get(), get(), conectividade = online) }
    single<org.br.dietamais.data.repositorio.PortaModelosDieta> { org.br.dietamais.data.repositorio.ModelosDietaRepositorio(get<ApiDieta>()) { } }
    viewModel { org.br.dietamais.ui.nutri.dieta.ModelosDietaViewModel(get(), conectividade = online) }
    viewModel { org.br.dietamais.ui.nutri.dieta.AplicarModeloViewModel(get(), get()) }
    viewModel { PainelViewModel(get(), get(), get(), get()) }
    viewModel { AnamneseNutriViewModel(get(), conectividade = online) }
}
