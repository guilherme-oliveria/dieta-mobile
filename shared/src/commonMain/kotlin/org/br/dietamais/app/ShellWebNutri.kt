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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import org.br.dietamais.recursos.web_aba_no_app
import org.br.dietamais.ui.comum.EstadoVazio
import org.br.dietamais.ui.notificacoes.AvisosRota
import org.br.dietamais.ui.notificacoes.AvisosViewModel
import org.br.dietamais.ui.nutri.anamnese.AnamneseNutriRota
import org.br.dietamais.ui.nutri.anamnese.AnamneseNutriViewModel
import org.br.dietamais.ui.nutri.painel.PainelRota
import org.br.dietamais.ui.nutri.painel.PainelViewModel
import org.br.dietamais.ui.paciente.evolucao.EvolucaoRota
import org.br.dietamais.ui.paciente.evolucao.EvolucaoViewModel
import org.br.dietamais.ui.nutri.agenda.AgendaRota
import org.br.dietamais.ui.nutri.agenda.AgendaViewModel
import org.br.dietamais.ui.nutri.alimentos.AlimentosViewModel
import org.br.dietamais.ui.nutri.avaliacao.AvaliacoesViewModel
import org.br.dietamais.ui.nutri.alimentos.AlimentosRota
import org.br.dietamais.ui.nutri.avaliacao.AvaliacoesNutriRota
import org.br.dietamais.ui.nutri.listapaciente.ListaPacienteTela
import org.br.dietamais.ui.nutri.listapaciente.ListaPacienteViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import org.br.dietamais.ui.auth.acoesPrivacidade
import org.br.dietamais.ui.nutri.assinatura.AcoesAssinatura
import org.br.dietamais.ui.nutri.assinatura.AcoesPerfilNutri
import org.br.dietamais.ui.nutri.assinatura.AssinaturaCard
import org.br.dietamais.ui.nutri.assinatura.AssinaturaViewModel
import org.br.dietamais.ui.nutri.assinatura.ContaNutriTela
import org.br.dietamais.ui.nutri.assinatura.PerfilNutriViewModel
import org.br.dietamais.ui.privacidade.PrivacidadeCard
import org.br.dietamais.ui.privacidade.PrivacidadeViewModel
import org.koin.core.parameter.parametersOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import kotlinx.coroutines.CancellationException
import org.br.dietamais.data.repositorio.PortaAlimentos
import org.br.dietamais.recursos.Res
import org.br.dietamais.recursos.app_nome
import org.br.dietamais.recursos.auth_sair
import org.br.dietamais.ui.nutri.dieta.DietasPacienteRota
import org.br.dietamais.ui.nutri.dieta.MontadorDietaViewModel
import org.br.dietamais.ui.nutri.pacientes.AbaFicha
import org.br.dietamais.ui.nutri.pacientes.PacientesRota
import org.br.dietamais.ui.nutri.pacientes.PacientesViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Nutri na web (MOB-43): painel, pacientes (dieta, avaliações, lista, evolução e anamnese), agenda, alimentos, conta e avisos.
 * Sem sync, o gráfico de evolução da nutri não traz os pesos que o paciente registra em casa.
 */
