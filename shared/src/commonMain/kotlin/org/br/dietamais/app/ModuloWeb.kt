package org.br.dietamais.app

import org.br.dietamais.data.online.AnamneseOnline
import org.br.dietamais.data.online.AvisosOnline
import org.br.dietamais.data.repositorio.PortaAnamnese
import org.br.dietamais.data.repositorio.PortaAvisos
import org.br.dietamais.ui.notificacoes.AvisosViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun moduloWeb(ambiente: AmbienteWeb) = module {
    single { ambiente.api }
    single { ambiente.conectividade }
    // avisos e anamnese servem aos dois perfis
    single<PortaAvisos> { AvisosOnline(get()) }
    single<PortaAnamnese> { AnamneseOnline(get(), ambiente.escopo) }
    viewModel { AvisosViewModel(get(), aoNavegar = SolicitacoesNavegacao::pedir) }
}
