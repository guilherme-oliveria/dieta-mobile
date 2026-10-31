package org.br.dietamais.dominio.calculos

/** Nível de atividade usado no GET. */
enum class NivelAtividade { SEDENTARIO, LEVE, MODERADO, INTENSO }
enum class ObjetivoPeso { MANUTENCAO, GANHO, PERDA }
enum class PeriodoLactacao { ATE_6_MESES, DE_7_A_12_MESES }
enum class ProtocoloCarboidrato { FAIXA_DRI, MINIMO_RDA, FAIXA_ESPORTIVA, POR_KG }
enum class ProtocoloProteina { FAIXA_DRI, RDA, MINIMO_15, ENDURANCE, HIPERTROFIA }
enum class ProtocoloLipidio { MINIMO_20, MINIMO_25 }

/** Faixa de um macronutriente em % do VET e em gramas; [percentualMaximo] e [gramasMaximo] nulos quando só há mínimo. */
data class MetaMacro(val percentualMinimo: Double, val percentualMaximo: Double?, val gramasMinimo: Double, val gramasMaximo: Double?)

/** Frações do lipídio total para dislipidêmico (SBC 2007). */
data class Lipidios(val total: MetaMacro, val saturado: MetaMacro, val monoinsaturado: MetaMacro, val poliinsaturado: MetaMacro, val trans: MetaMacro)

data class AtividadeDia(val met: Double, val minutos: Double)
data class EntradaGetPorMet(
    val sexo: Sexo, val pesoKg: Double, val massaMagraKg: Double, val horasSono: Double, val horasTrabalho: Double,
    val intensidadeTrabalho: NivelAtividade, val atividades: List<AtividadeDia>,
)
data class ResultadoGetPorMet(
    val gebDia: Double, val gebHora: Double, val gastoSono: Double, val gastoTrabalho: Double, val gastoAtividades: List<Double>,
    val horasRotina: Double, val gastoRotina: Double, val get: Double,
)

/**
 * Gasto energético e metas de macronutrientes de adulto e idoso, porta da aba "macronutrientes e calorias" e da aba MET
 * (plano das planilhas, seção 3.2). Os mesmos vetores do backend (`contrato/calculos/energia.json` e `met.json`) rodam em
 * `CalculosEnergiaTest`. Funções puras; é só prévia (RN-CAL-01): quem grava o resultado é o backend. kcal, g e % com 2 casas.
 */
object CalculosEnergia {
    const val MAXIMO_ATIVIDADES = 5
    private fun esc(v: Double) = CalculosAdulto.arredondar(v, 2)

    // ---------- GEB (kcal/dia) ----------
    fun gebFao1985(sexo: Sexo, idadeAnos: Int, pesoKg: Double): Double {
        val c = when (sexo) {
            Sexo.MASCULINO -> arrayOf(15.3 to 679.0, 11.6 to 879.0, 13.5 to 487.0)
            Sexo.FEMININO -> arrayOf(14.7 to 496.0, 8.7 to 829.0, 10.5 to 596.0)
        }[faixa(idadeAnos)]
        return esc(c.first * pesoKg + c.second)
    }

    fun gebFao2004(sexo: Sexo, idadeAnos: Int, pesoKg: Double): Double {
        val c = when (sexo) {
            Sexo.MASCULINO -> arrayOf(15.057 to 692.2, 11.472 to 873.1, 11.711 to 587.7)
            Sexo.FEMININO -> arrayOf(14.818 to 486.6, 8.126 to 845.6, 9.082 to 658.5)
        }[faixa(idadeAnos)]
        return esc(c.first * pesoKg + c.second)
    }

    /** IOM 2002; [sobrepesoOuObeso] escolhe o par de fórmulas (eutrófico/magro × sobrepeso/obeso). */
    fun gebIom2002(sexo: Sexo, idadeAnos: Int, pesoKg: Double, estaturaM: Double, sobrepesoOuObeso: Boolean): Double = esc(
        when (sexo) {
            Sexo.MASCULINO ->
                if (sobrepesoOuObeso) 293 - 3.8 * idadeAnos + 456.4 * estaturaM + 10.12 * pesoKg else 204 - 4 * idadeAnos + 450.5 * estaturaM + 11.69 * pesoKg
            Sexo.FEMININO ->
                if (sobrepesoOuObeso) 247 - 2.67 * idadeAnos + 401.5 * estaturaM + 8.6 * pesoKg else 255 - 2.35 * idadeAnos + 361.6 * estaturaM + 9.39 * pesoKg
        },
    )

    /** Cunningham 1991: [massaMagraKg] é a massa magra (ou músculo + osso + residual no protocolo 2). */
    fun gebCunningham(massaMagraKg: Double): Double = esc(370 + 21.6 * massaMagraKg)

