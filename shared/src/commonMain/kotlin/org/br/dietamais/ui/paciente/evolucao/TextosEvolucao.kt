package org.br.dietamais.ui.paciente.evolucao

import androidx.compose.runtime.Composable
import org.br.dietamais.dominio.avaliacao.*
import org.br.dietamais.recursos.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

fun rotuloSerie(serie: SerieEvolucao): StringResource = when (serie) {
    SerieEvolucao.PESO -> Res.string.evolucao_serie_peso
    SerieEvolucao.PESO_EM_CASA -> Res.string.evolucao_serie_peso_em_casa
    SerieEvolucao.IMC -> Res.string.evolucao_serie_imc
    SerieEvolucao.PERCENTUAL_GORDURA -> Res.string.evolucao_serie_percentual_gordura
    SerieEvolucao.PESO_GORDO -> Res.string.evolucao_serie_peso_gordo
    SerieEvolucao.PESO_MAGRO -> Res.string.evolucao_serie_peso_magro
    SerieEvolucao.CINTURA -> Res.string.evolucao_serie_cintura
    SerieEvolucao.ABDOMEN -> Res.string.evolucao_serie_abdomen
    SerieEvolucao.QUADRIL -> Res.string.evolucao_serie_quadril
    SerieEvolucao.TORAX -> Res.string.evolucao_serie_torax
    SerieEvolucao.PESCOCO -> Res.string.evolucao_serie_pescoco
    SerieEvolucao.BRACO -> Res.string.evolucao_serie_braco
    SerieEvolucao.ANTEBRACO -> Res.string.evolucao_serie_antebraco
    SerieEvolucao.COXA -> Res.string.evolucao_serie_coxa
    SerieEvolucao.PANTURRILHA -> Res.string.evolucao_serie_panturrilha
}

fun rotuloIntervalo(intervalo: IntervaloEvolucao): StringResource = when (intervalo) {
    IntervaloEvolucao.TRES_MESES -> Res.string.evolucao_intervalo_3m
    IntervaloEvolucao.SEIS_MESES -> Res.string.evolucao_intervalo_6m
    IntervaloEvolucao.UM_ANO -> Res.string.evolucao_intervalo_1a
    IntervaloEvolucao.TUDO -> Res.string.evolucao_intervalo_tudo
}

@Composable
fun textoMensagemEvolucao(mensagem: MensagemEvolucao): String = stringResource(when (mensagem) {
    MensagemEvolucao.PESO_INVALIDO -> Res.string.evolucao_peso_erro
    MensagemEvolucao.PESO_SALVO -> Res.string.evolucao_peso_salvo
    MensagemEvolucao.ERRO -> Res.string.evolucao_erro
})
