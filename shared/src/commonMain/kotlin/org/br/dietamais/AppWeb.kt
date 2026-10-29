package org.br.dietamais

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import org.br.dietamais.app.AmbienteWeb
import org.br.dietamais.app.moduloApp
import org.br.dietamais.app.moduloAutenticacao
import org.br.dietamais.app.moduloConvites
import org.br.dietamais.app.ShellWebNutri
import org.br.dietamais.app.moduloWeb
import org.br.dietamais.app.moduloWebNutri
import org.br.dietamais.data.remoto.dto.Perfil
import org.br.dietamais.plataforma.Compartilhamento
import org.br.dietamais.plataforma.IdentidadeDispositivo
import org.br.dietamais.recursos.Res
import org.br.dietamais.recursos.auth_sair
import org.br.dietamais.recursos.web_em_construcao
import org.br.dietamais.ui.auth.FluxoAutenticacao
import org.br.dietamais.ui.auth.TermosCadastro
import org.br.dietamais.ui.theme.DietaTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.KoinIsolatedContext
import org.koin.compose.viewmodel.koinViewModel
import org.koin.dsl.koinApplication

/**
 * Raiz da web (MOB-42): login, convite e recuperação de senha pela API. As telas da nutri e do paciente entram no MOB-43 e no
 * MOB-44, trocando [shell]; até lá, depois do login aparece só um aviso com "Sair".
 */
@Composable
fun AppWeb(ambiente: AmbienteWeb, identidadeDispositivo: IdentidadeDispositivo, compartilhamento: Compartilhamento,
    termosCadastro: TermosCadastro? = null,
    /** Liga a pilha de navegação à URL e ao botão voltar do navegador (só existe no `wasmJsMain`). */
    aoPrepararNavegacao: (androidx.navigation.NavHostController) -> Unit = {},
    shell: @Composable (org.br.dietamais.data.remoto.dto.UsuarioResposta, () -> Unit) -> Unit = { usuario, sair ->
        if (usuario.perfil == Perfil.NUTRICIONISTA) ShellWebNutri(usuario.nome, sair, aoPrepararNavegacao) else ShellProvisorio(usuario.nome, sair)
    }) {
    val contexto = remember(ambiente, identidadeDispositivo, compartilhamento) { koinApplication {
        modules(moduloApp(ambiente.config), moduloWeb(ambiente), moduloAutenticacao(identidadeDispositivo, termosCadastro),
            moduloConvites(identidadeDispositivo), moduloWebNutri(ambiente, compartilhamento))
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

@Composable
private fun ShellProvisorio(nome: String, sair: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(Res.string.web_em_construcao, nome), style = MaterialTheme.typography.bodyLarge)
        TextButton(sair) { Text(stringResource(Res.string.auth_sair)) }
    }
}
