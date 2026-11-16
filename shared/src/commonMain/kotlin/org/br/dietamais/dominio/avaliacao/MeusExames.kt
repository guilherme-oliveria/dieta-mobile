package org.br.dietamais.dominio.avaliacao

import org.br.dietamais.data.remoto.dto.AvaliacaoResposta
import org.br.dietamais.data.remoto.dto.ExameResultadoDto
import org.br.dietamais.data.remoto.dto.FaixaExameDto
import org.br.dietamais.dominio.graficos.SegmentoFaixa
import kotlin.time.Instant

/** Como a faixa se parece para a pessoa: define a cor da régua e sempre vem com o texto do rótulo, nunca só a cor. */
enum class ClasseFaixa { ABAIXO, NORMAL, ATENCAO, ALTO, OUTRA }

/** Lê o rótulo da planilha ("normal", "limite superior", "int. à glicose"...) sem depender de lista fechada: rótulo novo cai em [ClasseFaixa.OUTRA]. */
fun classeDoRotulo(rotulo: String): ClasseFaixa {
    val r = rotulo.trim().lowercase()
    return when {
        r.startsWith("muito alto") || r.startsWith("alto") || r.contains("diabetes") || r.contains("hipertens") -> ClasseFaixa.ALTO
        r.startsWith("baixo") || r.startsWith("muito baixo") || r == "limite inferior" || r.contains("deficien") -> ClasseFaixa.ABAIXO
        r == "normal" || r.startsWith("ótimo") || r.startsWith("otimo") || r.startsWith("bom") || r.startsWith("desej") || r.startsWith("adequad") || r.startsWith("negativ") || r == "ausente" || r.startsWith("não reagente") -> ClasseFaixa.NORMAL
        r.contains("limít") || r.contains("limit") || r.contains("limite superior") || r.contains("int.") || r.contains("pré") -> ClasseFaixa.ATENCAO
        else -> ClasseFaixa.OUTRA
    }
}

/**
 * Régua do exame a partir das faixas que o servidor resolveu para o paciente (BE-56c). Faixa de um ponto só (ex.: "limite inferior" em 70 a 70) não ocupa
 * espaço; ponta aberta ganha um quarto da largura média dos trechos fechados, para a régua ter começo e fim. Sem faixa numérica, nulo.
 */
fun segmentosDoExame(faixas: List<FaixaExameDto>): List<SegmentoFaixa>? {
    val uteis = faixas.filter { it.min == null || it.max == null || it.max > it.min }
        .sortedBy { it.min ?: Double.NEGATIVE_INFINITY }
    if (uteis.isEmpty()) return null
    val fechados = uteis.filter { it.min != null && it.max != null }
    val folga = if (fechados.isEmpty()) 1.0 else fechados.sumOf { it.max!! - it.min!! } / fechados.size / 4
    val limites = uteis.mapIndexed { i, f ->
        val de = f.min ?: ((f.max ?: 0.0) - folga.coerceAtLeast(1e-6) * 4)
        val ate = f.max ?: (de + folga.coerceAtLeast(1e-6) * 4)
        // ponta aberta no meio da lista usa o início do próximo trecho
        val proximo = uteis.getOrNull(i + 1)?.min
        SegmentoFaixa(f.rotulo, de.takeIf { f.min != null } ?: de, ate.takeIf { f.max != null } ?: (proximo ?: ate))
    }
    return limites.takeIf { l -> l.all { it.ate > it.de } }
}

/** Primeira letra maiúscula do rótulo da planilha. */
fun rotuloLegivel(rotulo: String): String = rotulo.trim().replaceFirstChar { it.uppercase() }

/** Exames de uma avaliação, em seções na ordem do servidor. */
data class ExamesDaAvaliacao(val avaliacaoId: kotlin.uuid.Uuid, val dataHora: Instant, val exames: List<ExameResultadoDto>)

/**
 * Exames que o paciente pode ver: da avaliação compartilhada mais recente que tenha algum. O servidor só manda `exames` quando a nutri compartilhou
 * (RN-RES-01); aqui só se escolhe a mais recente, offline, a partir do que já está no banco local.
 */
fun meusExames(avaliacoes: List<AvaliacaoResposta>): ExamesDaAvaliacao? =
    avaliacoes.filter { it.compartilhada && !it.exames.isNullOrEmpty() }.maxByOrNull { it.dataHora }
        ?.let { ExamesDaAvaliacao(it.id, it.dataHora, it.exames.orEmpty()) }