    // ---------- GET (kcal/dia) ----------
    fun getFao1985(gebKcal: Double, sexo: Sexo, nivel: NivelAtividade): Double {
        val h = sexo == Sexo.MASCULINO
        val fator = when (nivel) {
            NivelAtividade.SEDENTARIO -> 1.3
            NivelAtividade.LEVE -> if (h) 1.55 else 1.56
            NivelAtividade.MODERADO -> if (h) 1.78 else 1.64
            NivelAtividade.INTENSO -> if (h) 2.1 else 1.82
        }
        return esc(gebKcal * fator)
    }

    /** FAO 2004: mesmos fatores para os dois sexos; lactante soma 675 kcal. */
    fun getFao2004(gebKcal: Double, nivel: NivelAtividade, lactante: Boolean): Double {
        val fator = when (nivel) { NivelAtividade.SEDENTARIO, NivelAtividade.LEVE -> 1.53; NivelAtividade.MODERADO -> 1.76; NivelAtividade.INTENSO -> 2.25 }
        return esc(gebKcal * fator + if (lactante) 675 else 0)
    }

    fun getIom2005(gebIomKcal: Double, nivel: NivelAtividade): Double =
        esc(gebIomKcal * when (nivel) { NivelAtividade.SEDENTARIO -> 1.2; NivelAtividade.LEVE -> 1.5; NivelAtividade.MODERADO -> 1.75; NivelAtividade.INTENSO -> 2.2 })

    /** RDI 2005 (EER); [lactacao] nulo = não lactante. */
    fun getRdi2005(sexo: Sexo, idadeAnos: Int, pesoKg: Double, estaturaM: Double, nivel: NivelAtividade, lactacao: PeriodoLactacao?): Double {
        val eer = if (sexo == Sexo.MASCULINO) {
            val af = when (nivel) { NivelAtividade.SEDENTARIO -> 1.0; NivelAtividade.LEVE -> 1.11; NivelAtividade.MODERADO -> 1.25; NivelAtividade.INTENSO -> 1.48 }
            662 - 9.53 * idadeAnos + af * (15.91 * pesoKg + 539.36 * estaturaM)
        } else {
            val af = when (nivel) { NivelAtividade.SEDENTARIO -> 1.0; NivelAtividade.LEVE -> 1.12; NivelAtividade.MODERADO -> 1.27; NivelAtividade.INTENSO -> 1.45 }
            val base = 354 - 6.91 * idadeAnos + af * (9.36 * pesoKg + 727 * estaturaM)
            base + when (lactacao) { null -> 0.0; PeriodoLactacao.ATE_6_MESES -> 330.0; PeriodoLactacao.DE_7_A_12_MESES -> 400.0 }
        }
        return esc(eer)
    }

    // ---------- VET ----------
    /** Manutenção = GET; ganho soma e perda subtrai `(kg/mês ÷ 4 × 1000) × 500/450` kcal por dia (a planilha, tal como está). */
    fun vet(getKcal: Double, objetivo: ObjetivoPeso, kgPorMes: Double?): Double {
        if (objetivo == ObjetivoPeso.MANUTENCAO || kgPorMes == null) return esc(getKcal)
        val ajuste = kgPorMes / 4 * 1000 * 500 / 450
        return esc(if (objetivo == ObjetivoPeso.GANHO) getKcal + ajuste else getKcal - ajuste)
    }

    fun kcalPorKg(vetKcal: Double, pesoKg: Double): Double = esc(vetKcal / pesoKg)

    // ---------- Metas de macronutrientes ----------
    fun carboidrato(protocolo: ProtocoloCarboidrato, vetKcal: Double, pesoKg: Double, gestante: Boolean, lactante: Boolean): MetaMacro = when (protocolo) {
        ProtocoloCarboidrato.FAIXA_DRI -> porPercentual(vetKcal, 45.0, 65.0, 4.0)
        ProtocoloCarboidrato.FAIXA_ESPORTIVA -> porPercentual(vetKcal, 50.0, 60.0, 4.0)
        ProtocoloCarboidrato.POR_KG -> porKg(vetKcal, pesoKg, 5.0, 10.0, 4.0)
        ProtocoloCarboidrato.MINIMO_RDA -> minimoEmGramas(vetKcal, if (lactante) 210.0 else if (gestante) 175.0 else 130.0, 4.0)
    }

