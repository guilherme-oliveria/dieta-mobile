package org.br.dietamais.dominio.calculos

import kotlin.math.floor
import kotlin.math.pow

/**
 * Cálculo antropométrico de adulto e idoso, porta das macros da planilha da Nut. Luciana (plano das planilhas, seção 3.1)
 * com as correções da seção 3.6. Os mesmos vetores do backend (`contrato/calculos/adulto.json`) rodam em `CalculosAdultoTest`.
 * Funções puras; é só prévia (RN-CAL-01): quem grava o resultado é o backend.
 */
object CalculosAdulto {
    const val VERSAO_REFERENCIA = "adulto-1.0.0"

    /** HALF_UP em [casas] casas; o epsilon cobre o erro binário de empates como 27,145. */
    fun arredondar(valor: Double, casas: Int = 2): Double {
        val f = 10.0.pow(casas)
        return floor(valor * f + 0.5 + 1e-9) / f
    }

    /** `INT((data − nascimento)/365,25)`: a fórmula da planilha, guardada para os números baterem. */
    fun idadeEmAnos(dias: Long): Int = floor(dias / 365.25).toInt()

    fun imc(pesoKg: Double, estaturaM: Double): Double = arredondar(pesoKg / (estaturaM * estaturaM), 2)

    fun classificarImcAdulto(imc: Double): ClasseImc = when {
        imc < 16 -> ClasseImc.MG_III
        imc < 17 -> ClasseImc.MG_II
        imc < 18.5 -> ClasseImc.MG_I
        imc < 25 -> ClasseImc.NO
        imc < 30 -> ClasseImc.SP
        imc < 35 -> ClasseImc.OB_I
        imc < 40 -> ClasseImc.OB_II // correção 2 (3.6): a planilha usava < 39
        else -> ClasseImc.OB_III
    }

    fun classificarImcIdoso(idadeAnos: Int, imc: Double): ClasseImc =
        if (idadeAnos < 65) when { imc < 23 -> ClasseImc.MG; imc <= 28 -> ClasseImc.NO; else -> ClasseImc.SP }
        else when { imc < 24 -> ClasseImc.MG; imc <= 29 -> ClasseImc.NO; else -> ClasseImc.SP }

    fun classificarImc(idadeAnos: Int, imc: Double): ClasseImc =
        if (idadeAnos >= 60) classificarImcIdoso(idadeAnos, imc) else classificarImcAdulto(imc)

    fun riscoComorbidades(classe: ClasseImc): RiscoComorbidades = when (classe) {
        ClasseImc.MG_III, ClasseImc.MG_II, ClasseImc.MG_I, ClasseImc.MG -> RiscoComorbidades.B
        ClasseImc.NO -> RiscoComorbidades.ME
        ClasseImc.SP -> RiscoComorbidades.A
        ClasseImc.OB_I -> RiscoComorbidades.MO
        ClasseImc.OB_II -> RiscoComorbidades.S
        ClasseImc.OB_III -> RiscoComorbidades.MS
    }

    fun pesoTeorico(estaturaM: Double, idadeAnos: Int): Pair<Double, Double> {
        val e2 = estaturaM * estaturaM
        return if (idadeAnos >= 60) e2 * 22 to e2 * 27 else e2 * 18.5 to e2 * 24.9
    }

    fun classificarCintura(sexo: Sexo, cm: Double): ClasseCintura =
        if (sexo == Sexo.MASCULINO) when { cm < 94 -> ClasseCintura.B; cm < 102 -> ClasseCintura.A; else -> ClasseCintura.MA }
        else when { cm < 80 -> ClasseCintura.B; cm < 88 -> ClasseCintura.A; else -> ClasseCintura.MA }

    fun classificarCompleicao(sexo: Sexo, estaturaM: Double, punhoCm: Double): Compleicao {
        val r = estaturaM * 100 / punhoCm
        return if (sexo == Sexo.FEMININO) when { r > 11 -> Compleicao.PEQUENA; r >= 10.1 -> Compleicao.MEDIA; else -> Compleicao.GRANDE }
        else when { r > 10.4 -> Compleicao.PEQUENA; r >= 9.6 -> Compleicao.MEDIA; else -> Compleicao.GRANDE }
    }

    // Pollock e Wilmore (1993): t1..t7 por faixa etária (18–25, 26–35, 36–45, 46–55, 56+).
    // p < t1 MT_BXO; t(i) <= p < t(i+1) classe seguinte; t6 <= p <= t7 ALTO; p > t7 MT_ALTO.
    private val LIMITES_MASCULINO = listOf(
        intArrayOf(4, 7, 11, 14, 17, 21, 24), intArrayOf(8, 12, 16, 19, 21, 25, 27), intArrayOf(10, 15, 19, 22, 24, 26, 29),
        intArrayOf(12, 17, 21, 24, 26, 28, 30), intArrayOf(13, 19, 22, 24, 26, 28, 30),
    )
    private val LIMITES_FEMININO = listOf(
        intArrayOf(13, 17, 20, 23, 26, 29, 31), intArrayOf(14, 17, 21, 24, 26, 30, 33), intArrayOf(16, 20, 24, 27, 30, 33, 36),
        intArrayOf(17, 22, 26, 29, 32, 35, 38), intArrayOf(18, 23, 27, 30, 33, 36, 38),
    )

