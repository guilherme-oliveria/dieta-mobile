package org.br.dietamais.app

import org.br.dietamais.data.remoto.ApiDieta
import org.br.dietamais.plataforma.CofreSessao
import org.br.dietamais.plataforma.plataformaApi
import org.koin.dsl.module
import org.koin.dsl.onClose

fun moduloApi(cofre: CofreSessao, versaoApp: String) = module {
    single<CofreSessao> { cofre }
    single { ApiDieta(get(), versaoApp, plataformaApi(), get(), get()) } onClose { it?.close() }
}
