package org.br.dietamais.ui.comum.anamnese

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.dominio.anamnese.*
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.CardDieta
import org.br.dietamais.ui.paciente.anamnese.CampoPergunta
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource
import kotlin.uuid.Uuid

/** Agrupa pelo código estável, nunca pelo texto editável do modelo. Códigos futuros usam o formulário genérico. */
private fun chaveGrupo(p: PerguntaQuestionario): String? = when (assuntoAnamnese(p.codigo)) {
    AssuntoAnamnese.PRESSAO -> "PA"
    AssuntoAnamnese.SAL -> "SAL"
    AssuntoAnamnese.OLEO -> "OLEO"
    AssuntoAnamnese.ALCOOL -> p.codigo!!.removeSuffix("_UNIDADE").removeSuffix("_QUANTIDADE").removeSuffix("_DIAS_SEMANA").let { if (it == "ALCOOL_BEBIDA") "ALCOOL" else it }
    AssuntoAnamnese.ATIVIDADE -> p.codigo!!.removeSuffix("_INICIO").removeSuffix("_FIM")
    null -> null
}

@Composable
fun CamposAnamneseEstruturada(perguntas: List<PerguntaQuestionario>, entradas: Map<Uuid, EntradaResposta>, respostas: Map<Uuid, kotlinx.serialization.json.JsonElement>,
    erros: Map<Uuid, ErroResposta>, habilitado: Boolean, aoMudar: (Uuid, EntradaResposta) -> Unit) {
    val grupos = perguntas.groupBy(::chaveGrupo)
    grupos.forEach { (chave, grupo) ->
        if (chave == null) grupo.forEach { CampoPergunta(it, entradas[it.chave] ?: entradaInicial(it, respostas[it.chave]), erros[it.chave], habilitado) { entrada -> aoMudar(it.chave, entrada) } }
        else {
            val assunto = checkNotNull(assuntoAnamnese(grupo.first().codigo))
            val expansivel = chave.startsWith("ALCOOL_") || chave.startsWith("ATIVIDADE_")
            val titulo = if (expansivel) grupo.first().texto.substringBefore(':') else tituloAssunto(assunto)
            var aberto by rememberSaveable(chave) { mutableStateOf(!expansivel || grupo.any { respostas[it.chave] != null }) }
            val temErro = grupo.any { it.chave in erros }
            LaunchedEffect(chave, temErro) { if (temErro) aberto = true }
            val estadoGrupo = stringResource(if (aberto) Res.string.ana_expandido else Res.string.ana_recolhido)
            CardDieta(Modifier.fillMaxWidth()) {
                if (expansivel) TextButton({ aberto = !aberto }, modifier = Modifier.fillMaxWidth().heightIn(min = Dimens.alvoToque).semantics {
                    stateDescription = estadoGrupo
                }) {
                    Text(stringResource(if (aberto) Res.string.ana_ocultar_campos else Res.string.ana_mostrar_campos, titulo))
                } else Text(titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                if (aberto) {
                    val pareados = grupo.filter { it.tipo == TipoPergunta.NUMERO || it.tipo == TipoPergunta.HORARIO }
                    grupo.filter { it !in pareados }.forEach { p -> CampoPergunta(p, entradas[p.chave] ?: entradaInicial(p, respostas[p.chave]), erros[p.chave], habilitado) { aoMudar(p.chave, it) } }
                    val colunas = if (LocalDensity.current.fontScale > 1.3f) 1 else 2
                    pareados.chunked(colunas).forEach { linha ->
                        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.espacoPequeno)) {
                            linha.forEach { p -> Box(Modifier.weight(1f)) { CampoPergunta(p, entradas[p.chave] ?: entradaInicial(p, respostas[p.chave]), erros[p.chave], habilitado) { aoMudar(p.chave, it) } } }
                            if (linha.size < colunas) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun tituloAssunto(assunto: AssuntoAnamnese): String = stringResource(when (assunto) {
    AssuntoAnamnese.PRESSAO -> Res.string.ana_pressao
    AssuntoAnamnese.ALCOOL -> Res.string.ana_alcool
    AssuntoAnamnese.SAL -> Res.string.ana_sal
    AssuntoAnamnese.OLEO -> Res.string.ana_oleo
    AssuntoAnamnese.ATIVIDADE -> Res.string.ana_atividade
})
