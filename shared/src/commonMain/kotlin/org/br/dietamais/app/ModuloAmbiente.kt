package org.br.dietamais.app

import org.br.dietamais.data.sync.FilaMutacoes
import org.koin.dsl.module

fun moduloAmbiente(ambiente: AmbienteDieta) = module {
    single { ambiente.bancoLocal }
    single { ambiente.banco }
    single { ambiente.api }
    single { ambiente.sincronizador }
    single { FilaMutacoes(get()) }
}
