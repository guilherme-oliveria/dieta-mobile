package org.br.dietamais.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.toRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.window.core.layout.WindowSizeClass
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.EstadoVazio
import org.br.dietamais.ui.comum.ItemNavegacao
import org.br.dietamais.ui.comum.LayoutListaDetalhe
import org.br.dietamais.ui.comum.icones.IconesDieta
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private data class DestinoPrincipal(val chave: String, val rota: Any, val rotulo: StringResource, val icone: ImageVector)

typealias ConteudoPacientes = @Composable (WindowSizeClass, String?, (String, String) -> Unit, () -> Unit) -> Unit

@Composable
fun NavegacaoApp(
    perfil: PerfilNavegacao?,
    classeJanela: WindowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass,
    aoPrepararNavegacao: (NavHostController) -> Unit = {},
    conteudoPacientes: ConteudoPacientes? = null,
    conteudoAlimentos: (@Composable () -> Unit)? = null,
    conteudoHoje: (@Composable () -> Unit)? = null,
    conteudoDieta: (@Composable () -> Unit)? = null,
    conteudoPerfil: (@Composable () -> Unit)? = null,
) {
    // Trocar/limpar a sessão descarta também a pilha e os estados do perfil anterior.
    key(perfil) {
        val controlador = rememberNavController()
        val entradaAtual by controlador.currentBackStackEntryAsState()
        val destinos = destinosDoPerfil(perfil)
        val selecionado = destinos.firstOrNull { destino ->
            entradaAtual?.destination?.hierarchy?.any { it.hasRoute(destino.rota::class) } == true
        } ?: destinos.firstOrNull()
        val itens = destinos.map { ItemNavegacao(it.chave, stringResource(it.rotulo), it.icone) }

        val conteudo: @Composable () -> Unit = {
            NavHost(
                navController = controlador,
                startDestination = when (perfil) {
                    PerfilNavegacao.NUTRICIONISTA -> GrafoNutri
                    PerfilNavegacao.PACIENTE -> GrafoPaciente
                    null -> Entrada
                },
                modifier = Modifier.fillMaxSize(),
            ) {
                composable<Entrada> { AbaVazia(Res.string.entrar, classeJanela) }
                if (perfil == PerfilNavegacao.NUTRICIONISTA) grafoNutri(classeJanela, controlador, conteudoPacientes, conteudoAlimentos)
                if (perfil == PerfilNavegacao.PACIENTE) grafoPaciente(classeJanela, conteudoHoje, conteudoDieta, conteudoPerfil)
            }
        }
        if (perfil == null) Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) { conteudo() }
        }
        else AppScaffold(
            itens, selecionado?.chave,
            aoSelecionar = { chave ->
                val destino = destinos.first { it.chave == chave }
                controlador.navigate(destino.rota) {
                    popUpTo(controlador.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            classeJanela = classeJanela, content = conteudo,
        )
        LaunchedEffect(controlador) { aoPrepararNavegacao(controlador) }
    }
}

private fun NavGraphBuilder.grafoPaciente(classe: WindowSizeClass, hoje: (@Composable () -> Unit)?, dieta: (@Composable () -> Unit)?, perfil: (@Composable () -> Unit)?) {
    navigation<GrafoPaciente>(startDestination = RotaPaciente.Hoje) {
        composable<RotaPaciente.Hoje> { if (hoje != null) hoje() else AbaVazia(Res.string.aba_hoje, classe) }
        composable<RotaPaciente.Dieta> { if (dieta != null) dieta() else AbaVazia(Res.string.aba_dieta, classe) }
        composable<RotaPaciente.Compras> { AbaVazia(Res.string.aba_compras, classe) }
        composable<RotaPaciente.Evolucao> { AbaVazia(Res.string.aba_evolucao, classe) }
        composable<RotaPaciente.Perfil> { if (perfil != null) perfil() else AbaVazia(Res.string.aba_perfil, classe) }
    }
}

private fun NavGraphBuilder.grafoNutri(classe: WindowSizeClass, controlador: NavHostController, conteudo: ConteudoPacientes?, alimentos: (@Composable () -> Unit)?) {
    navigation<GrafoNutri>(startDestination = RotaNutri.Inicio) {
        composable<RotaNutri.Inicio> { AbaVazia(Res.string.aba_inicio, classe) }
        navigation<RotaNutri.Pacientes>(startDestination = RotaNutri.ListaPacientes) {
            composable<RotaNutri.ListaPacientes> {
                if (conteudo != null) conteudo(classe, null,
                    { id, vinculo -> controlador.navigate(RotaNutri.FichaPaciente(id, vinculo)) }, { controlador.popBackStack() })
                else LayoutListaDetalhe(classe, mostrarDetalhe = false,
                    lista = { AbaVazia(Res.string.aba_pacientes, classe) },
                    detalhe = { EstadoVazio(mensagem = stringResource(Res.string.selecione_paciente)) },
                )
            }
            composable<RotaNutri.FichaPaciente> { entrada ->
                if (conteudo != null) conteudo(classe, entrada.toRoute<RotaNutri.FichaPaciente>().vinculoId,
                    { id, vinculo -> controlador.navigate(RotaNutri.FichaPaciente(id, vinculo)) }, { controlador.popBackStack() })
                else LayoutListaDetalhe(classe, mostrarDetalhe = true,
                    lista = { AbaVazia(Res.string.aba_pacientes, classe) },
                    detalhe = { AbaVazia(Res.string.ficha_paciente, classe) },
                )
            }
        }
        composable<RotaNutri.Agenda> { AbaVazia(Res.string.aba_agenda, classe) }
        composable<RotaNutri.Alimentos> { if (alimentos != null) alimentos() else AbaVazia(Res.string.aba_alimentos, classe) }
        composable<RotaNutri.Conta> { AbaVazia(Res.string.aba_conta, classe) }
    }
}

private fun destinosDoPerfil(perfil: PerfilNavegacao?) = when (perfil) {
    PerfilNavegacao.PACIENTE -> listOf(
        DestinoPrincipal("hoje", RotaPaciente.Hoje, Res.string.aba_hoje, IconesDieta.Casa),
        DestinoPrincipal("dieta", RotaPaciente.Dieta, Res.string.aba_dieta, IconesDieta.Talheres),
        DestinoPrincipal("compras", RotaPaciente.Compras, Res.string.aba_compras, IconesDieta.Carrinho),
        DestinoPrincipal("evolucao", RotaPaciente.Evolucao, Res.string.aba_evolucao, IconesDieta.Grafico),
        DestinoPrincipal("perfil", RotaPaciente.Perfil, Res.string.aba_perfil, IconesDieta.Pessoa),
    )
    PerfilNavegacao.NUTRICIONISTA -> listOf(
        DestinoPrincipal("inicio", RotaNutri.Inicio, Res.string.aba_inicio, IconesDieta.Casa),
        DestinoPrincipal("pacientes", RotaNutri.Pacientes, Res.string.aba_pacientes, IconesDieta.Pessoa),
        DestinoPrincipal("agenda", RotaNutri.Agenda, Res.string.aba_agenda, IconesDieta.Calendario),
        DestinoPrincipal("alimentos", RotaNutri.Alimentos, Res.string.aba_alimentos, IconesDieta.Lista),
        DestinoPrincipal("conta", RotaNutri.Conta, Res.string.aba_conta, IconesDieta.Pessoa),
    )
    null -> emptyList()
}

@Composable
private fun AbaVazia(titulo: StringResource, classeJanela: WindowSizeClass) {
    Column(
        Modifier.fillMaxSize().padding(Dimens.margemTela(classeJanela)),
        verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio),
    ) {
        Text(stringResource(titulo), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        EstadoVazio(mensagem = stringResource(Res.string.sem_conteudo))
    }
}

@Composable
fun tituloAplicacao(): String = stringResource(Res.string.app_nome)
