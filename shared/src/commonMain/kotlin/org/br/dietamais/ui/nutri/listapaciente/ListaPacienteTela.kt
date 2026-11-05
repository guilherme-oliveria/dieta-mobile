package org.br.dietamais.ui.nutri.listapaciente

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource
import kotlin.uuid.Uuid

@Composable
fun ListaPacienteTela(estado: EstadoListaPaciente, aoBusca: (String) -> Unit = {}, aoRemover: (Uuid) -> Unit = {},
    aoAdicionar: (Uuid) -> Unit = {}, aoSalvar: () -> Unit = {}, aoDescartar: () -> Unit = {}, aoRecarregar: () -> Unit = {},
    acoesCopia: AcoesCopiaLista = AcoesCopiaLista()) {
    val habilitado = !estado.semConexao && !estado.carregando && !estado.enviando
    Column(Modifier.fillMaxWidth().testTag("lista-paciente"), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
        Text(stringResource(Res.string.lista_titulo), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        Text(stringResource(Res.string.lista_descricao), style = MaterialTheme.typography.bodySmall)
        if (estado.semConexao) FaixaOffline()
        estado.erro?.let { Text(textoMensagemLista(it), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
        estado.aviso?.let { Text(textoMensagemLista(it), style = MaterialTheme.typography.bodySmall, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
        if (estado.carregando) EstadoCarregando()
        else if (estado.erro != null && estado.efetiva.isEmpty()) EstadoErro(aoRecarregar, mensagem = textoMensagemLista(estado.erro))
        else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                BotaoPrincipal(stringResource(Res.string.lista_salvar), aoSalvar, habilitado = habilitado && estado.pendente, carregando = estado.enviando)
                BotaoTexto(stringResource(Res.string.lista_descartar), aoDescartar, habilitado = habilitado && estado.pendente)
            }
            if (estado.origens.isNotEmpty()) SecaoCopiarLista(estado, acoesCopia, habilitado)
            Text(stringResource(Res.string.lista_na_lista), style = MaterialTheme.typography.labelLarge)
            if (estado.efetiva.isEmpty()) EstadoVazio(mensagem = stringResource(Res.string.lista_vazia))
            estado.efetiva.forEach { alimento ->
                CardDieta(Modifier.fillMaxWidth()) {
                    Text(alimento.nome, style = MaterialTheme.typography.titleSmall)
                    Text(nomeGrupo(estado, alimento), style = MaterialTheme.typography.bodySmall)
                    BotaoTexto(stringResource(Res.string.lista_remover), { aoRemover(alimento.id) }, habilitado = habilitado)
                }
            }
            Text(stringResource(Res.string.lista_adicionar), style = MaterialTheme.typography.labelLarge)
            CampoTexto(stringResource(Res.string.alimentos_busca), estado.busca, aoBusca)
            if (estado.opcoes.isEmpty()) Text(stringResource(Res.string.lista_sem_opcoes), style = MaterialTheme.typography.bodySmall)
            estado.opcoes.take(30).forEach { alimento ->
                CardDieta(Modifier.fillMaxWidth()) {
                    Text(alimento.nome, style = MaterialTheme.typography.titleSmall)
                    Text(nomeGrupo(estado, alimento), style = MaterialTheme.typography.bodySmall)
                    BotaoTexto(stringResource(Res.string.lista_incluir), { aoAdicionar(alimento.id) }, habilitado = habilitado)
                }
            }
        }
    }
}

data class AcoesCopiaLista(val aoEscolher: (Boolean) -> Unit = {}, val aoPedir: (Uuid) -> Unit = {}, val aoConfirmar: () -> Unit = {}, val aoDesistir: () -> Unit = {})

/** Copiar a lista de outro paciente da nutri (G-19, RN-ALI-06): troca a lista inteira, por isso pede confirmação. */
@Composable
private fun SecaoCopiarLista(estado: EstadoListaPaciente, acoes: AcoesCopiaLista, habilitado: Boolean) {
    BotaoContornado(stringResource(if (estado.escolhendoOrigem) Res.string.lista_copiar_fechar else Res.string.lista_copiar_abrir), { acoes.aoEscolher(!estado.escolhendoOrigem) }, habilitado = habilitado)
    if (estado.escolhendoOrigem) {
        Text(stringResource(Res.string.lista_copiar_descricao), style = MaterialTheme.typography.bodySmall)
        estado.origens.forEach { p -> BotaoTexto(p.nome, { acoes.aoPedir(p.id) }, habilitado = habilitado) }
    }
    val origem = estado.origens.firstOrNull { it.id == estado.copiandoDe }
    if (origem != null) AlertDialog(onDismissRequest = acoes.aoDesistir,
        title = { Text(stringResource(Res.string.lista_copiar_titulo)) },
        text = { Text(stringResource(Res.string.lista_copiar_texto, origem.nome)) },
        confirmButton = { BotaoPrincipal(stringResource(Res.string.lista_copiar_confirmar), acoes.aoConfirmar, habilitado = habilitado) },
        dismissButton = { BotaoTexto(stringResource(Res.string.lista_copiar_desistir), acoes.aoDesistir) })
}

private fun nomeGrupo(estado: EstadoListaPaciente, alimento: AlimentoResposta) = estado.grupos.firstOrNull { it.id == alimento.grupoId }?.nome.orEmpty()

@Composable
fun textoMensagemLista(mensagem: MensagemLista): String = stringResource(when (mensagem) {
    MensagemLista.SEM_CONEXAO -> Res.string.pacientes_erro_rede
    MensagemLista.CONFLITO -> Res.string.lista_conflito
    MensagemLista.ASSINATURA_INATIVA -> Res.string.pacientes_erro_assinatura
    MensagemLista.NAO_ENCONTRADO -> Res.string.lista_nao_encontrada
    MensagemLista.SALVO -> Res.string.lista_salva
    MensagemLista.COPIADA -> Res.string.lista_copiada
    MensagemLista.VINCULO_ENCERRADO -> Res.string.lista_vinculo_encerrado
    MensagemLista.ERRO -> Res.string.erro_padrao
})
