package org.br.dietamais.app

import org.br.dietamais.data.remoto.dto.DispositivoRequisicao
import org.br.dietamais.data.repositorio.ConvitesRepositorio
import org.br.dietamais.data.repositorio.PortaConvites
import org.br.dietamais.plataforma.Config
import org.br.dietamais.plataforma.IdentidadeDispositivo
import org.br.dietamais.plataforma.plataformaApi
import org.br.dietamais.ui.auth.convite.ConviteViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun moduloConvites(identidade: IdentidadeDispositivo) = module {
    single<PortaConvites> { ConvitesRepositorio(get()) {
        DispositivoRequisicao(identidade.obter(), plataformaApi(), "Dieta+ ${plataformaApi().name}")
    } }
    viewModel { ConviteViewModel(get(), get<Config>().DOMINIOS_CONVITE) }
}
