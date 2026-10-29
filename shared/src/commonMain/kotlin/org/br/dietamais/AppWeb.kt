package org.br.dietamais

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import org.br.dietamais.app.AmbienteWeb
import org.br.dietamais.app.moduloApp
import org.br.dietamais.app.moduloAutenticacao
import org.br.dietamais.app.moduloConvites
import org.br.dietamais.app.ShellWebNutri
import org.br.dietamais.app.ShellWebPaciente
import org.br.dietamais.app.moduloWebPaciente
import org.br.dietamais.app.moduloWeb
import org.br.dietamais.app.moduloWebNutri
import org.br.dietamais.data.remoto.dto.Perfil
import org.br.dietamais.plataforma.Compartilhamento
import org.br.dietamais.plataforma.IdentidadeDispositivo
import org.br.dietamais.ui.auth.FluxoAutenticacao
import org.br.dietamais.ui.auth.TermosCadastro
import org.br.dietamais.ui.theme.DietaTheme
import org.koin.compose.KoinIsolatedContext
import org.koin.compose.viewmodel.koinViewModel
import org.koin.dsl.koinApplication

/**
 * Raiz da web (MOB-42): login, convite e recuperação de senha pela API. As telas da nutri e do paciente entram no MOB-43 e no
 * MOB-44, em [shell].
 */
@Composable
fun AppWeb(ambiente: AmbienteWeb, identidadeDispositivo: IdentidadeDispositivo, compartilhamento: Compartilhamento,
    termosCadastro: TermosCadastro? = null,
    /** Liga a pilha de navegação à URL e ao botão voltar do navegador (só existe no `wasmJsMain`). */
    aoPrepararNavegacao: (androidx.navigation.NavHostController) -> Unit = {},
    shell: @Composable (org.br.dietamais.data.remoto.dto.UsuarioResposta, () -> Unit) -> Unit = { usuario, sair ->
        if (usuario.perfil == Perfil.NUTRICIONISTA) ShellWebNutri(usuario.nome, sair, aoPrepararNavegacao)
        else ShellWebPaciente(usuario.nome, sair, aoPrepararNavegacao)
    }) {
    val contexto = remember(ambiente, identidadeDispositivo, compartilhamento) { koinApplication {
        modules(moduloApp(ambiente.config), moduloWeb(ambiente), moduloAutenticacao(identidadeDispositivo, termosCadastro),
            moduloConvites(identidadeDispositivo), moduloWebNutri(ambiente, compartilhamento), moduloWebPaciente(ambiente))
    } }
    DisposableEffect(contexto) { onDispose { contexto.close() } }
    KoinIsolatedContext(contexto) {
        DietaTheme {
            val dono = remember { object : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() } }
            DisposableEffect(dono) { onDispose { dono.viewModelStore.clear() } }
            FluxoAutenticacao(viewModel = koinViewModel(viewModelStoreOwner = dono), convites = koinViewModel(viewModelStoreOwner = dono),
                shellAutenticado = shell)
        }
    }
}
