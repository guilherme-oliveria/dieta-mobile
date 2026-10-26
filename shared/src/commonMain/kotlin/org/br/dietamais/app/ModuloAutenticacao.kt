package org.br.dietamais.app

import org.br.dietamais.data.repositorio.AutenticacaoRepositorio
import org.br.dietamais.data.repositorio.PortaAutenticacao
import org.br.dietamais.plataforma.IdentidadeDispositivo
import org.br.dietamais.ui.auth.AutenticacaoViewModel
import org.br.dietamais.ui.auth.TermosCadastro
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun moduloAutenticacao(identidade: IdentidadeDispositivo, termos: TermosCadastro?) = module {
    single { identidade }
    single<PortaAutenticacao> { AutenticacaoRepositorio(get(), get()) }
    viewModel { AutenticacaoViewModel(get(), termos) }
}
