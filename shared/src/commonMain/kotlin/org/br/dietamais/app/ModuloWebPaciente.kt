package org.br.dietamais.app

import kotlinx.coroutines.flow.map
import org.br.dietamais.data.online.ComprasOnline
import org.br.dietamais.data.online.DietaPacienteOnline
import org.br.dietamais.data.online.EvolucaoOnline
import org.br.dietamais.data.online.PreferenciasOnline
import org.br.dietamais.data.online.RotinaOnline
import org.br.dietamais.data.online.VinculosOnline
import org.br.dietamais.data.remoto.ApiDieta
import org.br.dietamais.data.repositorio.CalendarioRepositorio
import org.br.dietamais.data.repositorio.PortaCalendario
import org.br.dietamais.data.repositorio.PortaCompras
import org.br.dietamais.data.repositorio.PortaDietaPaciente
import org.br.dietamais.data.repositorio.PortaEvolucao
import org.br.dietamais.data.repositorio.PortaPreferencias
import org.br.dietamais.data.repositorio.PortaRotina
import org.br.dietamais.data.repositorio.PortaVinculosLocais
import org.br.dietamais.plataforma.EstadoConexao
import org.br.dietamais.recursos.Res
import org.br.dietamais.recursos.compras_titulo_texto
import org.br.dietamais.ui.nutri.avaliacao.AvaliacoesViewModel
import org.br.dietamais.ui.paciente.compras.ComprasViewModel
import org.br.dietamais.ui.paciente.dieta.DietaPacienteViewModel
import org.br.dietamais.ui.paciente.evolucao.EvolucaoViewModel
import org.br.dietamais.ui.paciente.hoje.ConsultasPacienteViewModel
import org.br.dietamais.ui.paciente.perfil.PerfilPacienteViewModel
import org.jetbrains.compose.resources.getString
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** Telas do paciente na web (MOB-44): Portas só-API; sem lembretes locais (a tela avisa para instalar o app). */
fun moduloWebPaciente(ambiente: AmbienteWeb) = module {
    val online = ambiente.conectividade.estado.map { it != EstadoConexao.INDISPONIVEL }
    single<PortaRotina> { RotinaOnline(get<ApiDieta>(), ambiente.escopo) }
    single<PortaDietaPaciente> { DietaPacienteOnline(get()) }
    single<PortaEvolucao> { EvolucaoOnline(get(), get()) }
    single<PortaCompras> { ComprasOnline(get()) }
    single<PortaPreferencias> { PreferenciasOnline(get()) }
    single<PortaVinculosLocais> { VinculosOnline(get()) }
    single<PortaCalendario> { CalendarioRepositorio(get()) }
    viewModel { DietaPacienteViewModel(get(), get(), conectividade = online) }
    viewModel { ConsultasPacienteViewModel(get()) }
    viewModel { EvolucaoViewModel(get(), get<PortaRotina>()) }
    viewModel { ComprasViewModel(get(), get(), get(), { getString(Res.string.compras_titulo_texto) }) }
    viewModel { PerfilPacienteViewModel(get(), get(), get(), conectividade = online) }
}
