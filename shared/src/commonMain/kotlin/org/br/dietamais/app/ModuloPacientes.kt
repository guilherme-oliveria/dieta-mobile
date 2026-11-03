package org.br.dietamais.app

import org.br.dietamais.data.repositorio.PacientesRepositorio
import org.br.dietamais.data.repositorio.PortaPacientes
import org.br.dietamais.plataforma.Compartilhamento
import org.br.dietamais.ui.nutri.pacientes.PacientesViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import kotlinx.coroutines.flow.combine
import org.br.dietamais.plataforma.EstadoConexao
import org.br.dietamais.data.sync.ResultadoLeituraSync

fun moduloPacientes(compartilhamento: Compartilhamento, ambiente: AmbienteDieta? = null) = module {
    single<Compartilhamento> { compartilhamento }
    single<PortaPacientes> {
        if (ambiente != null) PacientesRepositorio(get(), get(), ambiente.sincronizador, ambiente::solicitarLeitura)
        else PacientesRepositorio(get(), get())
    }
    viewModel { PacientesViewModel(get(), get(), conectividade = ambiente?.let {
        combine(it.conectividade.estado, it.sincronizador.estado) { rede, sync ->
            rede != EstadoConexao.INDISPONIVEL && sync.resultado != ResultadoLeituraSync.SEM_REDE
        }
    }) }
}
