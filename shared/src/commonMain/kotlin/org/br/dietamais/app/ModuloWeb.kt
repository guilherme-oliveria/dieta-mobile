package org.br.dietamais.app

import org.koin.dsl.module

fun moduloWeb(ambiente: AmbienteWeb) = module {
    single { ambiente.api }
    single { ambiente.conectividade }
}
