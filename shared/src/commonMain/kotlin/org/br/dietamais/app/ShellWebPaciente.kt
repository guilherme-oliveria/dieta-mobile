package org.br.dietamais.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import org.br.dietamais.data.repositorio.PortaPdf
import org.br.dietamais.data.repositorio.RecursoPdf
import org.br.dietamais.recursos.Res
import org.br.dietamais.recursos.app_nome
import org.br.dietamais.recursos.auth_sair
import org.br.dietamais.recursos.pdf_dieta
import org.br.dietamais.ui.auth.acoesPrivacidade
import org.br.dietamais.ui.comum.BotaoPdf
import org.br.dietamais.ui.nutri.avaliacao.AvaliacoesViewModel
import org.br.dietamais.ui.notificacoes.AvisosRota
import org.br.dietamais.ui.notificacoes.AvisosViewModel
import org.br.dietamais.ui.paciente.anamnese.AnamnesePacienteRota
import org.br.dietamais.ui.paciente.anamnese.AnamnesePacienteViewModel
import org.br.dietamais.ui.paciente.compras.AcoesCompras
import org.br.dietamais.ui.paciente.compras.ComprasTela
import org.br.dietamais.ui.paciente.compras.ComprasViewModel
import org.br.dietamais.ui.paciente.dieta.DietaPacienteTela
import org.br.dietamais.ui.paciente.dieta.DietaPacienteViewModel
import org.br.dietamais.ui.paciente.evolucao.EvolucaoPacienteRota
import org.br.dietamais.ui.paciente.evolucao.EvolucaoViewModel
import org.br.dietamais.ui.paciente.hoje.AcoesHoje
import org.br.dietamais.ui.paciente.hoje.ConsultasPacienteViewModel
import org.br.dietamais.ui.paciente.hoje.HojeTela
import org.br.dietamais.ui.paciente.hoje.ProximaConsultaRota
import org.br.dietamais.ui.paciente.perfil.AcoesPerfil
import org.br.dietamais.ui.paciente.perfil.PerfilPacienteTela
import org.br.dietamais.ui.paciente.perfil.PerfilPacienteViewModel
import org.br.dietamais.ui.paciente.substituicao.TrocaTela
import org.br.dietamais.ui.privacidade.PrivacidadeCard
import org.br.dietamais.ui.privacidade.PrivacidadeViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Paciente na web (MOB-44): Hoje (com a anamnese), Dieta, Evolução, Compras e Perfil, e a central de avisos, tudo pela API. Os
 * lembretes locais ficam para o app; a tela do perfil avisa para instalá-lo.
 */
@Composable
fun ShellWebPaciente(nome: String, aoSair: () -> Unit, aoPrepararNavegacao: (androidx.navigation.NavHostController) -> Unit = {}) {
    val dono = remember { object : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() } }
    DisposableEffect(dono) { onDispose { dono.viewModelStore.clear() } }
    // um ViewModel compartilhado: Hoje e Dieta leem os mesmos dados
    val vm = koinViewModel<DietaPacienteViewModel>(viewModelStoreOwner = dono)
    val estado by vm.estado.collectAsState()
    org.br.dietamais.ui.paciente.dieta.RetornoDietaPaciente(estado, vm::fecharLista, { vm.abrirRefeicao(null) })
    val atalhos = remember { AtalhosWeb(PerfilNavegacao.PACIENTE) }
    Column(Modifier.fillMaxSize().then(atalhos.modificador)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(Res.string.app_nome), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f))
            AvisosRota(koinViewModel<AvisosViewModel>(viewModelStoreOwner = dono))
            Text(nome, style = MaterialTheme.typography.bodyMedium)
            TextButton(aoSair) { Text(stringResource(Res.string.auth_sair)) }
        }
        NavegacaoApp(PerfilNavegacao.PACIENTE, aoPrepararNavegacao = { atalhos.registrar(it, aoPrepararNavegacao) },
            conteudoHoje = {
                Column(Modifier.fillMaxSize()) {
                    AnamnesePacienteRota(koinViewModel<AnamnesePacienteViewModel>(viewModelStoreOwner = dono))
                    ProximaConsultaRota(koinViewModel<ConsultasPacienteViewModel>(viewModelStoreOwner = dono))
                    HojeTela(estado, AcoesHoje(vm::mudarDia, vm::marcar, vm::abrirTroca, vm::beberAgua), Modifier.weight(1f))
                }
                TrocaTela(estado, vm::escolherSubstituto, vm::confirmarTroca, vm::cancelarTroca)
            },
            conteudoDieta = {
                val pdf = koinInject<PortaPdf>()
                DietaPacienteTela(estado, vm::abrirRefeicao, vm::escolher, aoAbrirLista = vm::abrirLista,
                    acoesLista = org.br.dietamais.ui.paciente.substituicao.AcoesListaPaciente(vm::fecharLista, vm::buscarNaLista, vm::filtrarGrupoLista, vm::expandirGrupoLista, vm::tentarNovamente),
                    aoTrocar = vm::abrirTroca, aoMarcar = vm::marcar, aoTrocarRefeicao = vm::abrirTroca, pdf = { e -> e.dieta?.let { d ->
                    BotaoPdf(stringResource(Res.string.pdf_dieta), !e.semConexao, { pdf.link(RecursoPdf.DIETA, d.dietaId) }) } })
                TrocaTela(estado, vm::escolherSubstituto, vm::confirmarTroca, vm::cancelarTroca)
            },
            conteudoEvolucao = { EvolucaoPacienteRota(koinViewModel<EvolucaoViewModel>(viewModelStoreOwner = dono), koinViewModel<AvaliacoesViewModel>(viewModelStoreOwner = dono)) },
            conteudoCompras = {
                val compras = koinViewModel<ComprasViewModel>(viewModelStoreOwner = dono)
                val estadoCompras by compras.estado.collectAsState()
                ComprasTela(estadoCompras, AcoesCompras(compras::mudarDias, compras::marcar, compras::limparMarcas, compras::compartilhar))
            },
            conteudoPerfil = {
                val perfil = koinViewModel<PerfilPacienteViewModel>(viewModelStoreOwner = dono)
                val estadoPerfil by perfil.estado.collectAsState()
                val priv = koinViewModel<PrivacidadeViewModel>(viewModelStoreOwner = dono) { parametersOf(true) }
                val estadoPriv by priv.estado.collectAsState()
                androidx.compose.runtime.LaunchedEffect(estadoPriv.contaExcluida) { if (estadoPriv.contaExcluida) aoSair() }
                PerfilPacienteTela(estadoPerfil, AcoesPerfil(perfil::mudarLembretes, perfil::mudarTipo, perfil::mudarSilencioAtivo, perfil::mudarInicio,
                    perfil::mudarFim, perfil::salvar, perfil::gerarLink, perfil::revogarLink),
                    extras = { PrivacidadeCard(estadoPriv, acoesPrivacidade(priv), comDadosPessoais = true) })
            })
    }
}
