package org.br.dietamais.app

import kotlinx.coroutines.flow.map
import org.br.dietamais.data.remoto.ApiDieta
import org.br.dietamais.data.repositorio.AlimentosRepositorio
import org.br.dietamais.data.repositorio.PortaAlimentos
import org.br.dietamais.plataforma.EstadoConexao
import org.br.dietamais.ui.nutri.alimentos.AlimentosViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun moduloAlimentos(ambiente: AmbienteDieta?) = module {
    single<PortaAlimentos> { AlimentosRepositorio(get(), get()) }
    viewModel { AlimentosViewModel(get(), sessao = get<ApiDieta>().operacaoSessao,
        conectividade = ambiente?.conectividade?.estado?.map { it != EstadoConexao.INDISPONIVEL }) }
}
