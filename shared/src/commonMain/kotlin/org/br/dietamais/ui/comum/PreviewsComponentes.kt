package org.br.dietamais.ui.comum

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.window.core.layout.WindowSizeClass
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.icones.IconesDieta
import org.br.dietamais.ui.theme.DietaTheme
import org.br.dietamais.ui.theme.Dimens
import org.br.dietamais.ui.theme.TamanhosPreview
import org.jetbrains.compose.resources.stringResource

@Preview(name = "Base compacta", widthDp = TamanhosPreview.compacto, heightDp = TamanhosPreview.altura)
@Preview(name = "Base média", widthDp = TamanhosPreview.medio, heightDp = TamanhosPreview.altura)
@Preview(name = "Base expandida", widthDp = TamanhosPreview.expandido, heightDp = TamanhosPreview.altura)
@Preview(name = "Base fonte 200%", widthDp = TamanhosPreview.compacto, heightDp = TamanhosPreview.altura, fontScale = 2f)
@Composable
private fun ComponentesBasePreview() = DietaTheme { GaleriaComponentesBase() }

/** Amostra para previews e conferência visual; não é uma tela navegável do produto. */
@Composable
internal fun GaleriaComponentesBase(classeJanela: WindowSizeClass? = null) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        val conteudo: @Composable ColumnScope.() -> Unit = {
            Text(stringResource(Res.string.amostra_titulo), style = MaterialTheme.typography.headlineMedium)
            FaixaOffline(Modifier.fillMaxWidth())
            CardProximaRefeicao(
                nome = stringResource(Res.string.amostra_refeicao), horario = stringResource(Res.string.amostra_horario),
                energia = stringResource(Res.string.amostra_energia), descricao = stringResource(Res.string.amostra_descricao),
                aoComer = {}, aoTrocar = {}, aoPular = {}, modifier = Modifier.fillMaxWidth(),
            )
            BotaoPrincipal(stringResource(Res.string.amostra_continuar), {}, Modifier.fillMaxWidth())
            CampoTexto(stringResource(Res.string.amostra_campo_nome), "", {})
            CardDieta(Modifier.fillMaxWidth()) {
                LinhaRefeicao(
                    stringResource(Res.string.amostra_refeicao), stringResource(Res.string.amostra_horario),
                    stringResource(Res.string.amostra_energia), EstiloStatus.FEITA,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno),
                    verticalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno),
                ) { EstiloStatus.entries.forEach { ChipStatus(it) } }
            }
            EstadoErro(aoTentarNovamente = {})
        }
        val modifier = Modifier.verticalScroll(rememberScrollState()).padding(vertical = Dimens.margemAmpla)
        if (classeJanela == null) ConteudoLeitura(modifier = modifier, content = conteudo)
        else ConteudoLeitura(modifier = modifier, classeJanela = classeJanela, content = conteudo)
    }
}

@Preview(name = "Botões e estados", widthDp = TamanhosPreview.compacto, heightDp = TamanhosPreview.altura)
@Composable
private fun BotoesPreview() = PreviewBase {
    val texto = stringResource(Res.string.amostra_continuar)
    for (carregando in listOf(false, true)) {
        BotaoPrincipal(texto, {}, carregando = carregando)
        BotaoContornado(texto, {}, carregando = carregando)
        BotaoTexto(texto, {}, carregando = carregando)
    }
    BotaoPrincipal(texto, {}, habilitado = false)
    BotaoContornado(texto, {}, habilitado = false)
    BotaoTexto(texto, {}, habilitado = false)
}

@Preview(name = "Campos", widthDp = TamanhosPreview.compacto, heightDp = TamanhosPreview.altura)
@Composable
private fun CamposPreview() = PreviewBase {
    val nome = stringResource(Res.string.amostra_campo_nome)
    CampoTexto(nome, "", {})
    CampoTexto(nome, "", {}, erro = stringResource(Res.string.amostra_erro_campo))
    CampoTexto(nome, "", {}, habilitado = false)
    CampoHora(stringResource(Res.string.amostra_campo_hora), stringResource(Res.string.amostra_horario), {})
    CampoNumero(stringResource(Res.string.amostra_campo_quantidade), stringResource(Res.string.amostra_quantidade), {})
}

@Preview(name = "Estados de conteúdo", widthDp = TamanhosPreview.compacto, heightDp = TamanhosPreview.altura)
@Composable
private fun EstadosPreview() = PreviewBase {
    EstadoVazio()
    EstadoVazio(textoAcao = stringResource(Res.string.amostra_criar), aoAcionar = {})
    EstadoErro(aoTentarNovamente = {})
    EstadoCarregando()
    FaixaOffline()
}

@Preview(name = "Ícones", widthDp = TamanhosPreview.compacto, heightDp = TamanhosPreview.altura)
@Composable
private fun IconesPreview() = PreviewBase {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Dimens.margemAmpla),
        verticalArrangement = Arrangement.spacedBy(Dimens.margemAmpla),
    ) {
        listOf(
            IconesDieta.Casa, IconesDieta.Talheres, IconesDieta.Carrinho, IconesDieta.Grafico,
            IconesDieta.Pessoa, IconesDieta.Sino, IconesDieta.Agua, IconesDieta.Cadeado,
            IconesDieta.Calendario, IconesDieta.Lista, IconesDieta.Mais, IconesDieta.Voltar,
            IconesDieta.Fechar, IconesDieta.Busca, IconesDieta.Alerta, IconesDieta.Informacao, IconesDieta.SemInternet,
        ).forEach { Icon(it, contentDescription = null, modifier = Modifier.size(Dimens.iconePadrao)) }
    }
}

@Composable
private fun PreviewBase(content: @Composable ColumnScope.() -> Unit) = DietaTheme {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(Dimens.margemAmpla),
            verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio), content = content,
        )
    }
}
