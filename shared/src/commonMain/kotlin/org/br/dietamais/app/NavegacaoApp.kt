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
import androidx.compose.runtime.collectAsState
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
import org.br.dietamais.dominio.avisos.DestinoAviso
import org.br.dietamais.dominio.avisos.destinoDaRota
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
    conteudoConta: (@Composable () -> Unit)? = null,
    conteudoHoje: (@Composable () -> Unit)? = null,
    conteudoDieta: (@Composable () -> Unit)? = null,
    conteudoPerfil: (@Composable () -> Unit)? = null,
    conteudoCompras: (@Composable () -> Unit)? = null,
    conteudoEvolucao: (@Composable () -> Unit)? = null,
    conteudoAgenda: (@Composable () -> Unit)? = null,
    conteudoInicio: (@Composable () -> Unit)? = null,
    /** Da ficha (`/pacientes/{id}` do aviso) para o vínculo, que é o que a ficha aberta usa; nulo cai na lista. */
    resolverVinculo: (String) -> String? = { null },
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
                if (perfil == PerfilNavegacao.NUTRICIONISTA) grafoNutri(classeJanela, controlador, conteudoPacientes, conteudoAlimentos, conteudoConta, conteudoAgenda, conteudoInicio)
                if (perfil == PerfilNavegacao.PACIENTE) grafoPaciente(classeJanela, conteudoHoje, conteudoDieta, conteudoPerfil, conteudoCompras, conteudoEvolucao)
            }
        }
        if (perfil == null) Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) { conteudo() }
        }
        else AppScaffold(
            itens, selecionado?.chave,
            aoSelecionar = { chave ->
                val destino = destinos.first { it.chave == chave }
                controlador.irParaAba(destino.rota)
            },
            classeJanela = classeJanela, content = conteudo,
        )
        LaunchedEffect(controlador) { aoPrepararNavegacao(controlador) }
        val pedido by SolicitacoesNavegacao.pendente.collectAsState()
        LaunchedEffect(pedido?.id, perfil) {
            val p = pedido
            if (p != null && perfil != null) {
                navegarParaAviso(controlador, destinoDaRota(perfil, p.rota), resolverVinculo)
                SolicitacoesNavegacao.consumir(p.id)
            }
        }
    }
}

private fun NavGraphBuilder.grafoPaciente(classe: WindowSizeClass, hoje: (@Composable () -> Unit)?, dieta: (@Composable () -> Unit)?, perfil: (@Composable () -> Unit)?, compras: (@Composable () -> Unit)?, evolucao: (@Composable () -> Unit)?) {
    navigation<GrafoPaciente>(startDestination = RotaPaciente.Hoje) {
        composable<RotaPaciente.Hoje> { if (hoje != null) hoje() else AbaVazia(Res.string.aba_hoje, classe) }
        composable<RotaPaciente.Dieta> { if (dieta != null) dieta() else AbaVazia(Res.string.aba_dieta, classe) }
        composable<RotaPaciente.Compras> { if (compras != null) compras() else AbaVazia(Res.string.aba_compras, classe) }
        composable<RotaPaciente.Evolucao> { if (evolucao != null) evolucao() else AbaVazia(Res.string.aba_evolucao, classe) }
        composable<RotaPaciente.Perfil> { if (perfil != null) perfil() else AbaVazia(Res.string.aba_perfil, classe) }
    }
}

private fun NavGraphBuilder.grafoNutri(classe: WindowSizeClass, controlador: NavHostController, conteudo: ConteudoPacientes?, alimentos: (@Composable () -> Unit)?, conta: (@Composable () -> Unit)?, agenda: (@Composable () -> Unit)? = null, inicio: (@Composable () -> Unit)? = null) {
    navigation<GrafoNutri>(startDestination = RotaNutri.Inicio) {
        composable<RotaNutri.Inicio> { if (inicio != null) inicio() else AbaVazia(Res.string.aba_inicio, classe) }
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
        composable<RotaNutri.Agenda> { if (agenda != null) agenda() else AbaVazia(Res.string.aba_agenda, classe) }
        composable<RotaNutri.Alimentos> { if (alimentos != null) alimentos() else AbaVazia(Res.string.aba_alimentos, classe) }
        composable<RotaNutri.Conta> { if (conta != null) conta() else AbaVazia(Res.string.aba_conta, classe) }
    }
}

private fun navegarParaAviso(controlador: NavHostController, destino: DestinoAviso, resolverVinculo: (String) -> String?) {
    val rota: Any = when (destino) {
        DestinoAviso.Hoje -> RotaPaciente.Hoje
        DestinoAviso.Dieta -> RotaPaciente.Dieta
        DestinoAviso.Agenda -> RotaNutri.Agenda
        is DestinoAviso.Paciente -> resolverVinculo(destino.fichaId)?.let { RotaNutri.FichaPaciente(destino.fichaId, it) } ?: RotaNutri.Pacientes
        DestinoAviso.Nenhum -> return
    }
    controlador.navigate(rota) {
        popUpTo(controlador.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = destino !is DestinoAviso.Paciente
    }
}

internal fun NavHostController.irParaAba(rota: Any) = navigate(rota) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

/** Atalho Alt+1..5 (web): a aba na posição do número, na mesma ordem da navegação; fora disso, nulo. */
fun rotaDoAtalho(perfil: PerfilNavegacao, numero: Int): Any? = destinosDoPerfil(perfil).getOrNull(numero - 1)?.rota

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
        DestinoPrincipal("pacientes", RotaNutri.Pacientes, Res.string.aba_pacientes, IconesDieta.Pacientes),
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