    /** RDA (protocolo 2), da macro `RDI_PTN`: 71 g para gestante e lactante; adulto 56 g (homem) e 46 g (mulher). */
    fun proteina(protocolo: ProtocoloProteina, vetKcal: Double, pesoKg: Double, sexo: Sexo, gestante: Boolean, lactante: Boolean): MetaMacro = when (protocolo) {
        ProtocoloProteina.FAIXA_DRI -> porPercentual(vetKcal, 10.0, 35.0, 4.0)
        ProtocoloProteina.MINIMO_15 -> MetaMacro(15.0, null, gramas(vetKcal, 15.0, 4.0), null)
        ProtocoloProteina.ENDURANCE -> porKg(vetKcal, pesoKg, 1.2, 1.6, 4.0)
        ProtocoloProteina.HIPERTROFIA -> porKg(vetKcal, pesoKg, 1.4, 1.8, 4.0)
        ProtocoloProteina.RDA -> minimoEmGramas(vetKcal, if (lactante || gestante) 71.0 else if (sexo == Sexo.MASCULINO) 56.0 else 46.0, 4.0)
    }

    fun lipidio(protocolo: ProtocoloLipidio, vetKcal: Double): MetaMacro = porPercentual(vetKcal, if (protocolo == ProtocoloLipidio.MINIMO_20) 20.0 else 25.0, 35.0, 9.0)

    /** Dislipidêmico (SBC 2007): saturado 7%, mono 20%, poli 10%, trans 1% do VET. */
    fun lipidiosDislipidemico(vetKcal: Double) =
        Lipidios(porPercentual(vetKcal, 20.0, 35.0, 9.0), fixo(vetKcal, 7.0), fixo(vetKcal, 20.0), fixo(vetKcal, 10.0), fixo(vetKcal, 1.0))

    /** 300 mg/dia, ou 200 se o colesterol total da bioquímica for 200 ou mais. */
    fun colesterolMaximoMg(colesterolTotal: Double?): Double = if (colesterolTotal != null && colesterolTotal >= 200) 200.0 else 300.0

    // ---------- GET por MET ----------
    /** `GEB/h = Cunningham/24`; sono ×0,9; trabalho ×fator; até 5 atividades `minutos/60 × MET × peso`; rotina ×1,3. O MET vem da lista do backend. */
    fun getPorMet(e: EntradaGetPorMet): ResultadoGetPorMet {
        require(e.atividades.size <= MAXIMO_ATIVIDADES) { "No máximo $MAXIMO_ATIVIDADES atividades" }
        val gebDia = gebCunningham(e.massaMagraKg)
        val gebHora = gebDia / 24
        val sono = gebHora * e.horasSono * 0.9
        val trabalho = gebHora * e.horasTrabalho * fatorTrabalho(e.sexo, e.intensidadeTrabalho)
        val gastos = e.atividades.map { it.minutos / 60 * it.met * e.pesoKg }
        val horasRotina = 24 - e.horasSono - e.horasTrabalho - e.atividades.sumOf { it.minutos } / 60
        require(horasRotina >= 0) { "As horas do dia passam de 24" }
        val rotina = gebHora * horasRotina * 1.3
        return ResultadoGetPorMet(gebDia, esc(gebHora), esc(sono), esc(trabalho), gastos.map(::esc), esc(horasRotina), esc(rotina), esc(sono + trabalho + gastos.sum() + rotina))
    }

    private fun fatorTrabalho(sexo: Sexo, nivel: NivelAtividade): Double {
        val h = sexo == Sexo.MASCULINO
        return when (nivel) {
            NivelAtividade.LEVE -> if (h) 1.55 else 1.56
            NivelAtividade.MODERADO -> if (h) 1.78 else 1.64
            NivelAtividade.INTENSO -> if (h) 2.1 else 1.82
            NivelAtividade.SEDENTARIO -> throw IllegalArgumentException("Intensidade do trabalho: leve, moderada ou intensa")
        }
    }

    // ---------- auxiliares ----------
    private fun faixa(idade: Int): Int {
        require(idade >= 18) { "Fórmula de adulto: idade $idade" }
        return if (idade < 30) 0 else if (idade < 60) 1 else 2
    }
    private fun gramas(vet: Double, percentual: Double, kcalPorG: Double) = esc(percentual * vet / 100 / kcalPorG)
    private fun percentualDe(gramas: Double, vet: Double, kcalPorG: Double) = esc(gramas * kcalPorG * 100 / vet)
    private fun porPercentual(vet: Double, min: Double, max: Double, k: Double) = MetaMacro(esc(min), esc(max), gramas(vet, min, k), gramas(vet, max, k))
    private fun fixo(vet: Double, p: Double) = MetaMacro(esc(p), esc(p), gramas(vet, p, 9.0), gramas(vet, p, 9.0))
    private fun porKg(vet: Double, peso: Double, gMin: Double, gMax: Double, k: Double) =
        MetaMacro(percentualDe(gMin * peso, vet, k), percentualDe(gMax * peso, vet, k), esc(gMin * peso), esc(gMax * peso))
    private fun minimoEmGramas(vet: Double, gMin: Double, k: Double) = MetaMacro(percentualDe(gMin, vet, k), null, esc(gMin), null)
}
