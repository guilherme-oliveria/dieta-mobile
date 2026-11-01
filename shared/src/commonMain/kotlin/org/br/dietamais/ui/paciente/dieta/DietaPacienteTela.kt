package org.br.dietamais.ui.paciente.dieta

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.br.dietamais.ui.nutri.dieta.formatar
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource
import kotlin.uuid.Uuid

/** Aba Dieta: refeições detalhadas, orientações e escolha da dieta em uso quando há mais de uma nutri (MOB-14). */
@Composable
fun DietaPacienteTela(estado: EstadoDietaPaciente, aoAbrirRefeicao: (Uuid?) -> Unit, aoEscolher: (Uuid) -> Unit, modifier: Modifier = Modifier,
    pdf: @Composable (EstadoDietaPaciente) -> Unit = {}) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Dimens.paddingCard),
        verticalArrangement = Arrangement.spacedBy(Dimens.espacoMedio)) {
        Text(stringResource(Res.string.aba_dieta), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        estado.erro?.let { Text(stringResource(textoErro(it)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        val dieta = estado.dieta
        when {
            !estado.carregado -> EstadoCarregando()
            dieta == null -> EstadoVazio(mensagem = stringResource(Res.string.pac_sem_dieta))
            else -> {
                Text(dieta.titulo, style = MaterialTheme.typography.titleLarge)
                Text(stringResource(Res.string.pac_dieta_nutri, dieta.nutricionista.nome, dieta.nutricionista.crn), style = MaterialTheme.typography.bodySmall)
                Text(stringResource(Res.string.pac_dieta_total, formatar(dieta.totais.kcal)), style = MaterialTheme.typography.labelLarge)
                Text(stringResource(Res.string.pac_dieta_macros, formatar(dieta.totais.carboidratos), formatar(dieta.totais.proteinas), formatar(dieta.totais.gorduras)),
                    style = MaterialTheme.typography.bodySmall)
                pdf(estado)
                if (estado.alternativas.isNotEmpty()) CardDieta(Modifier.fillMaxWidth()) {
                    Text(stringResource(Res.string.pac_dieta_escolher), style = MaterialTheme.typography.titleMedium)
                    estado.alternativas.forEach { s ->
                        Text("${s.nutricionista.nome} · ${s.titulo}", style = MaterialTheme.typography.bodyMedium)
                        if (s.dietaId == dieta.dietaId) Text(stringResource(Res.string.pac_dieta_em_uso), style = MaterialTheme.typography.labelLarge)
                        else BotaoTexto(stringResource(Res.string.pac_dieta_usar), { aoEscolher(s.dietaId) }, habilitado = !estado.semConexao && !estado.enviando)
                    }
                }
                estado.refeicoes.forEach { r ->
                    val aberta = estado.refeicaoAberta == r.id
                    CardDieta(Modifier.fillMaxWidth()) {
                        Text(stringResource(Res.string.refeicao_nome_horario, r.nome, r.horario.toString().take(5)), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(Res.string.pac_dieta_kcal_refeicao, formatar(r.totais.kcal)), style = MaterialTheme.typography.labelLarge)
                        Text(stringResource(Res.string.pac_dieta_macros, formatar(r.totais.carboidratos), formatar(r.totais.proteinas), formatar(r.totais.gorduras)),
                            style = MaterialTheme.typography.bodySmall)
                        if (aberta) r.itens.forEach { i ->
                            val nome = i.textoLivre ?: i.nomeAlimento.orEmpty()
                            val gramas = i.gramas
                            if (i.aVontade == true) Text(stringResource(Res.string.pac_dieta_item_a_vontade, nome), style = MaterialTheme.typography.bodyMedium)
                            else if (gramas != null) Text(stringResource(Res.string.pac_dieta_item, nome, i.medidaCaseira.orEmpty(), formatar(gramas)), style = MaterialTheme.typography.bodyMedium)
                            else Text(nome, style = MaterialTheme.typography.bodyMedium)
                            val porcoes = i.porcoes
                            i.grupoNome?.takeIf { it.isNotBlank() && porcoes != null }?.let { grupo ->
                                Text(stringResource(if (porcoes == 1.0) Res.string.pac_dieta_porcao else Res.string.pac_dieta_porcoes, formatar(porcoes!!), grupo), style = MaterialTheme.typography.bodySmall)
                            }
                            i.observacao?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        }
                        BotaoTexto(stringResource(if (aberta) Res.string.pac_dieta_ocultar else Res.string.pac_dieta_ver), { aoAbrirRefeicao(if (aberta) null else r.id) })
                    }
                }
                dieta.orientacao?.takeIf { it.isNotBlank() }?.let {
                    Text(stringResource(Res.string.pac_dieta_orientacoes), style = MaterialTheme.typography.titleMedium)
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

private fun textoErro(m: MensagemDietaPaciente) = when (m) {
    MensagemDietaPaciente.SEM_CONEXAO -> Res.string.pac_erro_sem_conexao
    MensagemDietaPaciente.CONFLITO -> Res.string.pac_erro_conflito
    MensagemDietaPaciente.ERRO -> Res.string.pac_erro_geral
}
