package org.br.dietamais

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.br.dietamais.app.NavegacaoApp
import org.br.dietamais.app.PerfilNavegacao
import org.br.dietamais.app.moduloApp
import org.br.dietamais.app.moduloBancoLocal
import org.br.dietamais.app.moduloApi
import org.br.dietamais.app.moduloAutenticacao
import org.br.dietamais.app.moduloPacientes
import org.br.dietamais.app.moduloConvites
import org.br.dietamais.app.moduloAmbiente
import org.br.dietamais.app.moduloAlimentos
import org.br.dietamais.app.moduloListaPaciente
import org.br.dietamais.app.moduloDietas
import org.br.dietamais.app.moduloPorcoes
import org.br.dietamais.app.moduloDietaPaciente
import org.br.dietamais.app.moduloLembretes
import org.br.dietamais.app.moduloPerfilNutri
import org.br.dietamais.app.moduloAvaliacoes
import org.br.dietamais.plataforma.NotificacaoLocal
import org.br.dietamais.app.AmbienteDieta
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import org.br.dietamais.plataforma.Compartilhamento
import org.br.dietamais.plataforma.CompartilhamentoIndisponivel
import org.br.dietamais.plataforma.IdentidadeDispositivo
import org.br.dietamais.ui.auth.FluxoAutenticacao
import org.br.dietamais.ui.auth.TermosCadastro
import org.br.dietamais.plataforma.CofreSessao
import org.br.dietamais.plataforma.Config
import org.br.dietamais.plataforma.FabricaDriver
import org.br.dietamais.ui.theme.DietaTheme
import org.koin.compose.KoinIsolatedContext
import org.koin.dsl.koinApplication

@Composable
@Preview
fun App(config: Config = Config(), perfil: PerfilNavegacao? = null, fabricaDriver: FabricaDriver? = null,
    cofreSessao: CofreSessao? = null, versaoApp: String = "1.0.0",
    identidadeDispositivo: IdentidadeDispositivo? = null, termosCadastro: TermosCadastro? = null,
    compartilhamento: Compartilhamento = Compartilhamento { throw CompartilhamentoIndisponivel() }, ambiente: AmbienteDieta? = null,
    notificacao: NotificacaoLocal? = null) {
    val dadosDisponiveis = ambiente != null || (fabricaDriver != null && cofreSessao != null)
    val autenticacaoDisponivel = dadosDisponiveis && identidadeDispositivo != null
    if (ambiente != null) {
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { ambiente.primeiroPlano(true) }
        LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { ambiente.primeiroPlano(false) }
        DisposableEffect(ambiente) { onDispose { ambiente.primeiroPlano(false) } }
    }
    val contexto = remember(ambiente, fabricaDriver, cofreSessao, identidadeDispositivo, notificacao) { koinApplication {
        modules(moduloApp(ambiente?.config ?: config))
        if (ambiente != null) modules(moduloAmbiente(ambiente))
        else {
            fabricaDriver?.let { modules(moduloBancoLocal(it)) }
            if (fabricaDriver != null && cofreSessao != null) modules(moduloApi(cofreSessao, versaoApp))
        }
        if (autenticacaoDisponivel) modules(moduloAutenticacao(identidadeDispositivo, termosCadastro))
        if (autenticacaoDisponivel) modules(moduloConvites(identidadeDispositivo))
        if (dadosDisponiveis) modules(moduloPacientes(compartilhamento, ambiente))
        if (dadosDisponiveis) modules(moduloAlimentos(ambiente))
        if (dadosDisponiveis) modules(moduloListaPaciente(ambiente))
        if (dadosDisponiveis) modules(moduloDietas(ambiente), moduloPorcoes(ambiente), moduloDietaPaciente(ambiente), moduloLembretes(notificacao), moduloPerfilNutri(ambiente), moduloAvaliacoes(ambiente))
    } }
    DisposableEffect(contexto) { onDispose { contexto.close() } }
    KoinIsolatedContext(contexto) {
        DietaTheme {
            if (autenticacaoDisponivel) {
                val dono = remember { object : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() } }
                DisposableEffect(dono) { onDispose { dono.viewModelStore.clear() } }
                FluxoAutenticacao(viewModel = koinViewModel(viewModelStoreOwner = dono),
                    convites = koinViewModel(viewModelStoreOwner = dono), ambiente = ambiente)
            }
            else NavegacaoApp(perfil)
        }
    }
}
