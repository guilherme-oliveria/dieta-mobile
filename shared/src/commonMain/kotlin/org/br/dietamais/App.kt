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
import org.br.dietamais.plataforma.Compartilhamento
import org.br.dietamais.plataforma.CompartilhamentoIndisponivel
import org.br.dietamais.plataforma.IdentidadeDispositivo
import org.br.dietamais.ui.auth.FluxoAutenticacao
import org.br.dietamais.ui.auth.TermosCadastro
import org.br.dietamais.plataforma.CofreSessao
import org.br.dietamais.plataforma.Config
import org.br.dietamais.plataforma.FabricaDriver
import org.br.dietamais.ui.theme.DietaTheme
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration

@Composable
@Preview
fun App(config: Config = Config(), perfil: PerfilNavegacao? = null, fabricaDriver: FabricaDriver? = null,
    cofreSessao: CofreSessao? = null, versaoApp: String = "1.0.0",
    identidadeDispositivo: IdentidadeDispositivo? = null, termosCadastro: TermosCadastro? = null,
    compartilhamento: Compartilhamento = Compartilhamento { throw CompartilhamentoIndisponivel() }) {
    KoinApplication(configuration = koinConfiguration {
        modules(moduloApp(config))
        fabricaDriver?.let { modules(moduloBancoLocal(it)) }
        if (fabricaDriver != null && cofreSessao != null) modules(moduloApi(cofreSessao, versaoApp))
        if (fabricaDriver != null && cofreSessao != null && identidadeDispositivo != null) modules(moduloAutenticacao(identidadeDispositivo, termosCadastro))
        if (fabricaDriver != null && cofreSessao != null) modules(moduloPacientes(compartilhamento))
    }) {
        DietaTheme {
            if (fabricaDriver != null && cofreSessao != null && identidadeDispositivo != null) FluxoAutenticacao()
            else NavegacaoApp(perfil)
        }
    }
}
