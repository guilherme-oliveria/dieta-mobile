package org.br.dietamais

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.br.dietamais.app.NavegacaoApp
import org.br.dietamais.app.PerfilNavegacao
import org.br.dietamais.app.moduloApp
import org.br.dietamais.app.moduloBancoLocal
import org.br.dietamais.plataforma.Config
import org.br.dietamais.plataforma.FabricaDriver
import org.br.dietamais.ui.theme.DietaTheme
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration

@Composable
@Preview
fun App(config: Config = Config(), perfil: PerfilNavegacao? = null, fabricaDriver: FabricaDriver? = null) {
    KoinApplication(configuration = koinConfiguration {
        modules(moduloApp(config))
        fabricaDriver?.let { modules(moduloBancoLocal(it)) }
    }) {
        DietaTheme {
            NavegacaoApp(perfil)
        }
    }
}
