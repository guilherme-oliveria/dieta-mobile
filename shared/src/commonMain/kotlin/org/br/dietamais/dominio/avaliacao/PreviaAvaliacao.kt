package org.br.dietamais.dominio.avaliacao

import kotlinx.datetime.LocalDate
import org.br.dietamais.data.remoto.dto.Sexo as SexoDto
import org.br.dietamais.dominio.calculos.*

/** Dado que falta para um resultado aparecer (RN-CAL-04). A tela mostra uma frase por item, nunca zero no lugar do número. */
enum class FaltaDado { SEXO, NASCIMENTO, FAIXA_ETARIA, PESO_ESTATURA, PUNHO, CINTURA, DOBRAS, DIAMETRO_PUNHO, DIAMETRO_JOELHO, PERCENTUAL_DESEJADO }

/** Prévia local enquanto a nutri digita (RN-CAL-01: o número gravado é o do servidor). `resultado` nulo = nada calculável ainda. */
data class PreviaAvaliacao(val resultado: ResultadoAvaliacaoAdulto?, val idadeAnos: Int?, val faltas: List<FaltaDado>)

/** Sobe o que falta por seção da planilha; só adulto e idoso (18+) calculam por enquanto, as outras faixas entram nas ondas 22. */
fun previaAvaliacao(valores: Map<MedidaAvaliacao, String>, sexo: SexoDto?, nascimento: LocalDate?, dia: LocalDate,
    dobras: ProtocoloDobras = ProtocoloDobras.SETE, massa: ProtocoloMassa = ProtocoloMassa.MAGRO): PreviaAvaliacao {
    val faltas = ArrayList<FaltaDado>()
    fun v(m: MedidaAvaliacao) = lerDecimal(valores[m].orEmpty())?.takeIf { it in m.min..m.max }
    val peso = v(MedidaAvaliacao.PESO); val estatura = v(MedidaAvaliacao.ESTATURA)
    if (peso == null || estatura == null) return PreviaAvaliacao(null, null, listOf(FaltaDado.PESO_ESTATURA))
    val idade = nascimento?.let { CalculosAdulto.idadeEmAnos(dia.toEpochDays() - it.toEpochDays()) }?.takeIf { it >= 0 }
    if (idade == null) faltas += FaltaDado.NASCIMENTO
    val sexoCalculo = when (sexo) { SexoDto.MASCULINO -> Sexo.MASCULINO; SexoDto.FEMININO -> Sexo.FEMININO; else -> null }
    if (sexoCalculo == null) faltas += FaltaDado.SEXO
    if (idade != null && idade < 18) return PreviaAvaliacao(null, idade, faltas + FaltaDado.FAIXA_ETARIA)
    if (idade == null || sexoCalculo == null) return PreviaAvaliacao(null, idade, faltas)
    val resultado = CalculosAdulto.avaliar(EntradaAvaliacaoAdulto(sexoCalculo, idade, peso, estatura, dobras, massa,
        DobrasCalculo(v(MedidaAvaliacao.DOBRA_TRICEPS), v(MedidaAvaliacao.DOBRA_PEITORAL), v(MedidaAvaliacao.DOBRA_AXILAR_MEDIA), v(MedidaAvaliacao.DOBRA_SUBESCAPULAR),
            v(MedidaAvaliacao.DOBRA_ABDOMINAL), v(MedidaAvaliacao.DOBRA_SUPRAILIACA), v(MedidaAvaliacao.DOBRA_COXA)),
        v(MedidaAvaliacao.CINTURA), v(MedidaAvaliacao.PUNHO), v(MedidaAvaliacao.DIAMETRO_PUNHO), v(MedidaAvaliacao.DIAMETRO_JOELHO), v(MedidaAvaliacao.PERCENTUAL_DESEJADO)))
    resultado.motivosNulos.values.mapNotNull { motivo ->
        when (motivo) {
            Motivo.FALTA_PUNHO -> FaltaDado.PUNHO; Motivo.FALTA_CINTURA -> FaltaDado.CINTURA; Motivo.FALTA_DOBRAS -> FaltaDado.DOBRAS
            Motivo.FALTA_DIAMETRO_PUNHO -> FaltaDado.DIAMETRO_PUNHO; Motivo.FALTA_DIAMETRO_JOELHO -> FaltaDado.DIAMETRO_JOELHO
            Motivo.FALTA_PERCENTUAL_DESEJADO -> FaltaDado.PERCENTUAL_DESEJADO; else -> null
        }
    }.distinct().let { faltas += it }
    return PreviaAvaliacao(resultado, idade, faltas)
}

/** "Repetir medidas da última avaliação": copia básicas, perímetros, dobras e complementares; a composição é recalculada. */
fun medidasParaRepetir(ultima: org.br.dietamais.data.remoto.dto.AvaliacaoResposta): Map<MedidaAvaliacao, String> =
    valoresDe(ultima).filterKeys { it.secao != SecaoAvaliacao.COMPOSICAO }

/** Passos do formulário (a "Necessidades" entra entre o 2 e o 3 no MOB-52). */
enum class PassoAvaliacao { MEDIDAS, COMPOSICAO, RESUMO }

/** Medidas que o passo exige antes de avançar: só peso e estatura, no passo 1. */
fun medidasDoPasso(passo: PassoAvaliacao): List<MedidaAvaliacao> = when (passo) {
    PassoAvaliacao.MEDIDAS -> MedidaAvaliacao.entries.filter { it.secao != SecaoAvaliacao.COMPOSICAO }
    PassoAvaliacao.COMPOSICAO -> MedidaAvaliacao.entries.filter { it.secao == SecaoAvaliacao.COMPOSICAO }
    PassoAvaliacao.RESUMO -> emptyList()
}

/** Dobras que o protocolo usa: 7 dobras todas; 3 dobras por sexo (Jackson e Pollock). Sem sexo, mostra as 7 para a nutri não perder campo. */
fun dobrasDoProtocolo(protocolo: ProtocoloDobras, sexo: SexoDto?): Set<MedidaAvaliacao> = when {
    protocolo == ProtocoloDobras.SETE -> MedidaAvaliacao.entries.filter { it.secao == SecaoAvaliacao.DOBRAS }.toSet()
    sexo == SexoDto.MASCULINO -> setOf(MedidaAvaliacao.DOBRA_PEITORAL, MedidaAvaliacao.DOBRA_ABDOMINAL, MedidaAvaliacao.DOBRA_COXA)
    sexo == SexoDto.FEMININO -> setOf(MedidaAvaliacao.DOBRA_TRICEPS, MedidaAvaliacao.DOBRA_SUPRAILIACA, MedidaAvaliacao.DOBRA_COXA)
    else -> MedidaAvaliacao.entries.filter { it.secao == SecaoAvaliacao.DOBRAS }.toSet()
}