@Composable
fun ShellWebNutri(nome: String, aoSair: () -> Unit, aoPrepararNavegacao: (androidx.navigation.NavHostController) -> Unit = {}) {
    val dono = remember { object : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() } }
    DisposableEffect(dono) { onDispose { dono.viewModelStore.clear() } }
    val alimentos = koinInject<PortaAlimentos>()
    // O montador só observa o catálogo; na web quem o carrega é o shell.
    LaunchedEffect(alimentos) { try { alimentos.atualizar() } catch (cancelamento: CancellationException) { throw cancelamento } catch (_: Exception) { } }
    val pacientes = koinViewModel<PacientesViewModel>(viewModelStoreOwner = dono)
    val atalhos = remember { AtalhosWeb(PerfilNavegacao.NUTRICIONISTA) }
    Column(Modifier.fillMaxSize().then(atalhos.modificador)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(Res.string.app_nome), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f))
            AvisosRota(koinViewModel<AvisosViewModel>(viewModelStoreOwner = dono))
            Text(nome, style = MaterialTheme.typography.bodyMedium)
            TextButton(aoSair) { Text(stringResource(Res.string.auth_sair)) }
        }
        NavegacaoApp(PerfilNavegacao.NUTRICIONISTA, aoPrepararNavegacao = { atalhos.registrar(it, aoPrepararNavegacao) },
            resolverVinculo = { ficha -> pacientes.estado.value.lista.firstOrNull { it.id.toString() == ficha }?.vinculoId?.toString() },
            conteudoPacientes = { classe, vinculo, abrir, voltar ->
                PacientesRota(pacientes, classe, vinculo, abrir, voltar, abaConteudo = { aba, paciente ->
                    when (aba) {
                        AbaFicha.DIETA -> DietasPacienteRota(koinViewModel<MontadorDietaViewModel>(viewModelStoreOwner = dono), paciente)
                        AbaFicha.AVALIACOES -> AvaliacoesNutriRota(koinViewModel<AvaliacoesViewModel>(viewModelStoreOwner = dono), paciente.id, paciente.sexo, paciente.dataNascimento)
                        AbaFicha.LISTA -> {
                            val lista = koinViewModel<ListaPacienteViewModel>(viewModelStoreOwner = dono)
                            val estadoLista by lista.estado.collectAsState()
                            LaunchedEffect(paciente.id) { lista.abrir(paciente.id) }
                            ListaPacienteTela(estadoLista, lista::mudarBusca, lista::remover, lista::adicionar, lista::salvar, lista::descartar, lista::recarregar)
                        }
                        AbaFicha.EVOLUCAO -> EvolucaoRota(koinViewModel<EvolucaoViewModel>(viewModelStoreOwner = dono), paciente.id)
                        AbaFicha.ANAMNESE -> AnamneseNutriRota(koinViewModel<AnamneseNutriViewModel>(viewModelStoreOwner = dono), paciente.id)
                        else -> EstadoVazio(mensagem = stringResource(Res.string.web_aba_no_app))
                    }
                })
            },
            conteudoInicio = { PainelRota(koinViewModel<PainelViewModel>(viewModelStoreOwner = dono)) },
            conteudoAgenda = { AgendaRota(koinViewModel<AgendaViewModel>(viewModelStoreOwner = dono)) },
            conteudoAlimentos = { AlimentosRota(koinViewModel<AlimentosViewModel>(viewModelStoreOwner = dono)) },
            conteudoConta = {
                val conta = koinViewModel<PerfilNutriViewModel>(viewModelStoreOwner = dono)
                val estadoConta by conta.estado.collectAsState()
                val assinatura = koinViewModel<AssinaturaViewModel>(viewModelStoreOwner = dono)
                val estadoAssinatura by assinatura.estado.collectAsState()
                ContaNutriTela(estadoConta, AcoesPerfilNutri(conta::mudarNome, conta::mudarCrn, conta::mudarTelefone, conta::mudarOrientacao, conta::salvar, conta::recarregar),
                    extras = {
                    val protocolos = koinViewModel<org.br.dietamais.ui.nutri.protocolos.ProtocolosNutriViewModel>(viewModelStoreOwner = dono)
                    val estadoProtocolos by protocolos.estado.collectAsState()
                    org.br.dietamais.ui.nutri.protocolos.ProtocolosNutriCard(estadoProtocolos, org.br.dietamais.ui.nutri.protocolos.AcoesProtocolosNutri(protocolos::mudarDobras, protocolos::mudarMassa, protocolos::salvar, protocolos::recarregar))
                        AssinaturaCard(estadoAssinatura, AcoesAssinatura(assinatura::assinar, assinatura::urlAberta, assinatura::recarregar))
                        val priv = koinViewModel<PrivacidadeViewModel>(viewModelStoreOwner = dono) { parametersOf(false) }
                        val estadoPriv by priv.estado.collectAsState()
                        PrivacidadeCard(estadoPriv, acoesPrivacidade(priv), comDadosPessoais = false)
                    })
            })
    }
}
