package org.br.dietamais.app

import kotlinx.coroutines.flow.map
import org.br.dietamais.data.repositorio.ListaPacienteRepositorio
import org.br.dietamais.data.repositorio.PortaListaPaciente
import org.br.dietamais.plataforma.EstadoConexao
import org.br.dietamais.ui.nutri.listapaciente.ListaPacienteViewModel
import org.br.dietamais.recursos.Res
import org.br.dietamais.recursos.compras_titulo_texto
import org.br.dietamais.recursos.lembrete_corpo
import org.br.dietamais.recursos.lembrete_titulo
import org.jetbrains.compose.resources.getString
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

fun moduloLembretes(notificacao: org.br.dietamais.plataforma.NotificacaoLocal?) = module {
    single<org.br.dietamais.data.repositorio.PortaPreferencias> { org.br.dietamais.data.repositorio.PreferenciasRepositorio(get(), get()) }
    single<org.br.dietamais.data.repositorio.PortaVinculosLocais> { org.br.dietamais.data.repositorio.VinculosLocaisRepositorio(get()) }
    single<org.br.dietamais.data.repositorio.PortaCalendario> { org.br.dietamais.data.repositorio.CalendarioRepositorio(get()) }
    viewModel { org.br.dietamais.ui.paciente.perfil.PerfilPacienteViewModel(get(), get(), get()) }
    single<org.br.dietamais.data.repositorio.PortaCompras> { org.br.dietamais.data.repositorio.ComprasRepositorio(get()) }
    viewModel { org.br.dietamais.ui.paciente.compras.ComprasViewModel(get(), get(), get(), { getString(Res.string.compras_titulo_texto) }) }
    if (notificacao != null) {
        single<org.br.dietamais.plataforma.NotificacaoLocal> { notificacao }
        single {
            org.br.dietamais.data.sync.AgendadorLembretes(get(), get(), get(), get(), {
                getString(Res.string.lembrete_titulo) to
                    getString(Res.string.lembrete_corpo).let { modelo -> { nome: String -> modelo.replace("%1\$s", nome) } }
            })
        }
    }
}
