package org.br.dietamais.app

import kotlinx.coroutines.flow.map
import org.br.dietamais.data.repositorio.ListaPacienteRepositorio
import org.br.dietamais.data.repositorio.PortaListaPaciente
import org.br.dietamais.plataforma.EstadoConexao
import org.br.dietamais.ui.nutri.listapaciente.ListaPacienteViewModel
import org.br.dietamais.recursos.Res
import org.br.dietamais.recursos.compras_titulo_texto
import org.br.dietamais.recursos.comi
import org.br.dietamais.recursos.lembrete_corpo
import org.br.dietamais.recursos.lembrete_titulo
import org.br.dietamais.recursos.pulei
import org.br.dietamais.recursos.troquei
import org.jetbrains.compose.resources.getString
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun moduloAvaliacoes(ambiente: AmbienteDieta?) = module {
    single { org.br.dietamais.data.sync.EscritaOffline(get(), aoEnfileirar = { ambiente?.solicitarLeitura() }) }
    single<org.br.dietamais.data.repositorio.PortaAvaliacoes> { org.br.dietamais.data.repositorio.AvaliacaoRepositorio(get(), get(), get()) }
    single<org.br.dietamais.data.repositorio.PortaEvolucao> { org.br.dietamais.data.repositorio.EvolucaoRepositorio(get()) }
    viewModel { org.br.dietamais.ui.paciente.evolucao.EvolucaoViewModel(get(), getOrNull<org.br.dietamais.data.repositorio.PortaRotina>()) }
    viewModel { org.br.dietamais.ui.nutri.avaliacao.AvaliacoesViewModel(get(), conectividade = ambiente?.conectividade?.estado?.map { it != EstadoConexao.INDISPONIVEL },
        protocolosPadrao = getOrNull<org.br.dietamais.data.repositorio.PortaProtocolosNutri>()?.let { org.br.dietamais.ui.nutri.protocolos.protocolosPadraoDe(it) }) }
    single<org.br.dietamais.data.repositorio.PortaExames> { org.br.dietamais.data.repositorio.ExamesRepositorio(get<org.br.dietamais.data.remoto.ApiDieta>()) }
    viewModel { org.br.dietamais.ui.nutri.avaliacao.ExamesNutriViewModel(get(), get(), conectividade = ambiente?.conectividade?.estado?.map { it != EstadoConexao.INDISPONIVEL }) }
    single<org.br.dietamais.data.repositorio.PortaConsultas> { org.br.dietamais.data.repositorio.ConsultaRepositorio(get(), get(), get()) }
    viewModel { org.br.dietamais.ui.nutri.agenda.AgendaViewModel(get(), get(), conectividade = ambiente?.conectividade?.estado?.map { it != EstadoConexao.INDISPONIVEL }) }
    viewModel { org.br.dietamais.ui.paciente.hoje.ConsultasPacienteViewModel(get()) }
    viewModel { org.br.dietamais.ui.nutri.painel.PainelViewModel(get(), get(), get(), get()) }
    single<org.br.dietamais.data.repositorio.PortaAvisos> { org.br.dietamais.data.repositorio.AvisosRepositorio(get(), get()) }
    viewModel { org.br.dietamais.ui.notificacoes.AvisosViewModel(get(), sincronizar = { ambiente?.sincronizarAgora(); Unit },
        aoNavegar = org.br.dietamais.app.SolicitacoesNavegacao::pedir) }
    single<org.br.dietamais.data.repositorio.PortaPendencias> { org.br.dietamais.data.repositorio.PendenciasRepositorio(get(), aoMudar = { ambiente?.solicitarLeitura() }) }
    viewModel { org.br.dietamais.ui.pendencias.PendenciasViewModel(get()) }
    single<org.br.dietamais.data.repositorio.PortaAnamnese> { org.br.dietamais.data.repositorio.AnamneseRepositorio(get(), get()) }
    viewModel { org.br.dietamais.ui.nutri.anamnese.AnamneseNutriViewModel(get(), conectividade = ambiente?.conectividade?.estado?.map { it != EstadoConexao.INDISPONIVEL }) }
    viewModel { org.br.dietamais.ui.paciente.anamnese.AnamnesePacienteViewModel(get(), sincronizar = { ambiente?.sincronizarAgora(); Unit },
        conectividade = ambiente?.conectividade?.estado?.map { it != EstadoConexao.INDISPONIVEL }) }
}

fun moduloListaPaciente(ambiente: AmbienteDieta?) = module {
    single<PortaListaPaciente> { ListaPacienteRepositorio(get<org.br.dietamais.data.remoto.ApiDieta>(), get<org.br.dietamais.data.local.BancoDieta>()) }
    viewModel { ListaPacienteViewModel(get(), get(), conectividade = ambiente?.conectividade?.estado?.map { it != EstadoConexao.INDISPONIVEL }) }
}

fun moduloDietas(ambiente: AmbienteDieta?) = module {
    single<org.br.dietamais.data.repositorio.PortaDietas> { org.br.dietamais.data.repositorio.DietasRepositorio(get<org.br.dietamais.data.remoto.ApiDieta>(), get<org.br.dietamais.data.local.BancoDieta>()) }
    viewModel { org.br.dietamais.ui.nutri.dieta.MontadorDietaViewModel(get(), get(), conectividade = ambiente?.conectividade?.estado?.map { it != EstadoConexao.INDISPONIVEL }) }
}

