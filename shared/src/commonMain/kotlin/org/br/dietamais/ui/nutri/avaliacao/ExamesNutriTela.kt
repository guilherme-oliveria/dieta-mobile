package org.br.dietamais.ui.nutri.avaliacao

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import org.br.dietamais.data.remoto.dto.ExameCatalogoDto
import org.br.dietamais.dominio.avaliacao.classeDoRotulo
import org.br.dietamais.dominio.avaliacao.ClasseFaixa
import org.br.dietamais.dominio.avaliacao.rotuloLegivel
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

class AcoesExames(
    val aoVoltar: () -> Unit = {}, val aoBuscar: (String) -> Unit = {}, val aoSecao: (String?) -> Unit = {}, val aoValor: (String, String) -> Unit = { _, _ -> },
    val aoCondicao: (String, String?) -> Unit = { _, _ -> }, val aoMostrar: (Boolean) -> Unit = {}, val aoJustificativa: (String) -> Unit = {}, val aoSalvar: () -> Unit = {},
)

/**
 * Lançar exames (artboard `A-Exames-Nutri`, MOB-60b): busca pelo nome, seções em chips, resultado por exame e "Mostrar exames ao paciente" (desligado por padrão, RN-RES-01).
 * A classificação (normal, alto...) é do servidor e aparece nos exames já salvos; ao digitar um valor novo ela só vem depois de salvar.
 */
@Composable
fun ExamesNutriTela(estado: EstadoExamesNutri, nomePaciente: String, acoes: AcoesExames) {
    val av = estado.avaliacao ?: return
    val habilitado = !estado.enviando && !estado.carregando && !estado.semConexao
    val gravados = av.exames.orEmpty().associateBy { it.codigo }
    Surface(Modifier.fillMaxSize().testTag("lancar-exames"), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Dimens.margemCompacta), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
            BotaoTexto(stringResource(Res.string.exn_voltar), acoes.aoVoltar, habilitado = !estado.enviando)
            Text(stringResource(Res.string.exn_titulo), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            Text(stringResource(Res.string.exn_subtitulo, textoData(av.dataHora), nomePaciente), style = MaterialTheme.typography.bodySmall)
            if (estado.semConexao) FaixaOffline()
            estado.erro?.let { Text(textoMensagemExames(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
            if (estado.carregando) EstadoCarregando()
            if (estado.catalogo.isNotEmpty()) {
                CampoTexto(stringResource(Res.string.exn_buscar), estado.busca, acoes.aoBuscar, Modifier.fillMaxWidth())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                    FilterChip(estado.secao == null, { acoes.aoSecao(null) }, { Text(stringResource(Res.string.exn_todas)) })
                    estado.secoes.forEach { s -> FilterChip(estado.secao == s, { acoes.aoSecao(s) }, { Text(s) }) }
                }
                if (estado.visiveis.isEmpty()) EstadoVazio(mensagem = stringResource(Res.string.exn_nenhum))
                estado.visiveis.forEach { LinhaExame(it, estado, gravados[it.codigo]?.rotulo, habilitado, acoes) }
                Row(Modifier.fillMaxWidth().toggleable(estado.mostrarAoPaciente, role = Role.Switch, onValueChange = acoes.aoMostrar), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
                    Text(stringResource(Res.string.exn_mostrar), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Switch(estado.mostrarAoPaciente, onCheckedChange = null)
                }
                if (estado.exigeJustificativa) CampoTexto(stringResource(Res.string.avaliacao_justificativa), estado.justificativa, acoes.aoJustificativa, Modifier.fillMaxWidth())
                BotaoPrincipal(stringResource(Res.string.exn_salvar), acoes.aoSalvar, Modifier.fillMaxWidth(), habilitado = habilitado, carregando = estado.enviando)
            }
        }
    }
}

@Composable
private fun LinhaExame(e: ExameCatalogoDto, estado: EstadoExamesNutri, rotuloGravado: String?, habilitado: Boolean, acoes: AcoesExames) {
    CardDieta(Modifier.fillMaxWidth().testTag("exame-${e.codigo}")) {
        Text(e.nome, style = MaterialTheme.typography.titleSmall)
        val valor = estado.valores[e.codigo].orEmpty()
        val erro = if (e.codigo in estado.invalidos) stringResource(Res.string.exn_numero_invalido) else null
        val rotulo = e.unidade?.let { "${stringResource(Res.string.exn_resultado)} ($it)" } ?: stringResource(Res.string.exn_resultado)
        if (e.qualitativo) CampoTexto(rotulo, valor, { acoes.aoValor(e.codigo, it) }, Modifier.fillMaxWidth(), erro, habilitado)
        else CampoNumero(rotulo, valor, { acoes.aoValor(e.codigo, it) }, Modifier.fillMaxWidth(), erro, habilitado)
        if (e.condicoes.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
            e.condicoes.forEach { c -> FilterChip(estado.condicoes[e.codigo] == c, { acoes.aoCondicao(e.codigo, if (estado.condicoes[e.codigo] == c) null else c) }, { Text(c) }) }
        }
        if (rotuloGravado != null && valor.isNotBlank()) ChipStatus(when (classeDoRotulo(rotuloGravado)) { ClasseFaixa.NORMAL -> EstiloStatus.FEITA; ClasseFaixa.ALTO -> EstiloStatus.ALERTA; else -> EstiloStatus.INFO }, texto = rotuloLegivel(rotuloGravado))
    }
}

@Composable
fun textoMensagemExames(m: MensagemExames): String = stringResource(when (m) {
    MensagemExames.SEM_CONEXAO -> Res.string.exn_erro_sem_conexao
    MensagemExames.SEM_NASCIMENTO -> Res.string.exn_erro_sem_nascimento
    MensagemExames.NUMERO_INVALIDO -> Res.string.exn_erro_numero
    MensagemExames.CONFLITO -> Res.string.exn_erro_conflito
    MensagemExames.JUSTIFICATIVA -> Res.string.exn_erro_justificativa
    MensagemExames.ASSINATURA_INATIVA -> Res.string.exn_erro_assinatura
    MensagemExames.VINCULO_ENCERRADO -> Res.string.exn_erro_vinculo
    MensagemExames.NAO_ENCONTRADO -> Res.string.exn_erro_nao_encontrado
    MensagemExames.TAXA_EXCEDIDA -> Res.string.exn_erro_taxa
    MensagemExames.ERRO -> Res.string.exn_erro_generico
    MensagemExames.SALVO -> Res.string.exn_salvo
})