    /** Recebe o % já arredondado em 2 casas. Abaixo de 18 anos não há tabela (nulo). */
    fun classificarPercentualGordura(sexo: Sexo, idadeAnos: Int, percentual: Double): ClassePercentualGordura? {
        if (idadeAnos < 18) return null
        val faixa = when { idadeAnos < 26 -> 0; idadeAnos < 36 -> 1; idadeAnos < 46 -> 2; idadeAnos < 56 -> 3; else -> 4 }
        val t = (if (sexo == Sexo.MASCULINO) LIMITES_MASCULINO else LIMITES_FEMININO)[faixa]
        if (percentual > t[6]) return ClassePercentualGordura.MT_ALTO
        for (k in 6 downTo 1) if (percentual >= t[k - 1]) return ClassePercentualGordura.entries[k]
        return ClassePercentualGordura.MT_BXO
    }

    fun avaliar(e: EntradaAvaliacaoAdulto): ResultadoAvaliacaoAdulto {
        val nulos = LinkedHashMap<String, String>()
        val imc = imc(e.pesoKg, e.estaturaM)
        val classe = classificarImc(e.idadeAnos, imc)
        val risco = riscoComorbidades(classe)
        val (minimo, maximo) = pesoTeorico(e.estaturaM, e.idadeAnos)
        val compleicao = e.punhoCm?.let { classificarCompleicao(e.sexo, e.estaturaM, it) }
        if (compleicao == null) nulos["compleicao"] = Motivo.FALTA_PUNHO
        val cintura = e.cinturaCm?.let { classificarCintura(e.sexo, it) }
        if (cintura == null) nulos["classificacaoCintura"] = Motivo.FALTA_CINTURA

        val d = e.dobras
        val necessarias = when {
            e.protocoloDobras == ProtocoloDobras.SETE -> listOf(d.triceps, d.peitoral, d.axilarMedia, d.subescapular, d.abdominal, d.suprailiaca, d.coxa)
            e.sexo == Sexo.MASCULINO -> listOf(d.peitoral, d.abdominal, d.coxa)
            else -> listOf(d.triceps, d.suprailiaca, d.coxa)
        }
        var densidade: Double? = null; var somatorio: Double? = null; var percentual: Double? = null
        var classePg: ClassePercentualGordura? = null; var gordo: Double? = null
        if (necessarias.all { it != null && it > 0 }) {
            val s7 = (d.triceps ?: 0.0) + (d.peitoral ?: 0.0) + (d.axilarMedia ?: 0.0) + (d.subescapular ?: 0.0) +
                (d.abdominal ?: 0.0) + (d.suprailiaca ?: 0.0) + (d.coxa ?: 0.0)
            val dens = densidade(e.sexo, e.protocoloDobras, d, e.idadeAnos)
            val pg = (4.95 / dens - 4.5) * 100
            somatorio = arredondar(s7); densidade = arredondar(dens, 4); percentual = arredondar(pg)
            classePg = classificarPercentualGordura(e.sexo, e.idadeAnos, percentual)
            gordo = e.pesoKg * pg / 100
            return concluir(e, nulos, imc, classe, risco, minimo, maximo, compleicao, cintura, somatorio, densidade, percentual, classePg, pg, gordo)
        }
        nulos["percentualGordura"] = Motivo.FALTA_DOBRAS
        return concluir(e, nulos, imc, classe, risco, minimo, maximo, compleicao, cintura, somatorio, densidade, percentual, classePg, null, gordo)
    }

    private fun concluir(
        e: EntradaAvaliacaoAdulto, nulos: LinkedHashMap<String, String>, imc: Double, classe: ClasseImc, risco: RiscoComorbidades,
        minimo: Double, maximo: Double, compleicao: Compleicao?, cintura: ClasseCintura?, somatorio: Double?, densidade: Double?,
        percentual: Double?, classePg: ClassePercentualGordura?, pgExato: Double?, gordo: Double?,
    ): ResultadoAvaliacaoAdulto {
        val dp = e.diametroPunhoCm; val dj = e.diametroJoelhoCm
        val osso = if (dp != null && dj != null) 3.02 * (400 * e.estaturaM * e.estaturaM * dp / 100 * dj / 100).pow(0.712) else null
        val motivoOsso = if (dp == null) Motivo.FALTA_DIAMETRO_PUNHO else Motivo.FALTA_DIAMETRO_JOELHO
        if (osso == null) nulos["pesoOsseoKg"] = motivoOsso
        val residual = (if (e.sexo == Sexo.MASCULINO) 0.241 else 0.209) * e.pesoKg
        var base: Double? = null
        when {
            gordo == null -> nulos["pesoMagroKg"] = Motivo.FALTA_DOBRAS
            e.protocoloMassa == ProtocoloMassa.MAGRO -> base = e.pesoKg - gordo
            osso != null -> base = e.pesoKg - (gordo + osso + residual)
            else -> nulos["pesoMagroKg"] = motivoOsso
        }
        val desejado = e.percentualDesejado
        var paraPercentual: Double? = null
        if (desejado != null && base != null) {
            val massa = if (e.protocoloMassa == ProtocoloMassa.MAGRO) base else base + (osso ?: 0.0) + residual
            paraPercentual = massa / (1 - desejado / 100)
        } else nulos["pesoParaPercentualDesejadoKg"] = if (desejado == null) Motivo.FALTA_PERCENTUAL_DESEJADO else nulos["pesoMagroKg"]!!
        return ResultadoAvaliacaoAdulto(
            imc, classe, risco, arredondar(minimo), arredondar(maximo), compleicao, cintura, somatorio, densidade, percentual, classePg,
            pgExato?.let { arredondar(100 - it) }, gordo?.let { arredondar(it) }, osso?.let { arredondar(it) }, arredondar(residual),
            base?.let { arredondar(it) }, paraPercentual?.let { arredondar(it) }, TextoResumo.gerar(classe, risco, cintura, classePg), nulos,
        )
    }