fun moduloPorcoes(ambiente: AmbienteDieta?) = module {
    single<org.br.dietamais.data.repositorio.PortaPorcoes> { org.br.dietamais.data.repositorio.PorcoesRepositorio(get<org.br.dietamais.data.remoto.ApiDieta>()) }
    viewModel { org.br.dietamais.ui.nutri.dieta.PlanejadorPorcoesViewModel(get(), get(), conectividade = ambiente?.conectividade?.estado?.map { it != EstadoConexao.INDISPONIVEL }) }
    single<org.br.dietamais.data.repositorio.PortaModelosDieta> { org.br.dietamais.data.repositorio.ModelosDietaRepositorio(get<org.br.dietamais.data.remoto.ApiDieta>(), get<org.br.dietamais.data.local.BancoDieta>()) }
    viewModel { org.br.dietamais.ui.nutri.dieta.ModelosDietaViewModel(get(), conectividade = ambiente?.conectividade?.estado?.map { it != EstadoConexao.INDISPONIVEL }) }
    viewModel { org.br.dietamais.ui.nutri.dieta.AplicarModeloViewModel(get(), get()) }
}

fun moduloDietaPaciente(ambiente: AmbienteDieta?) = module {
    single<org.br.dietamais.data.repositorio.PortaDietaPaciente> { org.br.dietamais.data.repositorio.DietaPacienteRepositorio(get(), get()) }
    single<org.br.dietamais.data.repositorio.PortaRotina> { org.br.dietamais.data.repositorio.RotinaRepositorio(get()) }
    viewModel { org.br.dietamais.ui.paciente.dieta.DietaPacienteViewModel(get(), get(), conectividade = ambiente?.conectividade?.estado?.map { it != EstadoConexao.INDISPONIVEL },
        aoGravar = { ambiente?.solicitarLeitura() }) }
}

fun moduloLembretes(notificacao: org.br.dietamais.plataforma.NotificacaoLocal?) = module {
    single<org.br.dietamais.data.repositorio.PortaPreferencias> { org.br.dietamais.data.repositorio.PreferenciasRepositorio(get(), get()) }
    single<org.br.dietamais.data.repositorio.PortaVinculosLocais> { org.br.dietamais.data.repositorio.VinculosLocaisRepositorio(get()) }
    single<org.br.dietamais.data.repositorio.PortaCalendario> { org.br.dietamais.data.repositorio.CalendarioRepositorio(get()) }
    viewModel { org.br.dietamais.ui.paciente.perfil.PerfilPacienteViewModel(get(), get(), get()) }
    single<org.br.dietamais.data.repositorio.PortaCompras> { org.br.dietamais.data.repositorio.ComprasRepositorio(get()) }
    viewModel { org.br.dietamais.ui.paciente.compras.ComprasViewModel(get(), get(), get(), { getString(Res.string.compras_titulo_texto) }) }
    if (notificacao != null) {
        single<org.br.dietamais.plataforma.NotificacaoLocal> { notificacao }
        single {
            org.br.dietamais.data.sync.AgendadorLembretes(get(), get(), get(), get(), {
                val modelo = getString(Res.string.lembrete_corpo)
                org.br.dietamais.dominio.rotina.TextosLembrete(getString(Res.string.lembrete_titulo), { nome: String -> modelo.replace("%1\$s", nome) },
                    org.br.dietamais.dominio.rotina.RotulosAcoes(getString(Res.string.comi), getString(Res.string.troquei), getString(Res.string.pulei)))
            })
        }
    }
}

fun moduloPerfilNutri(ambiente: AmbienteDieta?) = module {
    single<org.br.dietamais.data.repositorio.PortaPerfilNutri> { org.br.dietamais.data.repositorio.PerfilNutriRepositorio(get()) }
    viewModel { org.br.dietamais.ui.nutri.assinatura.PerfilNutriViewModel(get(), conectividade = ambiente?.conectividade?.estado?.map { it != EstadoConexao.INDISPONIVEL }) }
    single<org.br.dietamais.data.repositorio.PortaProtocolosNutri> { org.br.dietamais.data.repositorio.ProtocolosNutriRepositorio(get()) }
    viewModel { org.br.dietamais.ui.nutri.protocolos.ProtocolosNutriViewModel(get(), conectividade = ambiente?.conectividade?.estado?.map { it != EstadoConexao.INDISPONIVEL }) }
    single<org.br.dietamais.data.repositorio.PortaPrivacidade> { org.br.dietamais.data.repositorio.PrivacidadeRepositorio(get()) }
    viewModel { p -> org.br.dietamais.ui.privacidade.PrivacidadeViewModel(get(), conectividade = ambiente?.conectividade?.estado?.map { it != EstadoConexao.INDISPONIVEL }, exportacaoDisponivel = p.get()) }
    single<org.br.dietamais.data.repositorio.PortaPdf> { org.br.dietamais.data.repositorio.PdfRepositorio(get()) }
    single<org.br.dietamais.data.repositorio.PortaAssinatura> { org.br.dietamais.data.repositorio.AssinaturaRepositorio(get()) }
    viewModel { org.br.dietamais.ui.nutri.assinatura.AssinaturaViewModel(get(), conectividade = ambiente?.conectividade?.estado?.map { it != EstadoConexao.INDISPONIVEL }) }
}
