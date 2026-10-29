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
 * Nutri na web (MOB-43, primeira entrega): pacientes e a aba Dieta com o montador. As outras abas e telas chegam depois do
 * ponto de decisão; até lá o resto da navegação mostra o estado vazio padrão.
 */
@Composable
fun ShellWebNutri(nome: String, aoSair: () -> Unit) {
    val dono = remember { object : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() } }
    DisposableEffect(dono) { onDispose { dono.viewModelStore.clear() } }
    val alimentos = koinInject<PortaAlimentos>()
    // O montador só observa o catálogo; na web quem o carrega é o shell.
    LaunchedEffect(alimentos) { try { alimentos.atualizar() } catch (cancelamento: CancellationException) { throw cancelamento } catch (_: Exception) { } }
    val pacientes = koinViewModel<PacientesViewModel>(viewModelStoreOwner = dono)
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(Res.string.app_nome), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f))
            Text(nome, style = MaterialTheme.typography.bodyMedium)
            TextButton(aoSair) { Text(stringResource(Res.string.auth_sair)) }
        }
        NavegacaoApp(PerfilNavegacao.NUTRICIONISTA,
            resolverVinculo = { ficha -> pacientes.estado.value.lista.firstOrNull { it.id.toString() == ficha }?.vinculoId?.toString() },
            conteudoPacientes = { classe, vinculo, abrir, voltar ->
                PacientesRota(pacientes, classe, vinculo, abrir, voltar, abaConteudo = { aba, paciente ->
                    if (aba == AbaFicha.DIETA) DietasPacienteRota(koinViewModel<MontadorDietaViewModel>(viewModelStoreOwner = dono), paciente)
                })
            },
            conteudoConta = {
                val conta = koinViewModel<PerfilNutriViewModel>(viewModelStoreOwner = dono)
                val estadoConta by conta.estado.collectAsState()
                val assinatura = koinViewModel<AssinaturaViewModel>(viewModelStoreOwner = dono)
                val estadoAssinatura by assinatura.estado.collectAsState()
                ContaNutriTela(estadoConta, AcoesPerfilNutri(conta::mudarNome, conta::mudarCrn, conta::mudarTelefone, conta::mudarOrientacao, conta::salvar, conta::recarregar),
                    extras = {
                        AssinaturaCard(estadoAssinatura, AcoesAssinatura(assinatura::assinar, assinatura::urlAberta, assinatura::recarregar))
                        val priv = koinViewModel<PrivacidadeViewModel>(viewModelStoreOwner = dono) { parametersOf(false) }
                        val estadoPriv by priv.estado.collectAsState()
                        PrivacidadeCard(estadoPriv, acoesPrivacidade(priv), comDadosPessoais = false)
                    })
            })
    }
}
