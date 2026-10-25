package org.br.dietamais.app

import org.br.dietamais.data.local.BancoLocal
import org.br.dietamais.data.sync.FilaMutacoes
import org.br.dietamais.plataforma.FabricaDriver
import org.koin.dsl.module
import org.koin.dsl.onClose

fun moduloBancoLocal(fabrica: FabricaDriver) = module {
    single { BancoLocal(fabrica.criar()) } onClose { it?.close() }
    single { get<BancoLocal>().banco }
    single { FilaMutacoes(get()) }
}
