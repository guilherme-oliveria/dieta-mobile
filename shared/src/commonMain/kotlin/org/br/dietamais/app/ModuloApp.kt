package org.br.dietamais.app

import org.br.dietamais.plataforma.Config
import org.koin.dsl.module

fun moduloApp(config: Config) = module {
    single { config }
}