    private fun densidade(sexo: Sexo, protocolo: ProtocoloDobras, d: DobrasCalculo, idade: Int): Double {
        fun v(x: Double?) = x ?: 0.0
        val s7 = v(d.triceps) + v(d.peitoral) + v(d.axilarMedia) + v(d.subescapular) + v(d.abdominal) + v(d.suprailiaca) + v(d.coxa)
        return when {
            protocolo == ProtocoloDobras.SETE && sexo == Sexo.MASCULINO -> 1.112 - 0.00043499 * s7 + 0.00000055 * s7 * s7 - 0.0002882 * idade
            protocolo == ProtocoloDobras.SETE -> 1.097 - 0.00046971 * s7 + 0.00000056 * s7 * s7 - 0.00012828 * idade
            sexo == Sexo.MASCULINO -> { val s = v(d.peitoral) + v(d.abdominal) + v(d.coxa); 1.10938 - 0.0008267 * s + 0.0000016 * s * s - 0.0002574 * idade }
            else -> { val s = v(d.triceps) + v(d.suprailiaca) + v(d.coxa); 1.0994921 - 0.0009929 * s + 0.0000023 * s * s - 0.0001392 * idade }
        }
    }
}

/** Frases das macros `DIAG_*`. Só gera texto quando há IMC, risco e cintura, como a célula B96. */
object TextoResumo {
    fun gerar(classe: ClasseImc, risco: RiscoComorbidades, cintura: ClasseCintura?, pg: ClassePercentualGordura?): String? {
        if (cintura == null) return null
        val imc = when (classe) {
            ClasseImc.MG -> "magreza e "; ClasseImc.NO -> "normalidade e "; ClasseImc.SP -> "sobrepeso e "
            ClasseImc.MG_III -> "magreza grau III (grave) e "; ClasseImc.MG_II -> "magreza grau II (moderada) e "
            ClasseImc.MG_I -> "magreza grau I (leve) e "; ClasseImc.OB_III -> "obesidade grau III (grave) e "
            ClasseImc.OB_II -> "obesidade grau II (moderada) e "; ClasseImc.OB_I -> "obesidade grau I (leve) e "
        }
        val r = when (risco) {
            RiscoComorbidades.B -> "risco baixo"; RiscoComorbidades.ME -> "risco médio"; RiscoComorbidades.A -> "risco aumentado"
            RiscoComorbidades.MO -> "risco moderado"; RiscoComorbidades.S -> "risco severo"
            RiscoComorbidades.MS -> "risco muito severo" // correção 3 (3.6): a macro comparava com "MS I"
        }
        val c = when (cintura) {
            ClasseCintura.B -> "e baixo risco de complicações ligadas ao excesso de gordura abdominal, segundo a circunferência da cintura."
            ClasseCintura.A -> "e risco aumentado de complicações ligadas ao excesso de gordura abdominal, segundo a circunferência da cintura."
            ClasseCintura.MA -> "e risco muito aumentado de complicações ligadas ao excesso de gordura abdominal, segundo a circunferência da cintura."
        }
        val p = pg?.let {
            " Seu percentual de gordura é considerado " + when (it) {
                ClassePercentualGordura.MT_BXO -> "muito baixo"; ClassePercentualGordura.EXC -> "excelente"
                ClassePercentualGordura.MT_BOM -> "muito bom"; ClassePercentualGordura.BOM -> "bom"
                ClassePercentualGordura.ADEQ -> "adequado"; ClassePercentualGordura.MOD_ALTO -> "moderadamente alto"
                ClassePercentualGordura.ALTO -> "alto"; ClassePercentualGordura.MT_ALTO -> "muito alto"
            } + " para sua idade."
        }.orEmpty()
        return "O avaliado apresenta $imc$r de desenvolver comorbidades relacionadas ao excesso de peso, segundo o IMC $c$p"
    }
}
